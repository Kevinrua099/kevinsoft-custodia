// Prueba de integración sobre una base descartable cuyo nombre empiece por custodia_test_.
// Requiere una API local configurada para esa misma base, nunca un servidor publicado.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');

const base = process.env.TEST_CUSTODIA_API_BASE;
const mysql = process.env.TEST_CUSTODIA_MYSQL_CLI;
const database = process.env.TEST_CUSTODIA_DB;
const port = process.env.TEST_CUSTODIA_MYSQL_PORT || '3306';
if (!base || !mysql || !/^custodia_test_[a-z0-9_]+$/i.test(database || '') || !['127.0.0.1', 'localhost'].includes(new URL(base).hostname)) {
    throw new Error('Configura TEST_CUSTODIA_API_BASE local, TEST_CUSTODIA_MYSQL_CLI y TEST_CUSTODIA_DB descartable.');
}

function sql(query) {
    return execFileSync(mysql, ['--protocol=tcp', '--host=127.0.0.1', `--port=${port}`, '--user=root', '--default-character-set=utf8mb4', `--database=${database}`, '--batch', '--skip-column-names', '-e', query], { encoding: 'utf8' }).trim();
}
async function json(path, options) {
    const response = await fetch(new URL(path, base), options);
    const data = await response.json();
    assert.ok(response.ok, `${response.status}: ${JSON.stringify(data)}`);
    return data;
}
function formulario(numero, usuario, tipo) {
    const form = new FormData();
    for (const [key, value] of Object.entries({ numero_registro: numero, usuario_id: String(usuario), origen: 'Muelle', tipo_reporte: tipo, observaciones: '', data_json: '[]' })) form.set(key, value);
    return form;
}

test('rutas asignadas, reporte con foto, arribo y confirmación web comparten datos', async () => {
    const suffix = Date.now().toString().slice(-12);
    const cedula = `T${suffix}`;
    const otraCedula = `U${suffix}`;
    const numero = `TEST-${suffix}`;
    const usuario = Number(sql(`INSERT INTO usuarios (cedula,nombres_completos,rol,password_hash) VALUES ('${cedula}','Prueba Uno','Custodio','prueba'); SELECT LAST_INSERT_ID();`));
    const otroUsuario = Number(sql(`INSERT INTO usuarios (cedula,nombres_completos,rol,password_hash) VALUES ('${otraCedula}','Prueba Dos','Custodio','prueba'); SELECT LAST_INSERT_ID();`));
    const ruta = Number(sql(`INSERT INTO salvoconductos_rutas (numero_registro,origen,destino,estado_ruta) VALUES ('${numero}','Muelle','Planta','En Tránsito'); SELECT LAST_INSERT_ID();`));
    sql(`INSERT INTO asignacion_personal_armas (id_ruta,id_usuario,rol_en_ruta) VALUES (${ruta},${usuario},'CUSTODIO');`);

    const login = await json('api/login.php', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ cedula, password: 'prueba' }) });
    assert.equal(login.usuario.id, usuario);
    assert.ok((await json(`api/historial_app.php?usuario=${usuario}`)).data.some(item => item.numero_registro === numero));
    assert.ok(!(await json(`api/historial_app.php?usuario=${otroUsuario}`)).data.some(item => item.numero_registro === numero));

    const rechazado = await json('api/reportes_app.php', { method: 'POST', body: formulario(numero, otroUsuario, 'Pesca') });
    assert.equal(rechazado.status, 'error');
    assert.equal(Number(sql(`SELECT COUNT(*) FROM reportes_pesca WHERE id_ruta=${ruta}`)), 0);

    const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/aX8AAAAASUVORK5CYII=', 'base64');
    const inspeccion = new FormData();
    for (const [key, value] of Object.entries({ usuario_nombre: 'Prueba Uno', placa: `T${suffix}`.slice(0, 20), disco: 'TEST', kilometraje: '1234', observaciones: 'Prueba de sincronización' })) inspeccion.set(key, value);
    inspeccion.set('foto_0', new Blob([png], { type: 'image/png' }), 'patrulla.png');
    assert.equal((await json('api/patrullas.php', { method: 'POST', body: inspeccion })).status, 'success');
    const registro = (await json('api/patrullas.php')).data.find(item => item.placa === `T${suffix}`);
    assert.equal(registro.kilometraje, 1234);
    assert.equal(registro.fotos[0].guardada, true);
    const fotoPatrulla = await fetch(new URL(`api/patrullas.php?foto=${registro.fotos[0].id}`, base));
    assert.deepEqual(Buffer.from(await fotoPatrulla.arrayBuffer()), png);

    const invalido = formulario(numero, usuario, 'Pesca');
    invalido.set('foto_0', new Blob(['no es una imagen'], { type: 'image/png' }), 'falsa.png');
    assert.equal((await json('api/reportes_app.php', { method: 'POST', body: invalido })).status, 'error');
    assert.equal(Number(sql(`SELECT COUNT(*) FROM reportes_pesca WHERE id_ruta=${ruta}`)), 0);

    const form = formulario(numero, usuario, 'Pesca');
    form.set('data_json', JSON.stringify([{ identificador: 'Guía A', foto_campo: 'foto_0', piscinas: [{ numero: '12', libraje: '150.50' }] }]));
    form.set('foto_0', new Blob([png], { type: 'image/png' }), 'guia.png');
    assert.equal((await json('api/reportes_app.php', { method: 'POST', body: form })).status, 'success');
    const reporte = (await json('api/reportes.php')).data.find(item => item.numero_registro === numero);
    assert.equal(reporte.guias[0].identificador_plataforma, 'Guía A');
    assert.equal(reporte.guias[0].piscinas[0].libraje, '150.50');
    assert.equal(Number(reporte.evidencias[0].guardada), 1);
    const foto = await fetch(new URL(`api/reportes_app.php?evidencia=${reporte.evidencias[0].id_evidencia}`, base));
    assert.equal(foto.headers.get('content-type'), 'image/png');
    assert.deepEqual(Buffer.from(await foto.arrayBuffer()), png);
    assert.equal(reporte.guias[0].ruta_foto_guia, `../api/reportes_app.php?evidencia=${reporte.evidencias[0].id_evidencia}`);

    assert.equal((await json('api/reportes_app.php', { method: 'POST', body: formulario(numero, usuario, 'Arribo a Destino') })).status, 'success');
    assert.equal((await json(`api/verificar_estado.php?ruta=${numero}`)).estado_ruta, 'En espera de confirmación');
    assert.equal((await json('api/reportes_app.php', { method: 'POST', body: formulario(numero, usuario, 'Pesca') })).status, 'error');
    assert.equal((await json('api/finalizar_ruta.php', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ numero_registro: numero }) })).status, 'success');
    assert.equal((await json(`api/historial_app.php?usuario=${usuario}`)).data.find(item => item.numero_registro === numero).estado_ruta, 'Finalizada');
});

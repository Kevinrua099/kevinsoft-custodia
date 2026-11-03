const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function renderizar(reportes) {
    const feed = { innerHTML: '' };
    const document = {
        addEventListener() {},
        querySelectorAll() { return []; },
        getElementById(id) { return id === 'feed-container' ? feed : null; }
    };
    const contexto = vm.createContext({ document, console });
    vm.runInContext(fs.readFileSync(path.join(__dirname, '../web/js/reportes.js'), 'utf8'), contexto);
    contexto.renderizarFeed(reportes);
    return feed.innerHTML;
}

test('presenta cantidad de plataformas y totales por piscina', () => {
    const html = renderizar([{
        numero_registro: 'R-1', placa: 'ASIGNADA', disco: '1', estado_ruta: 'Activa',
        tipo_reporte: 'Pesca', tipo_carga: 'Plataformas', custodio_reporte: 'Custodio',
        patrulla_placa: 'RECIBIDA', patrulla_disco: '22',
        guias: [{ identificador_plataforma: 'P1', piscinas: [{ numero_piscina: '10', libraje: '2000.00' }, { numero_piscina: '11', libraje: '1000.00' }] },
            { identificador_plataforma: 'P2', piscinas: [{ numero_piscina: '10', libraje: '500.00' }] }]
    }]);
    assert.match(html, /Plataformas reportadas: 2/);
    assert.match(html, /Piscina 10: 2\.500,00 lb/);
    assert.match(html, /Total plataforma<\/th><th>3\.000,00 lb/);
    assert.match(html, /Patrulla recibida:<\/strong> Disco 22 \| Placa RECIBIDA/);
    assert.match(html, /Vehículo del salvoconducto/);
});

test('presenta origen, destino y guía del contenedor sin tabla de piscinas', () => {
    const html = renderizar([{
        numero_registro: 'R-2', estado_ruta: 'Activa', tipo_reporte: 'Pesca', tipo_carga: 'Contenedor',
        origen_camaronera: 'Puerto', destino_reporte: '<Bodega>',
        guias: [{ identificador_plataforma: 'CONT-1', ruta_foto_guia: '../api/reportes_app.php?evidencia=42', piscinas: [] }]
    }]);
    assert.match(html, /Puerto → &lt;Bodega&gt;/);
    assert.match(html, /reportes_app\.php\?evidencia=42/);
    assert.doesNotMatch(html, /N° Piscina|Total plataforma/);
});

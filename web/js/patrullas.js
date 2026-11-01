let inspecciones = [];

document.addEventListener('DOMContentLoaded', () => {
    const nombre = localStorage.getItem('usuarioLogueado');
    if (!nombre) { window.location.href = 'index.html'; return; }
    document.getElementById('nombreUsuarioTop').textContent = '👤 ' + nombre;
    document.getElementById('btnCerrarSesion').addEventListener('click', event => {
        event.preventDefault();
        localStorage.removeItem('usuarioLogueado');
        window.location.href = 'index.html';
    });
    document.getElementById('btnActualizar').addEventListener('click', cargarPatrullas);
    document.getElementById('buscarPatrulla').addEventListener('input', renderizarPatrullas);
    document.getElementById('formPatrulla').addEventListener('submit', guardarPatrulla);
    document.getElementById('cerrarDetalle').addEventListener('click', () => document.getElementById('detallePatrulla').close());
    cargarPatrullas();
});

async function cargarPatrullas() {
    const estado = document.getElementById('estadoHistorial');
    const boton = document.getElementById('btnActualizar');
    boton.disabled = true;
    estado.className = 'estado-historial';
    estado.textContent = 'Cargando inspecciones...';
    try {
        const respuesta = await fetch('../api/patrullas.php', { cache: 'no-store' });
        const datos = await respuesta.json();
        if (!respuesta.ok || datos.status !== 'success' || !Array.isArray(datos.data)) {
            throw new Error(datos.mensaje || 'No se pudieron cargar las inspecciones.');
        }
        inspecciones = datos.data;
        document.getElementById('statInspecciones').textContent = inspecciones.length;
        document.getElementById('statVehiculos').textContent = new Set(inspecciones.map(item => (item.placa || '').trim().toUpperCase()).filter(Boolean)).size;
        document.getElementById('statNovedades').textContent = inspecciones.filter(item => (item.observaciones || '').trim()).length;
        renderizarPatrullas();
    } catch (error) {
        console.error('Error al cargar patrullas:', error);
        inspecciones = [];
        document.getElementById('tablaPatrullas').replaceChildren();
        document.getElementById('resultadoBusqueda').textContent = '';
        ['statInspecciones', 'statVehiculos', 'statNovedades'].forEach(id => document.getElementById(id).textContent = '—');
        estado.className = 'estado-historial error';
        estado.textContent = 'No se pudieron cargar las inspecciones. Comprueba la conexión y vuelve a intentarlo.';
    } finally {
        boton.disabled = false;
    }
}

function renderizarPatrullas() {
    const texto = document.getElementById('buscarPatrulla').value.trim().toLocaleLowerCase('es');
    const filtradas = inspecciones.filter(item => [item.placa, item.disco, item.usuario_nombre].some(valor => String(valor || '').toLocaleLowerCase('es').includes(texto)));
    const tabla = document.getElementById('tablaPatrullas');
    tabla.replaceChildren();
    document.getElementById('resultadoBusqueda').textContent = `${filtradas.length} de ${inspecciones.length}`;
    const estado = document.getElementById('estadoHistorial');
    estado.className = 'estado-historial';
    estado.textContent = filtradas.length ? '' : texto ? 'No se encontraron inspecciones para esa búsqueda.' : 'Aún no hay inspecciones registradas.';

    filtradas.forEach(item => {
        const fila = document.createElement('tr');
        const vehiculo = document.createElement('td');
        const placa = document.createElement('strong');
        placa.textContent = item.placa || 'Sin placa';
        const disco = document.createElement('small');
        disco.textContent = 'Disco: ' + (item.disco || 'No indicado');
        vehiculo.append(placa, disco);
        const fecha = document.createElement('td');
        fecha.textContent = formatearFecha(item.fecha_hora_recepcion);
        const responsable = document.createElement('td');
        responsable.textContent = item.usuario_nombre || 'No indicado';
        const estadoTd = document.createElement('td');
        const badge = document.createElement('span');
        const tieneNovedad = Boolean((item.observaciones || '').trim());
        badge.className = 'estado-badge ' + (tieneNovedad ? 'estado-novedad' : 'estado-normal');
        badge.textContent = tieneNovedad ? 'Con observaciones' : 'Sin observaciones';
        estadoTd.appendChild(badge);
        const acciones = document.createElement('td');
        const detalle = document.createElement('button');
        detalle.type = 'button';
        detalle.className = 'btn btn-sm btn-outline-primary';
        detalle.textContent = 'Ver detalle';
        detalle.setAttribute('aria-label', 'Ver detalle de ' + (item.placa || 'patrulla'));
        detalle.addEventListener('click', () => mostrarDetalle(item));
        acciones.appendChild(detalle);
        fila.append(vehiculo, fecha, responsable, estadoTd, acciones);
        tabla.appendChild(fila);
    });
}

function formatearFecha(fecha) {
    if (!fecha) return 'No indicada';
    const valor = new Date(fecha.replace(' ', 'T'));
    return Number.isNaN(valor.getTime()) ? fecha : valor.toLocaleString('es-EC', { dateStyle: 'medium', timeStyle: 'short' });
}

function mostrarDetalle(item) {
    document.getElementById('detalleTitulo').textContent = item.placa || 'Patrulla sin placa';
    const contenido = document.getElementById('contenidoDetalle');
    contenido.replaceChildren();
    const grid = document.createElement('div');
    grid.className = 'detail-grid';
    const kilometraje = item.kilometraje === null || item.kilometraje === '' ? 'No indicado' : Number(item.kilometraje).toLocaleString('es-EC') + ' km';
    [['Disco', item.disco || 'No indicado'], ['Kilometraje', kilometraje], ['Recibida por', item.usuario_nombre || 'No indicado'], ['Fecha de recepción', formatearFecha(item.fecha_hora_recepcion)]].forEach(([etiqueta, valor]) => {
        const celda = document.createElement('div');
        celda.className = 'detail-item';
        const titulo = document.createElement('span');
        titulo.textContent = etiqueta;
        const dato = document.createElement('strong');
        dato.textContent = valor;
        celda.append(titulo, dato);
        grid.appendChild(celda);
    });
    contenido.appendChild(grid);
    const tituloObs = document.createElement('h3');
    tituloObs.className = 'detail-section-title';
    tituloObs.textContent = 'Novedades y observaciones';
    const observaciones = document.createElement('p');
    observaciones.className = 'detail-observaciones';
    observaciones.textContent = item.observaciones || 'Sin observaciones registradas.';
    contenido.append(tituloObs, observaciones);
    const tituloFotos = document.createElement('h3');
    tituloFotos.className = 'detail-section-title';
    tituloFotos.textContent = `Fotografías (${(item.fotos || []).length})`;
    contenido.appendChild(tituloFotos);
    const galeria = document.createElement('div');
    galeria.className = 'fotos-grid';
    (item.fotos || []).forEach(foto => {
        let url;
        if (foto.guardada) {
            url = '../api/patrullas.php?foto=' + encodeURIComponent(foto.id);
        } else if (/^uploads\/patrullas\/[a-zA-Z0-9_.-]+$/.test(foto.ruta_anterior || '')) {
            url = foto.ruta_anterior;
        }
        if (!url) return;
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.target = '_blank';
        enlace.rel = 'noopener noreferrer';
        enlace.setAttribute('aria-label', 'Abrir fotografía de la patrulla');
        const imagen = document.createElement('img');
        imagen.src = url;
        imagen.alt = 'Fotografía de la inspección';
        imagen.loading = 'lazy';
        enlace.appendChild(imagen);
        galeria.appendChild(enlace);
    });
    if (!galeria.childElementCount) galeria.textContent = 'Sin fotos disponibles.';
    contenido.appendChild(galeria);
    document.getElementById('detallePatrulla').showModal();
}

async function guardarPatrulla(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const mensaje = document.getElementById('mensajeFormulario');
    const boton = document.getElementById('btnGuardar');
    const archivos = Array.from(document.getElementById('fotos').files);
    mensaje.className = 'form-feedback';
    mensaje.textContent = '';
    if (archivos.length > 8 || archivos.some(archivo => archivo.size > 2 * 1024 * 1024) || archivos.reduce((total, archivo) => total + archivo.size, 0) > 7 * 1024 * 1024) {
        mensaje.classList.add('error');
        mensaje.textContent = 'Adjunta hasta 8 imágenes de 2 MB cada una y 7 MB en total.';
        return;
    }
    const datos = new FormData(form);
    datos.set('usuario_nombre', localStorage.getItem('usuarioLogueado') || 'Desconocido');
    archivos.forEach((archivo, indice) => datos.append('foto_' + indice, archivo));
    boton.disabled = true;
    boton.textContent = 'Guardando...';
    try {
        const respuesta = await fetch('../api/patrullas.php', { method: 'POST', body: datos });
        const resultado = await respuesta.json();
        if (!respuesta.ok || resultado.status !== 'success') throw new Error(resultado.mensaje || 'No se pudo guardar la inspección.');
        form.reset();
        mensaje.classList.add('success');
        mensaje.textContent = resultado.mensaje;
        await cargarPatrullas();
    } catch (error) {
        mensaje.classList.add('error');
        mensaje.textContent = error.message || 'No se pudo conectar con el servidor.';
    } finally {
        boton.disabled = false;
        boton.textContent = 'Guardar inspección';
    }
}

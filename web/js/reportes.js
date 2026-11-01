document.addEventListener("DOMContentLoaded", () => {
    const nombreUsuario = localStorage.getItem('usuarioLogueado');
    if (!nombreUsuario) { window.location.href = 'index.html'; return; }
    const divUsuario = document.getElementById('nombreUsuarioTop');
    if (divUsuario) divUsuario.textContent = '👤 ' + nombreUsuario;
    document.getElementById('btnCerrarSesion').addEventListener('click', event => {
        event.preventDefault();
        localStorage.removeItem('usuarioLogueado');
        window.location.href = 'index.html';
    });

    cargarReportes(true);
    setInterval(() => cargarReportes(false), 5000);

    document.addEventListener('pointerdown', habilitarSonido, { once: true });
});

let idsReportesConocidos = new Set();
let primeraCargaReportes = true;
let contextoSonido;

function cargarReportes(esCargaInicial) {
    fetch('../api/reportes.php')
        .then(res => res.json())
        .then(data => {
            if (data.status === 'success') {
                notificarReportesNuevos(data.data, esCargaInicial);
                renderizarFeed(data.data);
            } else {
                document.getElementById('feed-container').innerHTML = `<div class="alert alert-danger mt-3">Error del servidor: ${data.mensaje}</div>`;
            }
        })
        .catch(error => {
            console.error("Error de red:", error);
            document.getElementById('feed-container').innerHTML = `<div class="alert alert-danger mt-3">No se pudo conectar con el servidor. Comprueba la conexión e inténtalo de nuevo.</div>`;
        });
}

function notificarReportesNuevos(reportes, esCargaInicial) {
    const reportesNuevos = reportes.filter(reporte => {
        const id = String(reporte.id_reporte);
        return !idsReportesConocidos.has(id);
    });

    if (esCargaInicial || primeraCargaReportes) {
        primeraCargaReportes = false;
    } else {
        reportesNuevos.forEach(mostrarNotificacionReporte);
    }

    idsReportesConocidos = new Set(reportes.map(reporte => String(reporte.id_reporte)));
}

function mostrarNotificacionReporte(reporte) {
    const esArribo = reporte.tipo_reporte === 'Arribo' || reporte.tipo_reporte === 'Arribo a Destino';
    const tipo = esArribo ? 'arribo' : reporte.tipo_reporte === 'Novedad en Vía' ? 'novedad' : 'reporte';
    const titulo = esArribo ? '¡Custodio llegó a destino!' : `Nuevo ${reporte.tipo_reporte || 'reporte'}`;
    const detalle = esArribo
        ? `${reporte.numero_registro} reportó un arribo seguro.`
        : `${reporte.numero_registro} · ${reporte.origen_camaronera || 'Ubicación no indicada'}`;

    mostrarToast(titulo, detalle, tipo);
    reproducirSonidoNotificacion(esArribo);
}

function mostrarToast(titulo, detalle, tipo) {
    const contenedor = document.getElementById('notificaciones');
    if (!contenedor) return;

    const toast = document.createElement('div');
    toast.className = `notificacion-toast notificacion-${tipo}`;
    toast.innerHTML = `
        <div class="notificacion-icono"><i class="fas ${tipo === 'arribo' ? 'fa-flag-checkered' : tipo === 'novedad' ? 'fa-triangle-exclamation' : 'fa-bell'}"></i></div>
        <div class="notificacion-texto"><strong>${titulo}</strong><span>${detalle}</span></div>
        <button type="button" class="notificacion-cerrar" aria-label="Cerrar">&times;</button>
    `;
    toast.querySelector('.notificacion-cerrar').addEventListener('click', () => toast.remove());
    contenedor.appendChild(toast);
    requestAnimationFrame(() => toast.classList.add('visible'));
    setTimeout(() => {
        toast.classList.remove('visible');
        setTimeout(() => toast.remove(), 300);
    }, 7000);
}

function habilitarSonido() {
    contextoSonido = new (window.AudioContext || window.webkitAudioContext)();
}

function reproducirSonidoNotificacion(esArribo) {
    if (!contextoSonido) return;

    const ahora = contextoSonido.currentTime;
    const oscilador = contextoSonido.createOscillator();
    const ganancia = contextoSonido.createGain();
    oscilador.type = 'sine';
    oscilador.frequency.setValueAtTime(esArribo ? 880 : 660, ahora);
    oscilador.frequency.exponentialRampToValueAtTime(esArribo ? 1175 : 880, ahora + 0.14);
    ganancia.gain.setValueAtTime(0.0001, ahora);
    ganancia.gain.exponentialRampToValueAtTime(0.12, ahora + 0.02);
    ganancia.gain.exponentialRampToValueAtTime(0.0001, ahora + 0.32);
    oscilador.connect(ganancia).connect(contextoSonido.destination);
    oscilador.start(ahora);
    oscilador.stop(ahora + 0.32);
}

function renderizarFeed(reportes) {
    const contenedor = document.getElementById('feed-container');
    if (!contenedor) return;

    // 1. Guardar qué pestañas están abiertas actualmente para no cerrarlas
    let acordeonesAbiertos = [];
    document.querySelectorAll('.collapse.show').forEach(el => {
        acordeonesAbiertos.push(el.id);
    });

    if (reportes.length === 0) {
        contenedor.innerHTML = `<div class="alert alert-info shadow-sm mt-3"><i class="fas fa-info-circle"></i> No hay reportes recientes.</div>`;
        return;
    }

    const rutasAgrupadas = {};
    reportes.forEach(rep => {
        const numRuta = rep.numero_registro;
        if (!rutasAgrupadas[numRuta]) {
            rutasAgrupadas[numRuta] = {
                numero_registro: numRuta,
                placa: rep.placa,
                disco: rep.disco,
                estado_ruta: rep.estado_ruta,
                eventos: []
            };
        }
        rutasAgrupadas[numRuta].eventos.push(rep);
    });

    let html = '';

    for (const numRuta in rutasAgrupadas) {
        const ruta = rutasAgrupadas[numRuta];
        // 2. Crear un ID único para cada ruta para no confundir las tarjetas
        const idUnico = "col_" + numRuta.replace(/[^a-zA-Z0-9]/g, "");
        
        let botonAltaHtml = '';
        if (ruta.estado_ruta === 'Finalizada') {
            botonAltaHtml = `<span class="badge bg-success ms-auto fs-6 shadow-sm"><i class="fas fa-check-circle"></i> Finalizada</span>`;
        } else if (ruta.estado_ruta === 'En espera de confirmación') {
            botonAltaHtml = `
                <div class="ms-auto d-flex align-items-center gap-3">
                    <span class="badge bg-warning text-dark fs-6 fw-bold border border-warning shadow-sm"><i class="fas fa-hourglass-half"></i> En Espera de Confirmación</span>
                    <button onclick="darAltaDesdeReportes('${numRuta}', event)" class="btn btn-sm btn-success fw-bold shadow-sm"><i class="fas fa-check-double"></i> DAR DE ALTA</button>
                </div>`;
        } else {
            botonAltaHtml = `<button onclick="darAltaDesdeReportes('${numRuta}', event)" class="btn btn-sm btn-success fw-bold ms-auto shadow-sm"><i class="fas fa-check"></i> DAR DE ALTA</button>`;
        }

        html += `
        <div class="card mb-4 shadow-sm border-0 border-start border-4 border-warning">
            <div class="card-header bg-white d-flex align-items-center py-3" id="head_${idUnico}" data-bs-toggle="collapse" data-bs-target="#${idUnico}" style="cursor:pointer;">
                <i class="fas fa-clipboard-list text-secondary fs-4 me-3"></i>
                <h5 class="mb-0 fw-bold me-3 text-dark">${ruta.numero_registro}</h5>
                <span class="badge bg-dark text-white px-3 py-2 rounded-pill me-2">Disco: ${ruta.disco} | Placa: ${ruta.placa}</span>
                
                ${botonAltaHtml}
                
                <button class="btn btn-sm btn-outline-secondary ms-3 fw-bold">▼ Ver Detalles (${ruta.eventos.length})</button>
            </div>
            
            <div id="${idUnico}" class="collapse" aria-labelledby="head_${idUnico}">
                <div class="card-body bg-light position-relative">
                    <div class="timeline border-start border-2 border-secondary ms-4 ps-4 position-relative">
        `;

        ruta.eventos.forEach(evento => {
            let icono = 'fa-flag-checkered';
            let color = 'text-warning';
            if (evento.tipo_reporte === 'Novedad en Vía') { icono = 'fa-exclamation-triangle'; color = 'text-danger'; }
            if (evento.tipo_reporte === 'Pesca' || evento.tipo_reporte === 'Reporte en Ruta') { icono = 'fa-truck'; color = 'text-primary'; }

            html += `
            <div class="mb-5 position-relative">
                <div class="position-absolute bg-white border border-2 border-secondary rounded-circle text-center" style="width: 20px; height: 20px; left: -35px; top: 0;">
                    <div class="bg-warning rounded-circle mx-auto mt-1" style="width: 10px; height: 10px;"></div>
                </div>
                
                <div class="d-flex justify-content-between mb-2">
                    <h6 class="fw-bold text-uppercase ${color} mb-0 fs-5"><i class="fas ${icono} me-2"></i> ${evento.tipo_reporte}</h6>
                    <small class="text-muted fw-bold"><i class="far fa-clock"></i> ${evento.fecha_hora_reporte}</small>
                </div>
                
                <p class="mb-2"><i class="fas fa-map-marker-alt text-danger me-1"></i> <strong>Ubicación:</strong> <span class="text-dark">${evento.origen_camaronera || 'S/N'}</span></p>
                
                <div class="bg-white p-3 rounded shadow-sm border mb-3">
                    <p class="mb-1 text-muted"><i class="far fa-comment-dots"></i> <strong>Novedades / Observaciones:</strong></p>
                    <p class="mb-0 text-dark">${evento.observaciones ? evento.observaciones : 'Ninguna novedad reportada.'}</p>
                </div>
            `;

            if (evento.guias && evento.guias.length > 0) {
                evento.guias.forEach(guia => {
                    html += `
                    <div class="mb-3 px-2">
                        <p class="fw-bold text-primary mb-2"><i class="fas fa-truck-loading me-1"></i> ${guia.identificador_plataforma || 'S/N'}</p>
                        <table class="table table-sm table-bordered bg-white text-center shadow-sm">
                            <thead class="table-light">
                                <tr><th>N° Piscina</th><th>Libraje</th></tr>
                            </thead>
                            <tbody>
                    `;
                    if (guia.piscinas && guia.piscinas.length > 0) {
                        guia.piscinas.forEach(pisc => {
                            html += `<tr><td class="text-dark">${pisc.numero_piscina || '-'}</td><td class="text-dark">${pisc.libraje || '-'}</td></tr>`;
                        });
                    } else {
                        html += `<tr><td colspan="2" class="text-muted fst-italic">Sin piscinas registradas</td></tr>`;
                    }
                    html += `</tbody></table></div>`;
                });
            }

            if (evento.evidencias && evento.evidencias.length > 0) {
                html += `<div class="mt-4 p-3 bg-white rounded shadow-sm border"><p class="fw-bold mb-3 text-dark"><i class="fas fa-camera text-primary me-2"></i>Evidencias Adjuntas:</p><div class="d-flex gap-3 flex-wrap">`;
                evento.evidencias.forEach(ev => {
                    const [rutaArchivoFinal, rutaAlterna] = obtenerRutasEvidencia(ev.ruta_archivo);
                    
                    if (ev.tipo_archivo === 'video') {
                        html += `
                        <video width="200" height="200" controls class="rounded shadow-sm border border-2 border-light object-fit-cover">
                            <source src="${rutaArchivoFinal}" type="video/mp4">
                        </video>`;
                    } else {
                        html += `<img src="${rutaArchivoFinal}" class="rounded shadow-sm border border-2 border-light object-fit-cover hover-zoom" width="150" height="150" alt="Evidencia del reporte" style="cursor:pointer;" onerror="cambiarRutaEvidencia(this, '${rutaAlterna}')" onclick="abrirVisor(this.src, event)">`;
                    }
                });
                html += `</div></div>`;
            }
            html += `</div>`; 
        });
        html += `</div></div></div></div>`;
    }
    
    contenedor.innerHTML = html;

    // 3. Restaurar estado: Volver a abrir lo que estabas leyendo
    acordeonesAbiertos.forEach(id => {
        let elemento = document.getElementById(id);
        if (elemento) {
            elemento.classList.add('show');
        }
    });
}

function obtenerRutasEvidencia(rutaArchivo) {
    const rutaNormalizada = String(rutaArchivo || '')
        .replace(/\\/g, '/')
        .replace(/^(\.\.\/)+/, '')
        .replace(/^\/+/, '');
    const rutaRelativa = rutaNormalizada.replace(/^web\//, '');
    const archivoCodificado = rutaRelativa
        .split('/')
        .filter(Boolean)
        .map(segmento => encodeURIComponent(segmento))
        .join('/');
    const rutaPrincipal = `../uploads/${archivoCodificado.replace(/^uploads\//, '')}`;
    const rutaAlterna = `uploads/${archivoCodificado.replace(/^uploads\//, '')}`;

    return [rutaPrincipal, rutaAlterna];
}

function cambiarRutaEvidencia(elemento, rutaAlterna) {
    if (elemento.dataset.rutaAlternaUsada === 'true') return;

    elemento.dataset.rutaAlternaUsada = 'true';
    elemento.src = rutaAlterna;
}

function abrirVisor(ruta, event) {
    event.stopPropagation();

    const visor = document.getElementById('visorImagen');
    const imagen = document.getElementById('imagenAmpliada');
    const descarga = document.getElementById('descargarImagen');

    imagen.src = ruta;
    imagen.dataset.escala = '1';
    imagen.style.transform = 'scale(1)';
    descarga.href = ruta;
    visor.style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function cerrarVisor(event) {
    if (event.target.id !== 'visorImagen' && !event.target.classList.contains('lightbox-close')) return;

    document.getElementById('visorImagen').style.display = 'none';
    document.getElementById('imagenAmpliada').src = '';
    document.body.style.overflow = '';
}

document.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
        const visor = document.getElementById('visorImagen');
        if (visor && visor.style.display === 'flex') cerrarVisor({ target: visor });
    }
});

document.addEventListener('wheel', event => {
    const visor = document.getElementById('visorImagen');
    if (!visor || visor.style.display !== 'flex' || event.target.id !== 'imagenAmpliada') return;

    event.preventDefault();
    const imagen = event.target;
    const escalaActual = Number(imagen.dataset.escala || 1);
    const nuevaEscala = Math.min(4, Math.max(1, escalaActual + (event.deltaY < 0 ? 0.25 : -0.25)));
    imagen.dataset.escala = nuevaEscala;
    imagen.style.transform = `scale(${nuevaEscala})`;
}, { passive: false });

function darAltaDesdeReportes(numeroRuta, event) {
    event.stopPropagation(); 
    if (confirm(`¿Estás seguro de finalizar y dar de alta la ruta ${numeroRuta}?`)) {
        fetch('../api/finalizar_ruta.php', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ numero_registro: numeroRuta })
        })
            .then(res => res.json())
            .then(data => {
                if (data.status === 'success') {
                    mostrarToast('Ruta finalizada', data.mensaje, 'exito');
                    cargarReportes(false);
                } else {
                    mostrarToast('No se pudo finalizar', data.mensaje || 'El servidor rechazó la operación.', 'error');
                }
            })
            .catch(error => {
                console.error('Error al finalizar la ruta:', error);
                mostrarToast('Error de conexión', 'No fue posible finalizar la ruta.', 'error');
            });
    }
}

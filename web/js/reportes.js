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
    document.addEventListener('click', event => {
        const boton = event.target.closest('.btn-finalizar-ruta');
        if (boton) darAltaDesdeReportes(boton.dataset.ruta, event);
    });
});

function escaparHtml(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, caracter => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[caracter]));
}

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
                document.getElementById('feed-container').innerHTML = `<div class="alert alert-danger mt-3">Error del servidor: ${escaparHtml(data.mensaje)}</div>`;
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
        <div class="notificacion-texto"><strong>${escaparHtml(titulo)}</strong><span>${escaparHtml(detalle)}</span></div>
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
        const totalesPiscinas = new Map();
        ruta.eventos.forEach(evento => (evento.guias || []).forEach(guia => (guia.piscinas || []).forEach(piscina => {
            const numero = String(piscina.numero_piscina || '').trim();
            const libras = Number(piscina.libraje);
            if (numero && piscina.libraje !== null && piscina.libraje !== '' && Number.isFinite(libras)) {
                totalesPiscinas.set(numero, (totalesPiscinas.get(numero) || 0) + libras);
            }
        })));
        // 2. Crear un ID único para cada ruta para no confundir las tarjetas
        const idUnico = "col_" + numRuta.replace(/[^a-zA-Z0-9]/g, "");
        
        let botonAltaHtml = '';
        if (ruta.estado_ruta === 'Finalizada') {
            botonAltaHtml = `<span class="badge bg-success ms-auto fs-6 shadow-sm"><i class="fas fa-check-circle"></i> Finalizada</span>`;
        } else if (ruta.estado_ruta === 'En espera de confirmación') {
            botonAltaHtml = `
                <div class="ms-auto d-flex align-items-center gap-3">
                    <span class="badge bg-warning text-dark fs-6 fw-bold border border-warning shadow-sm"><i class="fas fa-hourglass-half"></i> En Espera de Confirmación</span>
                    <button data-ruta="${escaparHtml(numRuta)}" class="btn btn-sm btn-success fw-bold shadow-sm btn-finalizar-ruta"><i class="fas fa-check-double"></i> DAR DE ALTA</button>
                </div>`;
        } else {
            botonAltaHtml = `<button data-ruta="${escaparHtml(numRuta)}" class="btn btn-sm btn-success fw-bold ms-auto shadow-sm btn-finalizar-ruta"><i class="fas fa-check"></i> DAR DE ALTA</button>`;
        }

        html += `
        <div class="card mb-4 shadow-sm border-0 border-start border-4 border-warning">
            <div class="card-header bg-white d-flex align-items-center py-3" id="head_${idUnico}" data-bs-toggle="collapse" data-bs-target="#${idUnico}" style="cursor:pointer;">
                <i class="fas fa-clipboard-list text-secondary fs-4 me-3"></i>
                <h5 class="mb-0 fw-bold me-3 text-dark">${escaparHtml(ruta.numero_registro)}</h5>
                <span class="badge bg-dark text-white px-3 py-2 rounded-pill me-2">Vehículo del salvoconducto · Disco: ${escaparHtml(ruta.disco)} | Placa: ${escaparHtml(ruta.placa)}</span>
                
                ${botonAltaHtml}
                
                <button class="btn btn-sm btn-outline-secondary ms-3 fw-bold">▼ Ver Detalles (${ruta.eventos.length})</button>
            </div>
            
            <div id="${idUnico}" class="collapse" aria-labelledby="head_${idUnico}">
                <div class="card-body bg-light position-relative">
                    ${totalesPiscinas.size ? `<div class="bg-white p-3 rounded border mb-3"><strong>Libraje reportado por piscina (todos los reportes de esta ruta)</strong><ul class="mb-0">${Array.from(totalesPiscinas, ([numero, libras]) => `<li>Piscina ${escaparHtml(numero)}: ${libras.toLocaleString('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} lb</li>`).join('')}</ul></div>` : ''}
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
                    <h6 class="fw-bold text-uppercase ${color} mb-0 fs-5"><i class="fas ${icono} me-2"></i> ${escaparHtml(evento.tipo_reporte)}</h6>
                    <small class="text-muted fw-bold"><i class="far fa-clock"></i> ${escaparHtml(evento.fecha_hora_reporte)}</small>
                </div>
                
                <p class="mb-2"><i class="fas fa-map-marker-alt text-danger me-1"></i> <strong>Ubicación:</strong> <span class="text-dark">${escaparHtml(evento.origen_camaronera || 'S/N')}</span></p>
                ${evento.tipo_carga === 'Contenedor' ? `<p class="mb-2"><strong>Movimiento del contenedor:</strong> ${escaparHtml(evento.origen_camaronera || 'S/N')} → ${escaparHtml(evento.destino_reporte || 'S/N')}</p>` : ''}
                ${evento.custodio_reporte ? `<p class="mb-2"><strong>Reportado por:</strong> ${escaparHtml(evento.custodio_reporte)} · <strong>Patrulla recibida:</strong> ${evento.patrulla_placa ? `Disco ${escaparHtml(evento.patrulla_disco || 'S/N')} | Placa ${escaparHtml(evento.patrulla_placa)}` : 'No registrada'}</p>` : ''}
                
                <div class="bg-white p-3 rounded shadow-sm border mb-3">
                    <p class="mb-1 text-muted"><i class="far fa-comment-dots"></i> <strong>Novedades / Observaciones:</strong></p>
                    <p class="mb-0 text-dark">${escaparHtml(evento.observaciones || 'Ninguna novedad reportada.')}</p>
                </div>
            `;

            if (evento.guias && evento.guias.length > 0) {
                if (evento.tipo_carga === 'Plataformas') html += `<p class="fw-bold">Plataformas reportadas: ${evento.guias.length}</p>`;
                evento.guias.forEach(guia => {
                    const totalPlataforma = (guia.piscinas || []).reduce((total, piscina) => total + (Number(piscina.libraje) || 0), 0);
                    html += `
                    <div class="mb-3 px-2">
                        <p class="fw-bold text-primary mb-2"><i class="fas fa-truck-loading me-1"></i> ${escaparHtml(guia.identificador_plataforma || 'S/N')}</p>
                        ${/^\.\.\/api\/reportes_app\.php\?evidencia=\d+$/.test(guia.ruta_foto_guia || '') ? `<img src="${guia.ruta_foto_guia}" alt="Foto de la guía" width="150" height="150" class="rounded object-fit-cover mb-2" onclick="abrirVisor(this.src, event)">` : ''}
                    `;
                    if (evento.tipo_carga === 'Contenedor') {
                        html += `</div>`;
                        return;
                    }
                    html += `
                        <table class="table table-sm table-bordered bg-white text-center shadow-sm">
                            <thead class="table-light">
                                <tr><th>N° Piscina</th><th>Libraje</th></tr>
                            </thead>
                            <tbody>
                    `;
                    if (guia.piscinas && guia.piscinas.length > 0) {
                        guia.piscinas.forEach(pisc => {
                            html += `<tr><td class="text-dark">${escaparHtml(pisc.numero_piscina || '-')}</td><td class="text-dark">${escaparHtml(pisc.libraje || '-')}</td></tr>`;
                        });
                    } else {
                        html += `<tr><td colspan="2" class="text-muted fst-italic">Sin piscinas registradas</td></tr>`;
                    }
                    html += `</tbody><tfoot><tr><th>Total plataforma</th><th>${totalPlataforma.toLocaleString('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} lb</th></tr></tfoot></table></div>`;
                });
            }

            if (evento.evidencias && evento.evidencias.length > 0) {
                html += `<div class="mt-4 p-3 bg-white rounded shadow-sm border"><p class="fw-bold mb-3 text-dark"><i class="fas fa-camera text-primary me-2"></i>Evidencias Adjuntas:</p><div class="d-flex gap-3 flex-wrap">`;
                evento.evidencias.forEach(ev => {
                    const guardada = Number(ev.guardada) === 1;
                    const [rutaArchivoFinal, rutaAlterna] = guardada
                        ? [`../api/reportes_app.php?evidencia=${Number(ev.id_evidencia)}`, '']
                        : obtenerRutasEvidencia(ev.ruta_archivo);
                    
                    if (ev.tipo_archivo === 'video') {
                        html += `
                        <video width="200" height="200" controls class="rounded shadow-sm border border-2 border-light object-fit-cover">
                            <source src="${rutaArchivoFinal}" type="video/mp4">
                        </video>`;
                    } else {
                        html += `<img src="${rutaArchivoFinal}" class="rounded shadow-sm border border-2 border-light object-fit-cover hover-zoom" width="150" height="150" alt="Evidencia del reporte" style="cursor:pointer;" ${guardada ? '' : `onerror="cambiarRutaEvidencia(this, '${rutaAlterna}')"`} onclick="abrirVisor(this.src, event)">`;
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
        .map(segmento => encodeURIComponent(segmento).replace(/'/g, '%27'))
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

// js/dashboard.js
document.addEventListener('DOMContentLoaded', function() {
    // Seguridad
    const nombreUsuario = localStorage.getItem('usuarioLogueado');
    if (!nombreUsuario) { window.location.href = 'index.html'; return; }
    document.getElementById('nombreUsuarioTop').innerText = '👤 ' + nombreUsuario;

    // Cargar datos al iniciar
    cargarPanelGeneral();

    // Botón Cerrar Sesión
    document.getElementById('btnCerrarSesion').addEventListener('click', function(e) {
        e.preventDefault(); 
        localStorage.removeItem('usuarioLogueado'); 
        window.location.href = 'index.html';
    });
});

function cargarPanelGeneral() {
    fetch('../api/dashboard.php')
    .then(res => res.json())
    .then(data => {
        if(data.status === 'success') {
            // Llenar tarjetas numéricas
            document.getElementById('stat_rutas').innerText = data.stats.rutas_activas;
            document.getElementById('stat_armas').innerText = data.stats.armas_entregadas;
            document.getElementById('stat_personal').innerText = data.stats.personal_activo;

            // Llenar tabla de historial
            const tbody = document.getElementById('tablaHistorial');
            tbody.innerHTML = '';
            
            if (!data.historial.length) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No hay salvoconductos recientes.</td></tr>';
            }
            data.historial.forEach(orden => {
                let colorEstado = 'text-primary';
                if(orden.estado_ruta === 'Finalizada') colorEstado = 'text-success';
                
                let fecha = new Date(orden.fecha_emision);
                let fechaFormateada = fecha.toLocaleString('es-ES', { dateStyle: 'short', timeStyle: 'short' });

                // Botón de Dar Alta solo si está en tránsito
                let btnAlta = orden.estado_ruta === 'En Tránsito' 
                    ? `<button onclick="darDeAlta('${orden.numero_registro}')" class="btn btn-sm btn-success py-0 px-2" title="Finalizar Ruta">✅ Alta</button>` 
                    : ``;

                const fila = `
                    <tr>
                        <td class="fw-bold">${orden.numero_registro}</td>
                        <td>${fechaFormateada}</td>
                        <td>${orden.asunto}</td>
                        <td class="text-secondary fw-bold">👨‍💻 ${orden.creado_por || 'Sistema'}</td>
                        <td class="${colorEstado} fw-bold">● ${orden.estado_ruta}</td>
                        <td>
                            <button onclick="window.open('imprimir.html?orden=${orden.numero_registro}', '_blank')" class="btn btn-sm btn-outline-secondary py-0 px-2" title="Reimprimir PDF">🖨️ PDF</button>
                            ${btnAlta}
                        </td>
                    </tr>
                `;
                tbody.innerHTML += fila;
            });
        } else {
            throw new Error(data.mensaje || 'No se pudo cargar el panel');
        }
    })
    .catch(error => {
        console.error("Error al cargar el panel:", error);
        ['stat_rutas', 'stat_armas', 'stat_personal'].forEach(id => {
            document.getElementById(id).textContent = '—';
        });
        document.getElementById('tablaHistorial').innerHTML = '<tr><td colspan="6" class="text-center text-danger">No se pudo cargar el panel. Comprueba la conexión y vuelve a intentarlo.</td></tr>';
    });
}
function darDeAlta(numero_registro) {
    if(confirm("¿Dar de alta esta ruta? Esto liberará los vehículos y armas asociados.")) {
        fetch('../api/finalizar_ruta.php', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ numero_registro: numero_registro })
        })
        .then(res => res.json())
        .then(data => {
            if(data.status === 'success') {
                alert("✅ " + data.mensaje);
                cargarPanelGeneral(); // Recarga la tabla
            } else {
                alert("⚠️ Error: " + data.mensaje);
            }
        })
        .catch(() => alert('No se pudo conectar con el servidor. Inténtalo de nuevo.'));
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const nombreUsuario = localStorage.getItem('usuarioLogueado');
    if (!nombreUsuario) { window.location.href = 'index.html'; return; }
    document.getElementById('nombreUsuarioTop').innerText = '👤 ' + nombreUsuario;

    cargarRegistros();

    const form = document.getElementById('formBandidas');
    if(form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            const btn = document.getElementById('btnGuardar');
            btn.innerText = 'Guardando...';
            btn.disabled = true;

            const datos = {
                alias: document.getElementById('alias').value,
                nombre_completo: document.getElementById('nombre_completo').value,
                zona_frecuente: document.getElementById('zona_frecuente').value,
                nivel_riesgo: document.getElementById('nivel_riesgo').value,
                observaciones: document.getElementById('observaciones').value,
                creador: nombreUsuario
            };

            fetch('../api/bandidas.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(datos)
            })
            .then(res => res.json())
            .then(data => {
                btn.innerText = 'Guardar Registro';
                btn.disabled = false;
                if(data.status === 'success') {
                    form.reset();
                    cargarRegistros();
                } else { alert('Error: ' + data.mensaje); }
            });
        });
    }

    document.getElementById('btnCerrarSesion').addEventListener('click', function(e) {
        e.preventDefault(); localStorage.removeItem('usuarioLogueado'); window.location.href = 'index.html';
    });
});

function cargarRegistros() {
    fetch('../api/bandidas.php')
    .then(res => res.json())
    .then(data => {
        if(data.status === 'success') {
            const tbody = document.getElementById('tablaRegistros');
            tbody.innerHTML = '';
            data.data.forEach(reg => {
                let colorRiesgo = reg.nivel_riesgo === 'Crítico' ? 'text-danger' : (reg.nivel_riesgo === 'Alto' ? 'text-warning' : 'text-primary');
                tbody.innerHTML += `
                    <tr>
                        <td class="fw-bold">${reg.alias}</td>
                        <td>${reg.nombre_completo}</td>
                        <td>${reg.zona_frecuente}</td>
                        <td class="${colorRiesgo} fw-bold">${reg.nivel_riesgo}</td>
                        <td>${reg.observaciones}</td>
                        <td class="small text-muted">${reg.registrado_por}</td>
                    </tr>
                `;
            });
        }
    });
}

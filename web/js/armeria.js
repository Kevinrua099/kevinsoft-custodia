let dataArmas = []; // Memoria global para editar

document.addEventListener('DOMContentLoaded', function() {
    const nombreUsuario = localStorage.getItem('usuarioLogueado');
    if (!nombreUsuario) { window.location.href = 'index.html'; return; }
    document.getElementById('nombreUsuarioTop').innerText = '👤 ' + nombreUsuario;

    cargarArmeria();

    const formArmeria = document.getElementById('formArmeria');
    if(formArmeria) {
        formArmeria.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const btnGuardar = document.getElementById('btnGuardar');
            btnGuardar.innerText = 'Procesando...';
            btnGuardar.disabled = true;

            const id_arma = document.getElementById('id_arma').value;
            const metodo = id_arma === "" ? 'POST' : 'PUT';

            const datos = {
                id_arma: id_arma,
                serie: document.getElementById('serie').value,
                tipo: document.getElementById('tipo').value,
                marca: document.getElementById('marca').value,
                calibre: document.getElementById('calibre').value,
                estado: document.getElementById('estado').value || 'En Armería'
            };

            fetch('../api/armeria.php', {
                method: metodo,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(datos)
            })
            .then(res => res.json())
            .then(data => {
                btnGuardar.innerText = 'Registrar Arma';
                btnGuardar.disabled = false;
                
                if(data.status === 'success') {
                    cancelarEdicion();
                    cargarArmeria();
                } else {
                    alert('⚠️ Error: ' + data.mensaje);
                }
            })
            .catch(error => {
                btnGuardar.innerText = 'Registrar Arma';
                btnGuardar.disabled = false;
                alert('⚠️ Error de conexión.');
            });
        });
    }

    document.getElementById('btnCerrarSesion').addEventListener('click', function(e) {
        e.preventDefault(); localStorage.removeItem('usuarioLogueado'); window.location.href = 'index.html';
    });
});

function cargarArmeria() {
    fetch('../api/armeria.php')
    .then(res => res.json())
    .then(data => {
        if(data.status === 'success') {
            dataArmas = data.data; // Guardamos en memoria local
            const tbody = document.getElementById('tablaArmeria');
            tbody.innerHTML = '';
            
            data.data.forEach(arma => {
                let claseEstado = '';
                if(arma.estado === 'En Armería') claseEstado = 'estado-armeria';
                if(arma.estado === 'Entregada') claseEstado = 'estado-entregada';
                if(arma.estado === 'Mantenimiento') claseEstado = 'estado-mantenimiento';
                if(arma.estado === 'Robada') claseEstado = 'estado-robada';
                if(arma.estado === 'Inactiva') claseEstado = 'estado-inactiva';

                const fila = `
                    <tr>
                        <td class="fw-bold">${arma.serie}</td>
                        <td>${arma.tipo_arma}</td>
                        <td>${arma.marca}</td>
                        <td>${arma.calibre}</td>
                        <td><span class="${claseEstado}">● ${arma.estado}</span></td>
                        <td>
                            <button onclick="editarArma(${arma.id_arma})" class="btn btn-sm btn-outline-primary py-0 px-2" title="Editar Arma">✏️</button>
                            <button onclick="darDeBajaArma(${arma.id_arma})" class="btn btn-sm btn-outline-danger py-0 px-2" title="Dar de baja">🗑️</button>
                        </td>
                    </tr>
                `;
                tbody.innerHTML += fila;
            });
        }
    });
}

function editarArma(id) {
    const arma = dataArmas.find(a => a.id_arma == id);
    if(arma) {
        document.getElementById('id_arma').value = arma.id_arma;
        document.getElementById('serie').value = arma.serie;
        document.getElementById('tipo').value = arma.tipo_arma;
        document.getElementById('marca').value = arma.marca;
        document.getElementById('calibre').value = arma.calibre;
        document.getElementById('estado').value = arma.estado;
        
        document.getElementById('divEstado').style.display = 'block'; // Mostramos el estado
        document.getElementById('btnGuardar').innerText = 'Actualizar Arma';
        document.getElementById('btnGuardar').classList.replace('btn-primary', 'btn-warning');
        document.getElementById('btnCancelar').style.display = 'block';
    }
}

function cancelarEdicion() {
    document.getElementById('formArmeria').reset();
    document.getElementById('id_arma').value = '';
    document.getElementById('divEstado').style.display = 'none';
    document.getElementById('btnGuardar').innerText = 'Registrar Arma';
    document.getElementById('btnGuardar').classList.replace('btn-warning', 'btn-primary');
    document.getElementById('btnCancelar').style.display = 'none';
}

function darDeBajaArma(id) {
    if(confirm("¿Estás seguro de dar de baja esta arma? Pasará a estado Inactiva.")) {
        fetch('../api/armeria.php', {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id_arma: id })
        })
        .then(res => res.json())
        .then(data => {
            if(data.status === 'success') {
                cargarArmeria();
            }
        });
    }
}

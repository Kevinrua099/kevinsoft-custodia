document.addEventListener('DOMContentLoaded', function() {
    const nombreUsuario = localStorage.getItem('usuarioLogueado');
    if (!nombreUsuario) { window.location.href = 'index.html'; return; }
    document.getElementById('nombreUsuarioTop').innerText = '👤 ' + nombreUsuario;

    cargarNomina();

    // GUARDAR (CREAR O EDITAR)
    document.getElementById('formNomina').addEventListener('submit', function(e) {
        e.preventDefault();
        
        const btnGuardar = document.getElementById('btnGuardar');
        btnGuardar.innerText = 'Procesando...';
        btnGuardar.disabled = true;

        const id_usuario = document.getElementById('id_usuario').value;
        const metodo = id_usuario === "" ? 'POST' : 'PUT'; // Si hay ID es edición, si no es creación

        const datos = {
            id_usuario: id_usuario,
            cedula: document.getElementById('cedula').value,
            nombres: document.getElementById('nombres').value,
            cia: document.getElementById('cia').value,
            rol: document.getElementById('rol').value,
            estado: document.getElementById('estado').value,
            password: document.getElementById('password').value
        };

        // En POST la contraseña es obligatoria
        if(metodo === 'POST' && datos.password === '') {
            alert('La contraseña es obligatoria para usuarios nuevos.');
            btnGuardar.innerText = 'Guardar'; btnGuardar.disabled = false;
            return;
        }

        fetch('../api/nomina.php', {
            method: metodo,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datos)
        })
        .then(res => res.json())
        .then(data => {
            btnGuardar.innerText = 'Guardar';
            btnGuardar.disabled = false;
            
            if(data.status === 'success') {
                cancelarEdicion(); // Limpiamos formulario
                cargarNomina(); // Recargamos tabla
            } else {
                alert(data.mensaje);
            }
        });
    });

    document.getElementById('btnCerrarSesion').addEventListener('click', function(e) {
        e.preventDefault(); localStorage.removeItem('usuarioLogueado'); window.location.href = 'index.html';
    });
});

// VARIABLES GLOBALES PARA ALMACENAR LA DATA TEMPORALMENTE
let dataUsuarios = [];

function cargarNomina() {
    fetch('../api/nomina.php')
    .then(res => res.json())
    .then(data => {
        if(data.status === 'success') {
            dataUsuarios = data.data; // Guardamos en memoria para usar al editar
            const tbody = document.getElementById('tablaNomina');
            tbody.innerHTML = '';
            
            data.data.forEach(user => {
                let claseRol = 'rol-custodio';
                if(user.rol === 'Consolero') claseRol = 'rol-consolero';
                if(user.rol === 'Jefe') claseRol = 'rol-jefe';

                // Usamos comillas simples invertidas para el HTML
                const fila = `
                    <tr>
                        <td>${user.cedula}</td>
                        <td class="fw-bold">${user.nombres_completos}</td>
                        <td>${user.cia_empresa}</td>
                        <td><span class="badge-rol ${claseRol}">${user.rol}</span></td>
                        <td><span class="text-${user.estado === 'Activo' ? 'success' : 'danger'} fw-bold">${user.estado}</span></td>
                        <td>
                            <button onclick="editarUsuario(${user.id_usuario})" class="btn btn-sm btn-outline-primary py-0 px-2" title="Editar">✏️</button>
                            <button onclick="darDeBaja(${user.id_usuario})" class="btn btn-sm btn-outline-danger py-0 px-2" title="Dar de baja">🗑️</button>
                        </td>
                    </tr>
                `;
                tbody.innerHTML += fila;
            });
        }
    });
}

function editarUsuario(id) {
    // Buscamos el usuario en nuestro array de memoria
    const user = dataUsuarios.find(u => u.id_usuario == id);
    if(user) {
        document.getElementById('id_usuario').value = user.id_usuario;
        document.getElementById('cedula').value = user.cedula;
        document.getElementById('nombres').value = user.nombres_completos;
        document.getElementById('cia').value = user.cia_empresa;
        document.getElementById('rol').value = user.rol;
        document.getElementById('estado').value = user.estado;
        document.getElementById('password').value = ''; // Por seguridad no mostramos la clave
        
        // Cambiamos el formulario a modo edición
        document.getElementById('btnGuardar').innerText = 'Actualizar Cambios';
        document.getElementById('btnGuardar').classList.replace('btn-primary', 'btn-warning');
        document.getElementById('btnCancelar').style.display = 'block';
    }
}

function cancelarEdicion() {
    document.getElementById('formNomina').reset();
    document.getElementById('id_usuario').value = '';
    document.getElementById('btnGuardar').innerText = 'Guardar Registro';
    document.getElementById('btnGuardar').classList.replace('btn-warning', 'btn-primary');
    document.getElementById('btnCancelar').style.display = 'none';
}

function darDeBaja(id) {
    if(confirm("¿Estás seguro de dar de baja (inactivar) a este custodio?")) {
        fetch('../api/nomina.php', {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id_usuario: id })
        })
        .then(res => res.json())
        .then(data => {
            if(data.status === 'success') {
                cargarNomina(); // Recargamos para ver el cambio a Inactivo
            }
        });
    }
}

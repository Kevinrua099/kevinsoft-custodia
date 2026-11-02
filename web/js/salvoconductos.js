// js/salvoconductos.js

// Variables globales para clonar las listas cuando agregues más de 1 custodio
let opcionesUsuariosHTML = '<option value="">Seleccione personal...</option>';
let opcionesArmasHTML = '<option value="">Sin arma asignada...</option>';
let contadorCustodios = 1; // El HTML ya trae el "Custodio 1" por defecto
let patrullasTitulares = [];

document.addEventListener('DOMContentLoaded', function() {
    const nombreUsuario = localStorage.getItem('usuarioLogueado');
    if (!nombreUsuario) { window.location.href = 'index.html'; return; }
    document.getElementById('nombreUsuarioTop').innerText = '👤 ' + nombreUsuario;

    cargarListasDesplegables();
    document.getElementById('chofer').addEventListener('change', sugerirPatrullaTitular);

    const formSalvoconducto = document.getElementById('formSalvoconducto');
    if(formSalvoconducto) {
        formSalvoconducto.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const btnSubmit = document.querySelector('#formSalvoconducto button[type="submit"]');
            btnSubmit.innerText = 'Procesando Ruta...';
            btnSubmit.disabled = true;

            // 1. Datos Generales
            const datos = {
                tipo_viaje: document.getElementById('tipo_viaje').value,
                asunto: document.getElementById('asunto').value,
                fecha_salida: document.getElementById('fecha_salida').value,
                fecha_retorno: document.getElementById('fecha_retorno').value,
                origen: document.getElementById('origen').value,
                destino: document.getElementById('destino').value,
                vehiculo: document.getElementById('vehiculo').value,
                chofer: document.getElementById('chofer').value,
                arma_chofer: document.getElementById('arma_chofer').value,
                custodios_array: [], 
                creado_por: nombreUsuario // <--- Enviamos el nombre de quien está conectado
            };

            if(!datos.chofer) {
                alert("Debes asignar al menos a un Líder/Chofer a la ruta.");
                btnSubmit.innerText = 'Generar Salvoconducto y Asignar Armas';
                btnSubmit.disabled = false;
                return;
            }

            // 2. Extraer a todos los custodios generados
            for(let i = 1; i <= contadorCustodios; i++) {
                const elCustodio = document.getElementById('custodio' + i);
                const elArma = document.getElementById('arma_custodio' + i);
                
                // Si el campo existe y tiene a alguien seleccionado, lo agregamos al viaje
                if(elCustodio && elCustodio.value !== "") {
                    datos.custodios_array.push({
                        id_usuario: elCustodio.value,
                        id_arma: elArma ? elArma.value : ""
                    });
                }
            }

            // 3. Enviar todo el paquete a la API
            fetch('../api/salvoconductos.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(datos)
            })
            .then(res => res.json())
            .then(data => {
                btnSubmit.innerText = 'Generar Salvoconducto y Asignar Armas';
                btnSubmit.disabled = false;
                
                if(data.status === 'success') {
                    window.open('imprimir.html?orden=' + data.numero_registro, '_blank');
                    formSalvoconducto.reset(); 
                    document.getElementById('contenedor_custodios_extra').innerHTML = ''; // Limpiamos los extras
                    contadorCustodios = 1; // Reiniciamos el contador
                    cargarListasDesplegables(); 
                } else {
                    alert('⚠️ Error: ' + data.mensaje);
                }
            })
            .catch(error => {
                console.error(error);
                btnSubmit.innerText = 'Generar Salvoconducto y Asignar Armas';
                btnSubmit.disabled = false;
                alert('Error de conexión.');
            });
        });
    }

    document.getElementById('btnCerrarSesion').addEventListener('click', function(e) {
        e.preventDefault(); localStorage.removeItem('usuarioLogueado'); window.location.href = 'index.html';
    });
});

// FUNCIÓN PARA CREAR NUEVOS CAMPOS EN PANTALLA
function agregarCustodioAdicional() {
    contadorCustodios++;
    
    // Creamos un nuevo bloque idéntico pero con números correlativos (Custodio 2, Custodio 3...)
    const div = document.createElement('div');
    div.className = 'row mb-3 p-3 border rounded bg-light';
    div.innerHTML = `
        <label class="fw-bold text-primary mb-2">CUSTODIO ${contadorCustodios}</label>
        <div class="col-md-6">
            <select id="custodio${contadorCustodios}" class="form-select mb-2">
                ${opcionesUsuariosHTML}
            </select>
        </div>
        <div class="col-md-6">
            <select id="arma_custodio${contadorCustodios}" class="form-select">
                ${opcionesArmasHTML}
            </select>
        </div>
    `;
    
    // Lo inyectamos en el HTML
    document.getElementById('contenedor_custodios_extra').appendChild(div);
}

function cargarListasDesplegables() {
    fetch('../api/salvoconductos.php')
    .then(res => res.json())
    .then(data => {
        if(data.status === 'success') {
            patrullasTitulares = data.patrullas_titulares || [];
            
            let vehiculosHTML = '<option value="">Seleccione vehículo...</option>';
            data.vehiculos.forEach(v => { vehiculosHTML += `<option value="${v.id_vehiculo}">${v.placa} (${v.cia_vehiculo})</option>`; });
            document.getElementById('vehiculo').innerHTML = vehiculosHTML;

            // Guardamos las opciones en las variables globales para poder clonarlas después
            opcionesUsuariosHTML = '<option value="">Seleccione personal...</option>';
            data.usuarios.forEach(u => { opcionesUsuariosHTML += `<option value="${u.id_usuario}">${u.nombres_completos} - ${u.rol}</option>`; });
            document.getElementById('chofer').innerHTML = opcionesUsuariosHTML;
            document.getElementById('custodio1').innerHTML = opcionesUsuariosHTML;

            opcionesArmasHTML = '<option value="">Sin arma asignada...</option>';
            data.armas.forEach(a => { opcionesArmasHTML += `<option value="${a.id_arma}">${a.tipo_arma} ${a.marca} (Serie: ${a.serie})</option>`; });
            document.getElementById('arma_chofer').innerHTML = opcionesArmasHTML;
            document.getElementById('arma_custodio1').innerHTML = opcionesArmasHTML;
        }
    });
}

function sugerirPatrullaTitular() {
    const titular = document.getElementById('chofer').value;
    const asignacion = patrullasTitulares.find(item => String(item.id_usuario) === titular);
    // El selector sigue editable para elegir otra unidad.
    document.getElementById('vehiculo').value = asignacion ? String(asignacion.id_vehiculo) : '';
}

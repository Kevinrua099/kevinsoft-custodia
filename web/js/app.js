// js/app.js
document.addEventListener('DOMContentLoaded', function() {
    const formLogin = document.getElementById('formLogin');
    
    if(formLogin) {
        formLogin.addEventListener('submit', function(e) {
            e.preventDefault(); // Evita que la página recargue

            const cedula = document.getElementById('cedula').value;
            const password = document.getElementById('password').value;
            const alerta = document.getElementById('mensaje_alerta');
            const btnSubmit = document.getElementById('btnSubmit');

            // Cambiamos el botón para que parezca que está cargando
            btnSubmit.innerHTML = 'VERIFICANDO...';
            btnSubmit.disabled = true;

            // Petición a la API PHP
            fetch('../api/login.php', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ cedula: cedula, password: password })
            })
            .then(response => response.json())
            .then(data => {
                // Restauramos el botón
                btnSubmit.innerHTML = 'INICIAR SESIÓN';
                btnSubmit.disabled = false;
                
                alerta.style.display = 'block';

                if(data.status === 'success') {
                    // Diseño verde de éxito
                    alerta.className = 'alert-message bg-success text-white';
                    alerta.textContent = '✅ ' + data.mensaje + '. Abriendo consola...';
                    
                    // GUARDAMOS EL USUARIO Y HACEMOS LA REDIRECCIÓN
                    localStorage.setItem('usuarioLogueado', data.usuario.nombres);
                    
                    // Redirigimos al Dashboard después de 1 segundo
                    setTimeout(() => {
                        window.location.href = 'dashboard.html';
                    }, 1000);
                    
                } else {
                    // Diseño rojo de error
                    alerta.className = 'alert-message bg-danger text-white';
                    alerta.textContent = '⚠️ ' + data.mensaje;
                }
            })
            .catch(error => {
                btnSubmit.innerHTML = 'INICIAR SESIÓN';
                btnSubmit.disabled = false;
                
                alerta.style.display = 'block';
                alerta.className = 'alert-message bg-danger text-white';
                alerta.textContent = '⚠️ No se pudo conectar con el servidor. Comprueba la conexión e inténtalo de nuevo.';
                console.error("Error en Fetch:", error);
            });
        });
    }
});

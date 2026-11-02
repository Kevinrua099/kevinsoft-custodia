# Conexión del sistema

La web debe abrirse a través del servidor HTTP, con las carpetas `web`, `api` y `uploads` dentro de la misma carpeta del proyecto. Por ejemplo: `http://localhost/custodia/web/`. Desde otro equipo se utiliza la dirección del servidor en lugar de `localhost`.

La aplicación Android utiliza por defecto `https://kevinsoft-custodia.onrender.com/api/`, tanto en teléfonos como en el emulador. Para aplicar esta configuración, recompila e instala el APK:

```powershell
.\gradlew.bat assembleDebug
```

Ejecuta el comando desde `Custodia_App/` e instala `app/build/outputs/apk/debug/app-debug.apk`. El servicio de Render puede tardar en responder después de un período de inactividad.

Para usar otro servidor, configura su dirección al compilar, por ejemplo para un teléfono en la misma red local:

```powershell
.\gradlew.bat assembleDebug -PcustodiaApiBaseUrl=http://192.168.1.10/custodia/api/
```

Sustituye la dirección por la del servidor. El teléfono debe poder acceder a esa dirección, y la URL debe terminar en `/`. Para el servidor del equipo anfitrión desde el emulador puedes usar `http://10.0.2.2/custodia/api/`. La misma configuración se usa para todas las peticiones de la app. Para un servidor local, comprueba que el servidor web y MySQL estén encendidos y que la base de datos `sistema_custodia_db` exista.

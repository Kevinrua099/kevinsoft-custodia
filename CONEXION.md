# Conexión del sistema

La web debe abrirse a través del servidor HTTP, con las carpetas `web`, `api` y `uploads` dentro de la misma carpeta del proyecto. Por ejemplo: `http://localhost/custodia/web/`. Desde otro equipo se utiliza la dirección del servidor en lugar de `localhost`.

La aplicación Android utiliza por defecto `http://10.0.2.2/custodia/api/`, dirección que corresponde al equipo anfitrión desde el emulador de Android. Para instalarla en un teléfono físico, configura la IP o el nombre accesible del servidor al compilar, por ejemplo:

```powershell
.\gradlew.bat assembleDebug -PcustodiaApiBaseUrl=http://192.168.1.10/custodia/api/
```

Sustituye la dirección por la del servidor. El teléfono debe poder acceder a esa dirección, y la URL debe terminar en `/`. La misma configuración se usa para todas las peticiones de la app. Comprueba también que el servidor web y MySQL estén encendidos y que la base de datos `sistema_custodia_db` exista.

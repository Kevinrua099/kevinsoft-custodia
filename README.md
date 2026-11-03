# KevinSoft — Sistema de custodia

Proyecto de presentación con una consola web, una API PHP/MySQL y una aplicación Android. La consola incluye panel, armería, salvoconductos, nómina y reportes; la app incluye acceso e inspección de patrullas.

## Estructura

- `web/`: interfaz web en HTML, CSS y JavaScript.
- `api/`: API PHP y conexión MySQL.
- `Custodia_App/`: aplicación Android (Kotlin y Jetpack Compose).
- `database/schema.sql`: estructura vacía de MySQL, sin datos personales. Para una base existente, las migraciones de `database/` agregan almacenamiento de fotos y el tipo de inspección sin borrar datos.
- `Dockerfile` y `render.yaml`: configuración para desplegar en Render.
- `CONEXION.md`: detalles de conexión local y Android.

## Ejecutar en el equipo

1. Ejecuta Apache y MySQL con Laragon. Mantén `api/` y `web/` dentro de la misma carpeta del proyecto, servida por Apache.
2. Crea la base de datos `sistema_custodia_db` e importa `database/schema.sql` **solo si es una base nueva y vacía**. Si ya tienes la base funcionando, conserva la existente.
3. Abre `http://localhost/custodia/web/` (ajusta `custodia` al nombre configurado en Apache). No abras el archivo HTML directamente desde el explorador.
4. El proyecto usa por defecto MySQL en `127.0.0.1:3306`, usuario `root` y contraseña vacía. Para otra configuración, consulta las variables de entorno más abajo.

La app Android usa por defecto la API publicada en `https://kevinsoft-custodia.onrender.com/api/`. Para conectar la app al servidor local, recompílala con una dirección accesible desde el dispositivo; `localhost` y `10.0.2.2` no apuntan al PC desde un teléfono físico. Consulta `CONEXION.md` para ver los comandos.

## Publicar en Render

Render ejecuta la web y la API como un **Web Service Docker**. La aplicación usa MySQL; la base PostgreSQL de Render no es compatible directamente. Para una presentación puedes usar, por ejemplo, [Aiven for MySQL](https://aiven.io/docs/products/mysql/get-started) con su plan gratuito, si está disponible en tu cuenta.

### 1. Preparar MySQL

1. Crea una cuenta en Aiven, crea un servicio **MySQL** y espera a que indique **Running**. En **Overview / Quick connect** copia host, puerto, base de datos, usuario y contraseña. Descarga también el certificado CA del servicio.
2. Con MySQL Workbench o HeidiSQL conecta con TLS usando el certificado CA. Importa `database/schema.sql` en la base de datos nueva del servicio (habitualmente `defaultdb`). La importación crea tablas vacías; **no copia el usuario ni los datos de tu PC**. Para una base existente, ejecuta `database/patrullas_migration.sql` y `database/reportes_fotos_migration.sql`; no vuelvas a importar el esquema completo.
3. Crea un usuario de presentación en la tabla `usuarios`. Genera primero un hash con PHP instalado localmente:

   ```powershell
   & "D:\laragon\bin\php\php-8.3.36-Win32-vs16-x64\php.exe" -r "echo password_hash('ELIGE_UNA_CLAVE', PASSWORD_DEFAULT);"
   ```

   Cambia la ruta si tu versión de PHP es distinta. En tu cliente SQL ejecuta lo siguiente, reemplazando cédula, nombre y hash por los tuyos:

   ```sql
   INSERT INTO usuarios (cedula, nombres_completos, cia_empresa, rol, password_hash, estado)
   VALUES ('CEDULA_DE_PRUEBA', 'Usuario de prueba', 'PRINCIPAL', 'Jefe', 'HASH_GENERADO', 'Activo');
   ```

   El inicio de sesión también acepta las claves antiguas en texto plano y las transforma en hash después de un acceso correcto.

### 2. Crear el servicio web

1. En [Render](https://dashboard.render.com/) conecta tu cuenta de GitHub y concede acceso al repositorio privado.
2. Selecciona **New → Blueprint**, elige este repositorio y usa `render.yaml`. El plan configurado es Free; revisa los detalles y precios en Render antes de confirmar.
3. Cuando Render solicite las variables, completa las siguientes con los datos de Aiven:

   | Variable | Valor |
   | --- | --- |
   | `DB_HOST` | Host MySQL, sin `https://` |
   | `DB_PORT` | Puerto MySQL del proveedor |
   | `DB_NAME` | Nombre de la base, por ejemplo `defaultdb` |
   | `DB_USER` | Usuario de MySQL |
   | `DB_PASSWORD` | Contraseña de MySQL |
   | `DB_SSL_CA_PEM` | Contenido completo del certificado CA, incluidas las líneas `BEGIN` y `END` |

   Guarda las credenciales en Render; no las subas al repositorio ni las envíes por chat. El certificado permite verificar la conexión TLS con MySQL.
4. Cuando el servicio termine de desplegar, abre `https://NOMBRE.onrender.com/`. La página `/api/conexion.php` permite comprobar la conexión con MySQL; `/health.php` comprueba que el servidor web esté vivo.

### 3. Conectar Android a Render

En PowerShell, desde `Custodia_App/`, recompila la app. Por defecto utiliza el servicio `kevinsoft-custodia`:

```powershell
.\gradlew.bat assembleDebug
```

Instala `Custodia_App/app/build/outputs/apk/debug/app-debug.apk` en el teléfono. Para utilizar otro servicio, añade `"-PcustodiaApiBaseUrl=https://NOMBRE.onrender.com/api/"` al comando y sustituye `NOMBRE` por el dominio asignado. La URL debe terminar en `/`.

### 4. Actualizar una instalación existente

1. Ejecuta `database/patrullas_entrega_migration.sql` y luego `database/reportes_custodia_migration.sql` una sola vez en la base de datos usada por el servicio. Si ya ejecutaste la primera, aplica solo la nueva. Si aún no se habían instalado las fotos, ejecuta también `database/reportes_fotos_migration.sql` y `database/patrullas_migration.sql`. Estas migraciones conservan los datos existentes.
2. Publica los cambios de `api/` y `web/` en el servicio de Render y espera a que termine el despliegue. La app nueva necesita la API actualizada para identificar al usuario y sus rutas.
3. Instala el APK nuevo e inicia sesión de nuevo. En la app, registra la recepción de la patrulla con al menos una foto. Al seleccionar a ese usuario como líder/chofer del salvoconducto, se propone su patrulla disponible y se puede elegir otra en el selector. La entrega también se registra en la app con fotos; ambas inspecciones aparecen en **Control de Patrullas**. Las inspecciones antiguas sin usuario identificado no generan una propuesta automática.
4. Asigna al usuario como chofer o custodio para que la ruta aparezca en **Custodia en ruta**. En una ruta terrestre o fluvial, agrega cada plataforma con su foto de guía y una o más piscinas con su libraje. En una ruta de contenedores, informa origen, destino y foto de guía. La app toma la patrulla de la última recepción vigente del usuario; si no existe, avisa y el reporte queda sin patrulla. La web muestra los datos y la patrulla guardada en cada reporte. Después de notificar el arribo, la web puede finalizar la ruta y la app actualizará el estado.

## Prueba de integración local

`tests/reportes_sync.test.js` comprueba rutas asignadas, inspecciones y fotos, reportes, arribo y finalización contra una API y MySQL locales. Prepara una base descartable cuyo nombre empiece por `custodia_test_`, importa `database/schema.sql` con UTF-8 y ejecuta la API local apuntando a esa base. En PowerShell, ajusta las rutas y puertos y ejecuta:

```powershell
$env:TEST_CUSTODIA_API_BASE = 'http://127.0.0.1:18081/'
$env:TEST_CUSTODIA_MYSQL_CLI = 'D:\laragon\bin\mysql\mysql-8.4.3-winx64\bin\mysql.exe'
$env:TEST_CUSTODIA_MYSQL_PORT = '33316'
$env:TEST_CUSTODIA_DB = 'custodia_test_sync'
node --test tests/reportes_sync.test.js
```

La prueba inserta datos de prueba; úsala únicamente con una base descartable y una API local configurada para esa base.

## Límites de la presentación

- La base de datos y los archivos locales no se suben automáticamente. El repositorio excluye evidencias, fotos, credenciales, archivos locales de Android y compilaciones.
- En Render Free, el servicio se suspende tras un periodo de inactividad y puede tardar en despertar. Las nuevas fotos de inspección y de reportes se guardan en MySQL; los archivos antiguos guardados en el disco del servicio pueden perderse al reiniciar o redesplegar.
- Los endpoints todavía no verifican una sesión ni permisos en el servidor. Usa solo datos ficticios para la presentación y no lo emplees con datos reales hasta implementar autenticación y permisos completos.
- El esquema vacío no incluye una cuenta inicial: crea el usuario de prueba como se indica arriba. Las credenciales de MySQL son distintas de las de acceso a KevinSoft.

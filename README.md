# KevinSoft — Sistema de custodia

Proyecto de presentación con una consola web, una API PHP/MySQL y una aplicación Android. La consola incluye panel, armería, salvoconductos, nómina y reportes; la app incluye acceso e inspección de patrullas.

## Estructura

- `web/`: interfaz web en HTML, CSS y JavaScript.
- `api/`: API PHP y conexión MySQL.
- `Custodia_App/`: aplicación Android (Kotlin y Jetpack Compose).
- `database/schema.sql`: estructura vacía de MySQL, sin datos personales. Para una base existente, `database/patrullas_migration.sql` agrega el almacenamiento de fotos de patrullas sin borrar datos.
- `Dockerfile` y `render.yaml`: configuración para desplegar en Render.
- `CONEXION.md`: detalles de conexión local y Android.

## Ejecutar en el equipo

1. Ejecuta Apache y MySQL con Laragon. Mantén `api/` y `web/` dentro de la misma carpeta del proyecto, servida por Apache.
2. Crea la base de datos `sistema_custodia_db` e importa `database/schema.sql` **solo si es una base nueva y vacía**. Si ya tienes la base funcionando, conserva la existente.
3. Abre `http://localhost/custodia/web/` (ajusta `custodia` al nombre configurado en Apache). No abras el archivo HTML directamente desde el explorador.
4. El proyecto usa por defecto MySQL en `127.0.0.1:3306`, usuario `root` y contraseña vacía. Para otra configuración, consulta las variables de entorno más abajo.

El emulador Android usa por defecto `http://10.0.2.2/custodia/api/`. Para un teléfono real usa una dirección accesible desde el teléfono y recompila la app; `localhost` y `10.0.2.2` no apuntan al PC desde un teléfono físico.

## Publicar en Render

Render ejecuta la web y la API como un **Web Service Docker**. La aplicación usa MySQL; la base PostgreSQL de Render no es compatible directamente. Para una presentación puedes usar, por ejemplo, [Aiven for MySQL](https://aiven.io/docs/products/mysql/get-started) con su plan gratuito, si está disponible en tu cuenta.

### 1. Preparar MySQL

1. Crea una cuenta en Aiven, crea un servicio **MySQL** y espera a que indique **Running**. En **Overview / Quick connect** copia host, puerto, base de datos, usuario y contraseña. Descarga también el certificado CA del servicio.
2. Con MySQL Workbench o HeidiSQL conecta con TLS usando el certificado CA. Importa `database/schema.sql` en la base de datos nueva del servicio (habitualmente `defaultdb`). La importación crea tablas vacías; **no copia el usuario ni los datos de tu PC**. Si ya habías importado el esquema antes de incorporar Control de Patrullas, ejecuta solo `database/patrullas_migration.sql` en esa base.
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

En PowerShell, desde `Custodia_App/`, recompila con la URL real del servicio:

```powershell
.\gradlew.bat assembleDebug "-PcustodiaApiBaseUrl=https://NOMBRE.onrender.com/api/"
```

Instala `Custodia_App/app/build/outputs/apk/debug/app-debug.apk` en el teléfono. Sustituye `NOMBRE` por el dominio asignado por Render. La URL debe terminar en `/`.

## Límites de la presentación

- La base de datos y los archivos locales no se suben automáticamente. El repositorio excluye evidencias, fotos, credenciales, archivos locales de Android y compilaciones.
- En Render Free, el servicio se suspende tras un periodo de inactividad y puede tardar en despertar. Las nuevas fotos de inspección de patrullas se guardan en MySQL; los demás archivos subidos se pierden al reiniciar, redesplegar o suspender y necesitan almacenamiento externo o un disco persistente.
- Los endpoints todavía no verifican una sesión ni permisos en el servidor, y el flujo de reportes Android contiene partes simuladas y una ruta fija. Usa solo datos ficticios para la presentación y no lo emplees con datos reales hasta implementar autenticación y permisos completos.
- El esquema vacío no incluye una cuenta inicial: crea el usuario de prueba como se indica arriba. Las credenciales de MySQL son distintas de las de acceso a KevinSoft.

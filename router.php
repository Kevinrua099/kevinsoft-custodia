<?php
// Los archivos cargados se sirven como datos, nunca como scripts PHP.
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
if (preg_match('#^/(?:web/)?uploads/.*\.(?:php\d*|phtml|phar)$#i', $path)) {
    http_response_code(403);
    exit('Acceso denegado');
}
return false;

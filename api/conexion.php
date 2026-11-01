<?php
// La configuración local es compatible con Laragon. En Render se usan variables de entorno.
function conectarBD(): PDO {
    $host = getenv('DB_HOST') ?: '127.0.0.1';
    $port = getenv('DB_PORT') ?: '3306';
    $dbname = getenv('DB_NAME') ?: 'sistema_custodia_db';
    $user = getenv('DB_USER') ?: 'root';
    $password = getenv('DB_PASSWORD');
    if ($password === false) $password = '';

    $options = [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION];
    $caFile = null;
    $caPem = getenv('DB_SSL_CA_PEM');
    if ($caPem) {
        $caFile = tempnam(sys_get_temp_dir(), 'custodia-ca-');
        if ($caFile === false || file_put_contents($caFile, $caPem) === false) {
            throw new PDOException('No se pudo preparar el certificado de la base de datos.');
        }
        $options[PDO::MYSQL_ATTR_SSL_CA] = $caFile;
        $options[PDO::MYSQL_ATTR_SSL_VERIFY_SERVER_CERT] = true;
    }

    try {
        return new PDO("mysql:host=$host;port=$port;dbname=$dbname;charset=utf8mb4", $user, $password, $options);
    } finally {
        if ($caFile !== null) unlink($caFile);
    }
}

// Permite comprobar la conexión al abrir este archivo directamente.
if (isset($_SERVER['SCRIPT_FILENAME']) && realpath($_SERVER['SCRIPT_FILENAME']) === __FILE__) {
    header('Content-Type: application/json; charset=utf-8');
    try {
        conectarBD();
        echo json_encode(['status' => 'success', 'mensaje' => 'Conexión con la base de datos establecida.']);
    } catch (PDOException $e) {
        http_response_code(503);
        error_log('Error de conexión a la base de datos: ' . $e->getMessage());
        echo json_encode(['status' => 'error', 'mensaje' => 'No se pudo conectar con la base de datos.']);
    }
}

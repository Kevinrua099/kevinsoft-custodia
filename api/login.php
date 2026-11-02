<?php
// Permitimos que nuestra página web consulte esta API
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

// Recibimos los datos que envía el formulario web (en formato JSON)
$datos = json_decode(file_get_contents("php://input"), true);

// Verificamos si enviaron cédula y contraseña
if(!isset($datos['cedula']) || !isset($datos['password'])) {
    echo json_encode(["status" => "error", "mensaje" => "Faltan datos de acceso"]);
    exit;
}

$cedula_ingresada = trim($datos['cedula']);
$password_ingresada = $datos['password'];

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();

    // Buscamos al usuario en la base de datos usando la cédula
    // Usamos consultas preparadas (prepare) para evitar inyecciones SQL (Hackers)
    $query = $conexion->prepare("SELECT id_usuario, nombres_completos, rol, password_hash, estado FROM usuarios WHERE cedula = :cedula");
    $query->bindParam(':cedula', $cedula_ingresada);
    $query->execute();
    
    $usuario = $query->fetch(PDO::FETCH_ASSOC);

    // Si el usuario existe
    if($usuario) {
        if($usuario['estado'] == 'Inactivo') {
             echo json_encode(["status" => "error", "mensaje" => "Usuario inactivo. Contacte a Sistemas."]);
             exit;
        }

        $hash = $usuario['password_hash'];
        $esHash = password_get_info($hash)['algo'] !== null;
        if (($esHash && password_verify($password_ingresada, $hash)) || (!$esHash && hash_equals($hash, $password_ingresada))) {
            // Actualiza automáticamente contraseñas antiguas en texto plano al entrar.
            if (!$esHash || password_needs_rehash($hash, PASSWORD_DEFAULT)) {
                $actualizar = $conexion->prepare('UPDATE usuarios SET password_hash = ? WHERE id_usuario = ?');
                $actualizar->execute([password_hash($password_ingresada, PASSWORD_DEFAULT), $usuario['id_usuario']]);
            }
            // Login Exitoso
            echo json_encode([
                "status" => "success", 
                "mensaje" => "Bienvenido " . $usuario['nombres_completos'],
                "usuario" => [
                    "id" => (int)$usuario['id_usuario'],
                    "nombres" => $usuario['nombres_completos'],
                    "rol" => $usuario['rol']
                ]
            ]);
        } else {
            echo json_encode(["status" => "error", "mensaje" => "Contraseña incorrecta"]);
        }
    } else {
        echo json_encode(["status" => "error", "mensaje" => "Usuario no encontrado"]);
    }

} catch(PDOException $e) {
    echo json_encode(["status" => "error", "mensaje" => "Error del servidor"]);
}
?>

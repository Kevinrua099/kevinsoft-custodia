<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();

    $metodo = $_SERVER['REQUEST_METHOD'];

    // LEER (GET)
    if ($metodo == 'GET') {
        $query = $conexion->query("SELECT id_usuario, cedula, nombres_completos, cia_empresa, rol, estado FROM usuarios ORDER BY nombres_completos ASC");
        $usuarios = $query->fetchAll(PDO::FETCH_ASSOC);
        echo json_encode(["status" => "success", "data" => $usuarios]);
    } 
    
    // CREAR (POST)
    elseif ($metodo == 'POST') {
        $datos = json_decode(file_get_contents("php://input"), true);
        $cedula = trim($datos['cedula']);
        
        $check = $conexion->prepare("SELECT id_usuario FROM usuarios WHERE cedula = :cedula");
        $check->bindParam(':cedula', $cedula);
        $check->execute();
        
        if($check->rowCount() > 0) {
            echo json_encode(["status" => "error", "mensaje" => "Esta cédula ya está registrada."]);
            exit;
        }

        $insert = $conexion->prepare("INSERT INTO usuarios (cedula, nombres_completos, cia_empresa, rol, password_hash, estado) VALUES (:cedula, :nombres, :cia, :rol, :pass, :estado)");
        $insert->bindParam(':cedula', $cedula);
        $insert->bindValue(':nombres', trim($datos['nombres']));
        $insert->bindValue(':cia', trim($datos['cia']));
        $insert->bindValue(':rol', trim($datos['rol']));
        $passwordHash = password_hash($datos['password'], PASSWORD_DEFAULT);
        $insert->bindParam(':pass', $passwordHash);
        $insert->bindParam(':estado', $datos['estado']);
        $insert->execute();

        echo json_encode(["status" => "success", "mensaje" => "Personal registrado correctamente."]);
    }

    // ACTUALIZAR (PUT)
    elseif ($metodo == 'PUT') {
        $datos = json_decode(file_get_contents("php://input"), true);
        
        $update = $conexion->prepare("UPDATE usuarios SET cedula=:cedula, nombres_completos=:nombres, cia_empresa=:cia, rol=:rol, estado=:estado WHERE id_usuario=:id");
        $update->bindValue(':cedula', trim($datos['cedula']));
        $update->bindValue(':nombres', trim($datos['nombres']));
        $update->bindValue(':cia', trim($datos['cia']));
        $update->bindValue(':rol', trim($datos['rol']));
        $update->bindParam(':estado', $datos['estado']);
        $update->bindParam(':id', $datos['id_usuario']);
        $update->execute();

        // Si enviaron contraseña nueva, la actualizamos también
        if(!empty($datos['password'])) {
            $passUpdate = $conexion->prepare("UPDATE usuarios SET password_hash=:pass WHERE id_usuario=:id");
            $passwordHash = password_hash($datos['password'], PASSWORD_DEFAULT);
            $passUpdate->bindParam(':pass', $passwordHash);
            $passUpdate->bindParam(':id', $datos['id_usuario']);
            $passUpdate->execute();
        }

        echo json_encode(["status" => "success", "mensaje" => "Registro actualizado."]);
    }

    // ELIMINAR (DELETE)
    elseif ($metodo == 'DELETE') {
        $datos = json_decode(file_get_contents("php://input"), true);
        
        // Cuidado: En sistemas de seguridad, no se borra la fila (porque rompe el historial de armas y rutas). 
        // Lo correcto es cambiar su estado a "Inactivo" en lugar de hacer un DELETE real.
        $delete = $conexion->prepare("UPDATE usuarios SET estado='Inactivo' WHERE id_usuario=:id");
        $delete->bindParam(':id', $datos['id_usuario']);
        $delete->execute();

        echo json_encode(["status" => "success", "mensaje" => "Personal dado de baja (Inactivo)."]);
    }

} catch(PDOException $e) {
    echo json_encode(["status" => "error", "mensaje" => "Error de BD: " . $e->getMessage()]);
}
?>

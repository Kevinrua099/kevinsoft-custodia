<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();
    $metodo = $_SERVER['REQUEST_METHOD'];

    // LEER
    if ($metodo == 'GET') {
        $query = $conexion->query("SELECT * FROM armamento ORDER BY marca, tipo_arma ASC");
        $armas = $query->fetchAll(PDO::FETCH_ASSOC);
        echo json_encode(["status" => "success", "data" => $armas]);
    } 
    
    // CREAR
    elseif ($metodo == 'POST') {
        $datos = json_decode(file_get_contents("php://input"), true);
        
        // Asignamos las variables primero (esto soluciona el error)
        $serie = trim(strtoupper($datos['serie']));
        $tipo = trim($datos['tipo']);
        $marca = trim(strtoupper($datos['marca']));
        $calibre = trim(strtoupper($datos['calibre']));

        $check = $conexion->prepare("SELECT id_arma FROM armamento WHERE serie = :serie");
        $check->bindParam(':serie', $serie);
        $check->execute();
        
        if($check->rowCount() > 0) {
            echo json_encode(["status" => "error", "mensaje" => "El numero de serie ya existe."]); exit;
        }

        $insert = $conexion->prepare("INSERT INTO armamento (serie, tipo_arma, marca, calibre, estado) VALUES (:serie, :tipo, :marca, :calibre, 'En Armería')");
        $insert->bindParam(':serie', $serie);
        $insert->bindParam(':tipo', $tipo);
        $insert->bindParam(':marca', $marca);
        $insert->bindParam(':calibre', $calibre);
        $insert->execute();

        echo json_encode(["status" => "success", "mensaje" => "Arma registrada exitosamente."]);
    }
    
    // ACTUALIZAR (EDICIÓN COMPLETA)
    elseif ($metodo == 'PUT') {
        $datos = json_decode(file_get_contents("php://input"), true);
        
        // Asignamos las variables primero
        $id_arma = $datos['id_arma'];
        $serie = trim(strtoupper($datos['serie']));
        $tipo = trim($datos['tipo']);
        $marca = trim(strtoupper($datos['marca']));
        $calibre = trim(strtoupper($datos['calibre']));
        $estado = $datos['estado'];

        $update = $conexion->prepare("UPDATE armamento SET serie=:serie, tipo_arma=:tipo, marca=:marca, calibre=:calibre, estado=:estado WHERE id_arma=:id");
        $update->bindParam(':serie', $serie);
        $update->bindParam(':tipo', $tipo);
        $update->bindParam(':marca', $marca);
        $update->bindParam(':calibre', $calibre);
        $update->bindParam(':estado', $estado);
        $update->bindParam(':id', $id_arma);
        $update->execute();
        
        echo json_encode(["status" => "success", "mensaje" => "Registro del arma actualizado."]);
    }

    // ELIMINAR (DAR DE BAJA LÓGICA)
    elseif ($metodo == 'DELETE') {
        $datos = json_decode(file_get_contents("php://input"), true);
        $id_arma = $datos['id_arma'];
        
        $delete = $conexion->prepare("UPDATE armamento SET estado='Inactiva' WHERE id_arma=:id");
        $delete->bindParam(':id', $id_arma);
        $delete->execute();
        
        echo json_encode(["status" => "success", "mensaje" => "Arma dada de baja (Inactiva)."]);
    }

} catch(PDOException $e) {
    echo json_encode(["status" => "error", "mensaje" => "Error de BD"]);
}
?>

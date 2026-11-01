<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
header('Access-Control-Allow-Methods: GET, POST');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();
    $metodo = $_SERVER['REQUEST_METHOD'];

    if ($metodo == 'GET') {
        $query = $conexion->query("SELECT * FROM registro_bandidas ORDER BY fecha_registro DESC");
        echo json_encode(["status" => "success", "data" => $query->fetchAll(PDO::FETCH_ASSOC)]);
    } 
    elseif ($metodo == 'POST') {
        $datos = json_decode(file_get_contents("php://input"), true);
        
        $insert = $conexion->prepare("INSERT INTO registro_bandidas (alias, nombre_completo, zona_frecuente, nivel_riesgo, observaciones, registrado_por) VALUES (:alias, :nombre, :zona, :riesgo, :obs, :creador)");
        $insert->execute([
            ':alias' => trim($datos['alias']),
            ':nombre' => trim($datos['nombre_completo']),
            ':zona' => trim($datos['zona_frecuente']),
            ':riesgo' => $datos['nivel_riesgo'],
            ':obs' => trim($datos['observaciones']),
            ':creador' => $datos['creador']
        ]);

        echo json_encode(["status" => "success", "mensaje" => "Registro guardado exitosamente."]);
    }
} catch(PDOException $e) {
    echo json_encode(["status" => "error", "mensaje" => "Error de BD: " . $e->getMessage()]);
}
?>

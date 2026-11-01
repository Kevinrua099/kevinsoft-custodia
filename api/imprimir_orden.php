<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();

    // Verificamos si nos enviaron el número de registro por la URL (Ej: ?orden=ORD-123)
    if(isset($_GET['orden'])) {
        $num_orden = $_GET['orden'];

        // 1. Buscamos la cabecera de la ruta y el vehículo
        $q_ruta = $conexion->prepare("SELECT r.*, v.placa, v.cia_vehiculo FROM salvoconductos_rutas r LEFT JOIN vehiculos v ON r.id_vehiculo = v.id_vehiculo WHERE r.numero_registro = :orden");
        $q_ruta->bindParam(':orden', $num_orden);
        $q_ruta->execute();
        $ruta = $q_ruta->fetch(PDO::FETCH_ASSOC);

        if(!$ruta) {
            echo json_encode(["status" => "error", "mensaje" => "Orden no encontrada."]);
            exit;
        }

        // 2. Buscamos el personal y armas asignadas a esta ruta específica
        $q_personal = $conexion->prepare("SELECT a.rol_en_ruta, u.nombres_completos, u.cedula, u.cia_empresa, arm.serie, arm.tipo_arma, arm.marca, arm.calibre 
                                          FROM asignacion_personal_armas a 
                                          JOIN usuarios u ON a.id_usuario = u.id_usuario 
                                          LEFT JOIN armamento arm ON a.id_arma = arm.id_arma 
                                          WHERE a.id_ruta = :id_ruta");
        $q_personal->bindParam(':id_ruta', $ruta['id_ruta']);
        $q_personal->execute();
        $personal = $q_personal->fetchAll(PDO::FETCH_ASSOC);

        // Devolvemos todo junto
        echo json_encode(["status" => "success", "ruta" => $ruta, "personal" => $personal]);
    } else {
        echo json_encode(["status" => "error", "mensaje" => "Falta el número de orden."]);
    }
} catch(PDOException $e) {
    echo json_encode(["status" => "error", "mensaje" => "Error de BD"]);
}
?>

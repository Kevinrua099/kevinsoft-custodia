<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();

    // Aquí consultamos el historial de rutas, uniendo los datos del vehículo
    // (Opcional: Filtrar por el ID del custodio si ya manejas sesiones en la app)
    $query = "SELECT s.numero_registro, s.fecha_emision, s.origen, s.destino, s.estado_ruta, 
                     v.placa, v.disco 
              FROM salvoconductos_rutas s 
              LEFT JOIN vehiculos v ON s.id_vehiculo = v.id_vehiculo 
              ORDER BY s.id_ruta DESC 
              LIMIT 15"; // Traemos los últimos 15 viajes

    $stmt = $conexion->query($query);
    $historial = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode(["status" => "success", "data" => $historial]);

} catch(PDOException $e) {
    error_log('Error al consultar historial: ' . $e->getMessage());
    echo json_encode(["status" => "error", "mensaje" => "Error de base de datos."]);
}
?>

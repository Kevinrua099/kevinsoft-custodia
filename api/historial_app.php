<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();

    $usuario = filter_input(INPUT_GET, 'usuario', FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if (!$usuario) {
        echo json_encode(['status' => 'error', 'mensaje' => 'Inicia sesión para consultar tus rutas.']);
        exit;
    }
    $query = "SELECT s.numero_registro, s.fecha_emision, s.origen, s.destino, s.estado_ruta,
                     v.placa, v.disco 
              FROM salvoconductos_rutas s 
              LEFT JOIN vehiculos v ON s.id_vehiculo = v.id_vehiculo
              WHERE EXISTS (SELECT 1 FROM asignacion_personal_armas a WHERE a.id_ruta = s.id_ruta AND a.id_usuario = ?)
              ORDER BY s.id_ruta DESC 
              LIMIT 100";

    $stmt = $conexion->prepare($query);
    $stmt->execute([$usuario]);
    $historial = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode(["status" => "success", "data" => $historial]);

} catch(PDOException $e) {
    error_log('Error al consultar historial: ' . $e->getMessage());
    echo json_encode(["status" => "error", "mensaje" => "Error de base de datos."]);
}
?>

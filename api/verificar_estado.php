<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();
    
    // Recibimos el número de ruta (ej: ORD-2026-1792104421)
    $numero_registro = $_GET['ruta'] ?? '';

    $stmt = $conexion->prepare("SELECT estado_ruta FROM salvoconductos_rutas WHERE numero_registro = ?");
    $stmt->execute([$numero_registro]);
    $resultado = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($resultado) {
        echo json_encode(["status" => "success", "estado_ruta" => $resultado['estado_ruta']]);
    } else {
        echo json_encode(["status" => "error", "mensaje" => "Ruta no encontrada"]);
    }
} catch(PDOException $e) {
    error_log('Error al verificar ruta: ' . $e->getMessage());
    echo json_encode(["status" => "error", "mensaje" => "Error de base de datos."]);
}
?>

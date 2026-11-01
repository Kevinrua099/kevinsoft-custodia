<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();

    // 1. Estadísticas para las tarjetas superiores
    $q_rutas = $conexion->query("SELECT COUNT(*) as total FROM salvoconductos_rutas WHERE estado_ruta = 'En Tránsito'");
    $rutas_activas = $q_rutas->fetch(PDO::FETCH_ASSOC)['total'];
    
    $q_armas = $conexion->query("SELECT COUNT(*) as total FROM armamento WHERE estado = 'Entregada'");
    $armas_entregadas = $q_armas->fetch(PDO::FETCH_ASSOC)['total'];

    $q_personal = $conexion->query("SELECT COUNT(*) as total FROM usuarios WHERE estado = 'Activo'");
    $personal_activo = $q_personal->fetch(PDO::FETCH_ASSOC)['total'];

    // 2. Historial de los últimos 20 salvoconductos
    $q_historial = $conexion->query("SELECT numero_registro, fecha_emision, asunto, creado_por, estado_ruta FROM salvoconductos_rutas ORDER BY fecha_emision DESC LIMIT 20");
    $historial = $q_historial->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "status" => "success", 
        "stats" => [
            "rutas_activas" => $rutas_activas,
            "armas_entregadas" => $armas_entregadas,
            "personal_activo" => $personal_activo
        ],
        "historial" => $historial
    ]);

} catch(PDOException $e) {
    echo json_encode(["status" => "error", "mensaje" => "Error de BD"]);
}
?>

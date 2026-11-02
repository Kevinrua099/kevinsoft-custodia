<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
header('Access-Control-Allow-Methods: POST');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();
    $datos = json_decode(file_get_contents("php://input"), true);
    
    if(isset($datos['numero_registro'])) {
        $num = $datos['numero_registro'];
        $conexion->beginTransaction();

        // 1. Obtener ID de la ruta
        $q = $conexion->prepare("SELECT id_ruta, id_vehiculo FROM salvoconductos_rutas WHERE numero_registro = ?");
        $q->execute([$num]);
        $ruta = $q->fetch(PDO::FETCH_ASSOC);
        if (!$ruta) {
            $conexion->rollBack();
            echo json_encode(['status' => 'error', 'mensaje' => 'Ruta no encontrada.']);
            exit;
        }

        // 2. Finalizar Ruta
        $conexion->prepare("UPDATE salvoconductos_rutas SET estado_ruta = 'Finalizada' WHERE id_ruta = ?")->execute([$ruta['id_ruta']]);
        
        // 3. Liberar Vehículo
        $conexion->prepare("UPDATE vehiculos SET estado = 'Disponible' WHERE id_vehiculo = ?")->execute([$ruta['id_vehiculo']]);

        // 4. Liberar Armas y Personal
        $conexion->prepare("UPDATE armamento SET estado = 'En Armería' WHERE id_arma IN (SELECT id_arma FROM asignacion_personal_armas WHERE id_ruta = ? AND id_arma IS NOT NULL)")->execute([$ruta['id_ruta']]);
        $conexion->prepare("UPDATE asignacion_personal_armas SET fecha_hora_devolucion = NOW() WHERE id_ruta = ?")->execute([$ruta['id_ruta']]);

        $conexion->commit();
        echo json_encode(["status" => "success", "mensaje" => "Ruta dada de alta exitosamente."]);
    }
} catch(Exception $e) {
    if(isset($conexion) && $conexion->inTransaction()) $conexion->rollBack();
    echo json_encode(["status" => "error", "mensaje" => "Error al finalizar ruta."]);
}
?>

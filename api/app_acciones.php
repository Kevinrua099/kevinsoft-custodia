<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();
    
    $datos = json_decode(file_get_contents("php://input"), true);
    $accion = $datos['accion']; 
    
    // NOTA: Para esta prueba estamos asumiendo que estamos trabajando sobre la ruta con ID 1.
    // En el futuro, la app enviará su ID de ruta exacto.
    $id_ruta = 1; 

    if($accion == 'novedad') {
        $obs = $datos['observacion'];
        $conexion->prepare("INSERT INTO reportes_pesca (id_ruta, fecha_hora_reporte, origen_camaronera, observaciones, tipo_reporte) VALUES (?, NOW(), 'EN RUTA', ?, 'Novedad en Vía')")->execute([$id_ruta, $obs]);
        echo json_encode(["status" => "success", "mensaje" => "Novedad transmitida a consola."]);
    }
    elseif($accion == 'arribo') {
        $conexion->prepare("INSERT INTO reportes_pesca (id_ruta, fecha_hora_reporte, origen_camaronera, observaciones, tipo_reporte) VALUES (?, NOW(), 'PLANTA', 'El custodio notificó el arribo a su destino de forma segura.', 'Arribo a Destino')")->execute([$id_ruta]);
        $conexion->prepare("UPDATE salvoconductos_rutas SET estado_ruta = 'En espera de confirmación' WHERE id_ruta = ?")->execute([$id_ruta]);
        echo json_encode(["status" => "success", "mensaje" => "Arribo notificado a consola."]);
    }
    elseif($accion == 'verificar') {
        // Consultamos si el Jefe ya dio de "Alta" (Finalizó la ruta)
        $q = $conexion->query("SELECT estado_ruta FROM salvoconductos_rutas WHERE id_ruta = $id_ruta");
        $estado = $q->fetch(PDO::FETCH_ASSOC)['estado_ruta'];
        
        echo json_encode(["status" => "success", "estado_ruta" => $estado]);
    }
} catch(Exception $e) {
    echo json_encode(["status" => "error", "mensaje" => "Error del servidor."]);
}
?>

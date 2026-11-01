<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();

    // Corregido con los nombres reales de tus tablas de HeidiSQL
    $query = "SELECT rp.*, 
              COALESCE(sr.numero_registro, 'Ruta General (Sin Salvoconducto)') as numero_registro, 
              COALESCE(v.placa, 'S/N') as placa, 
              COALESCE(v.disco, 'S/N') as disco, 
              COALESCE(sr.estado_ruta, 'Activa') as estado_ruta
              FROM reportes_pesca rp
              LEFT JOIN salvoconductos_rutas sr ON rp.id_ruta = sr.id_ruta
              LEFT JOIN vehiculos v ON sr.id_vehiculo = v.id_vehiculo
              ORDER BY rp.fecha_hora_reporte DESC";

    $stmt = $conexion->prepare($query);
    $stmt->execute();
    $reportes = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $resultado = [];
    foreach ($reportes as $rep) {
        $q_guias = $conexion->prepare("SELECT * FROM guias_plataformas WHERE id_reporte = ?");
        $q_guias->execute([$rep['id_reporte']]);
        $guias = $q_guias->fetchAll(PDO::FETCH_ASSOC);

        foreach($guias as &$guia) {
            $q_piscinas = $conexion->prepare("SELECT * FROM detalle_piscinas WHERE id_guia = ?");
            $q_piscinas->execute([$guia['id_guia']]);
            $guia['piscinas'] = $q_piscinas->fetchAll(PDO::FETCH_ASSOC);
        }
        $rep['guias'] = $guias;

        $q_evidencias = $conexion->prepare("SELECT * FROM evidencias_reporte WHERE id_reporte = ?");
        $q_evidencias->execute([$rep['id_reporte']]);
        $rep['evidencias'] = $q_evidencias->fetchAll(PDO::FETCH_ASSOC);

        $resultado[] = $rep;
    }

    echo json_encode(["status" => "success", "data" => $resultado]);

} catch(PDOException $e) {
    error_log('Error al consultar reportes: ' . $e->getMessage());
    echo json_encode(["status" => "error", "mensaje" => "Error de base de datos."]);
}
?>

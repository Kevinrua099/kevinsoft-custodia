<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();
    $conexion->beginTransaction();

    // 1. Recibir datos del formulario (FormData) enviados por Kotlin
    $origen = $_POST['origen'] ?? '';
    $data_json = $_POST['data_json'] ?? '[]';
    $observaciones = $_POST['observaciones'] ?? '';
    $tipo_reporte = $_POST['tipo_reporte'] ?? 'Reporte en Ruta';
    
    // Por defecto asignamos a la ruta 1 (Puedes cambiar esto luego para que sea dinámico)
    $id_ruta = 1;

    // 2. Insertar el reporte general
    $stmt_rep = $conexion->prepare("INSERT INTO reportes_pesca (id_ruta, origen_camaronera, observaciones, tipo_reporte, fecha_hora_reporte) VALUES (?, ?, ?, ?, NOW())");
    $stmt_rep->execute([$id_ruta, $origen, $observaciones, $tipo_reporte]);
    $id_reporte = $conexion->lastInsertId();

    // 3. Procesar las plataformas y piscinas desde el JSON
    $plataformas = json_decode($data_json, true);
    if (is_array($plataformas)) {
        // CORRECCIÓN APLICADA: Se usa identificador_plataforma en lugar de numero_guia
        $stmt_guia = $conexion->prepare("INSERT INTO guias_plataformas (id_reporte, identificador_plataforma) VALUES (?, ?)");
        $stmt_piscina = $conexion->prepare("INSERT INTO detalle_piscinas (id_guia, numero_piscina, libraje) VALUES (?, ?, ?)");
        
        foreach ($plataformas as $plat) {
            $identificador = $plat['identificador'] ?? 'S/N';
            $stmt_guia->execute([$id_reporte, $identificador]);
            $id_guia = $conexion->lastInsertId();

            if (isset($plat['piscinas']) && is_array($plat['piscinas'])) {
                foreach ($plat['piscinas'] as $piscina) {
                    $num = $piscina['numero'] ?? '';
                    $libr = $piscina['libraje'] ?? '';
                    if (!empty($num) || !empty($libr)) {
                        $stmt_piscina->execute([$id_guia, $num, $libr]);
                    }
                }
            }
        }
    }

    // 4. Procesar archivos adjuntos (Fotos y Videos de Galería/Cámara)
    $upload_dir = '../uploads/';
    if (!file_exists($upload_dir)) {
        mkdir($upload_dir, 0777, true);
    }

    $stmt_evidencia = $conexion->prepare("INSERT INTO evidencias_reporte (id_reporte, ruta_archivo, tipo_archivo) VALUES (?, ?, ?)");

    foreach ($_FILES as $key => $file) {
        if ($file['error'] === UPLOAD_ERR_OK) {
            $ext = pathinfo($file['name'], PATHINFO_EXTENSION);
            $new_name = uniqid('ev_') . '.' . $ext;
            $destino = $upload_dir . $new_name;
            
            if (move_uploaded_file($file['tmp_name'], $destino)) {
                // Si el MIME type dice video, es video, si no, es foto
                $tipo_archivo = (strpos($file['type'], 'video') !== false) ? 'video' : 'foto';
                $ruta_bd = 'uploads/' . $new_name; // Ruta relativa para la web
                $stmt_evidencia->execute([$id_reporte, $ruta_bd, $tipo_archivo]);
            }
        }
    }

    // 5. LÓGICA DE ACTUALIZACIÓN DE ESTADO SI ES "ARRIBO"
    // Convertimos el texto a mayúsculas para que siempre coincida sin importar cómo lo envíe la app
    $tipo_upper = strtoupper(trim($tipo_reporte));

    if ($tipo_upper === 'ARRIBO' || $tipo_upper === 'ARRIBO A DESTINO') {
        $query_estado = "UPDATE salvoconductos_rutas 
                         SET estado_ruta = 'En espera de confirmación' 
                         WHERE id_ruta = ?";
        $stmt_estado = $conexion->prepare($query_estado);
        $stmt_estado->execute([$id_ruta]);
    }

    $conexion->commit();
    echo json_encode(["status" => "success", "mensaje" => "Reporte registrado correctamente"]);
} catch (PDOException $e) {
    if (isset($conexion) && $conexion->inTransaction()) {
        $conexion->rollBack();
    }
    http_response_code(500);
    error_log('Error al registrar reporte: ' . $e->getMessage());
    echo json_encode(["status" => "error", "mensaje" => "No se pudo registrar el reporte"]);
}

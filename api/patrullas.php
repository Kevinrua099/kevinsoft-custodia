<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();

    if ($_SERVER['REQUEST_METHOD'] == 'POST') {
        $usuario = $_POST['usuario_nombre'] ?? 'Desconocido';
        $placa = strtoupper(trim($_POST['placa'] ?? ''));
        $disco = strtoupper(trim($_POST['disco'] ?? ''));
        $kilometraje = $_POST['kilometraje'] ?? 0;
        $observaciones = trim($_POST['observaciones'] ?? '');

        $conexion->beginTransaction();

        $stmt = $conexion->prepare("INSERT INTO inspeccion_vehiculos (usuario_nombre, placa, disco, kilometraje, observaciones_estado) VALUES (?, ?, ?, ?, ?)");
        $stmt->execute([$usuario, $placa, $disco, $kilometraje, $observaciones]);
        $id_inspeccion = $conexion->lastInsertId();

        $stmt_foto = $conexion->prepare("INSERT INTO inspeccion_fotos (id_inspeccion, ruta_foto) VALUES (?, ?)");

        // Procesar múltiples fotos dinámicamente
        $dir = '../web/uploads/patrullas/';
        if (!file_exists($dir)) mkdir($dir, 0777, true);

        foreach ($_FILES as $key => $file) {
            if (strpos($key, 'foto_') === 0 && $file['error'] === UPLOAD_ERR_OK) {
                $nombre_archivo = "insp_" . time() . "_" . $key . ".jpg";
                if (move_uploaded_file($file['tmp_name'], $dir . $nombre_archivo)) {
                    $stmt_foto->execute([$id_inspeccion, "uploads/patrullas/" . $nombre_archivo]);
                }
            }
        }

        $conexion->commit();
        echo json_encode(["status" => "success", "mensaje" => "Inspección de patrulla registrada con éxito."]);
    }
} catch(Exception $e) {
    if(isset($conexion) && $conexion->inTransaction()) $conexion->rollBack();
    error_log('Error de inspección: ' . $e->getMessage());
    echo json_encode(["status" => "error", "mensaje" => "Error del servidor."]);
}
?>

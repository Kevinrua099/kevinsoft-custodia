<?php
header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();
    $metodo = $_SERVER['REQUEST_METHOD'];

    if ($metodo === 'GET' && isset($_GET['foto'])) {
        $idFoto = filter_var($_GET['foto'], FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
        if (!$idFoto) {
            http_response_code(400);
            echo json_encode(['status' => 'error', 'mensaje' => 'Foto no válida.']);
            exit;
        }
        $consulta = $conexion->prepare('SELECT contenido, tipo_mime FROM inspeccion_fotos_archivo WHERE id_foto = ?');
        $consulta->execute([$idFoto]);
        $foto = $consulta->fetch(PDO::FETCH_ASSOC);
        if (!$foto) {
            http_response_code(404);
            echo json_encode(['status' => 'error', 'mensaje' => 'Foto no encontrada.']);
            exit;
        }
        header('Content-Type: ' . $foto['tipo_mime']);
        header('X-Content-Type-Options: nosniff');
        header('Cache-Control: private, max-age=3600');
        echo $foto['contenido'];
        exit;
    }

    if ($metodo === 'GET') {
        // Limitar inspecciones antes de unir las fotos para no cortar una galería.
        $query = $conexion->query('SELECT i.*, f.id_foto, f.ruta_foto, a.id_foto AS foto_guardada
            FROM (SELECT * FROM inspeccion_vehiculos ORDER BY id_inspeccion DESC LIMIT 500) i
            LEFT JOIN inspeccion_fotos f ON f.id_inspeccion = i.id_inspeccion
            LEFT JOIN inspeccion_fotos_archivo a ON a.id_foto = f.id_foto
            ORDER BY i.id_inspeccion DESC, f.id_foto ASC');
        $inspecciones = [];
        while ($fila = $query->fetch(PDO::FETCH_ASSOC)) {
            $id = $fila['id_inspeccion'];
            if (!isset($inspecciones[$id])) {
                $inspecciones[$id] = [
                    'id_inspeccion' => $id,
                    'placa' => $fila['placa'],
                    'disco' => $fila['disco'],
                    'kilometraje' => $fila['kilometraje'],
                    'observaciones' => $fila['observaciones_estado'],
                    'fecha_hora_recepcion' => $fila['fecha_hora_recepcion'],
                    'usuario_nombre' => $fila['usuario_nombre'],
                    'id_usuario' => $fila['id_usuario'],
                    'tipo_inspeccion' => $fila['tipo_inspeccion'],
                    'fotos' => [],
                ];
            }
            if ($fila['id_foto']) {
                $inspecciones[$id]['fotos'][] = [
                    'id' => $fila['id_foto'],
                    'guardada' => $fila['foto_guardada'] !== null,
                    'ruta_anterior' => $fila['ruta_foto'],
                ];
            }
        }
        echo json_encode(['status' => 'success', 'data' => array_values($inspecciones)]);
        exit;
    }

    if ($metodo !== 'POST') {
        http_response_code(405);
        header('Allow: GET, POST');
        echo json_encode(['status' => 'error', 'mensaje' => 'Método no permitido.']);
        exit;
    }

    $usuario = trim($_POST['usuario_nombre'] ?? '') ?: 'Desconocido';
    $usuarioId = filter_var($_POST['usuario_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    $tipo = trim($_POST['tipo_inspeccion'] ?? 'Recepción');
    if (!in_array($tipo, ['Recepción', 'Entrega'], true) || ($tipo === 'Entrega' && !$usuarioId)) {
        echo json_encode(['status' => 'error', 'mensaje' => 'Selecciona un tipo de inspección y un usuario válidos.']);
        exit;
    }
    if ($usuarioId) {
        $consultaUsuario = $conexion->prepare("SELECT nombres_completos, cia_empresa FROM usuarios WHERE id_usuario = ? AND estado = 'Activo'");
        $consultaUsuario->execute([$usuarioId]);
        $datosUsuario = $consultaUsuario->fetch(PDO::FETCH_ASSOC);
        if (!$datosUsuario) {
            echo json_encode(['status' => 'error', 'mensaje' => 'Usuario no encontrado. Inicia sesión de nuevo.']);
            exit;
        }
        $usuario = $datosUsuario['nombres_completos'];
    }
    $placa = strtoupper(trim($_POST['placa'] ?? ''));
    $disco = strtoupper(trim($_POST['disco'] ?? ''));
    $kmTexto = trim($_POST['kilometraje'] ?? '');
    $observaciones = trim($_POST['observaciones'] ?? '');
    if ($placa === '' || $disco === '' || strlen($placa) > 20 || strlen($disco) > 50 || strlen($usuario) > 100) {
        echo json_encode(['status' => 'error', 'mensaje' => 'Indica una placa y un disco válidos.']);
        exit;
    }
    if ($kmTexto !== '' && (!ctype_digit($kmTexto) || (float)$kmTexto > 2147483647)) {
        echo json_encode(['status' => 'error', 'mensaje' => 'El kilometraje debe ser un número entero válido.']);
        exit;
    }
    $kilometraje = $kmTexto === '' ? null : (int)$kmTexto;

    $archivos = [];
    foreach ($_FILES as $key => $file) {
        if (strpos($key, 'foto_') !== 0) continue;
        if ($file['error'] !== UPLOAD_ERR_OK || $file['size'] > 2 * 1024 * 1024) {
            echo json_encode(['status' => 'error', 'mensaje' => 'No se pudo subir una foto. El máximo es 2 MB por imagen.']);
            exit;
        }
        $info = getimagesize($file['tmp_name']);
        if (!$info || !in_array($info['mime'], ['image/jpeg', 'image/png', 'image/webp'], true)) {
            echo json_encode(['status' => 'error', 'mensaje' => 'Las fotos deben ser imágenes JPEG, PNG o WebP.']);
            exit;
        }
        $contenido = file_get_contents($file['tmp_name']);
        if ($contenido === false) throw new RuntimeException('No se pudo leer una foto.');
        $archivos[] = ['contenido' => $contenido, 'mime' => $info['mime']];
    }
    if (count($archivos) > 8) {
        echo json_encode(['status' => 'error', 'mensaje' => 'Puedes adjuntar hasta 8 fotos.']);
        exit;
    }
    if ($usuarioId && count($archivos) === 0) {
        echo json_encode(['status' => 'error', 'mensaje' => 'Adjunta al menos una foto del estado de la patrulla.']);
        exit;
    }

    $conexion->beginTransaction();
    if ($usuarioId && $tipo === 'Recepción') {
        // La placa permite relacionar la inspección con el vehículo del salvoconducto.
        $vehiculo = $conexion->prepare("INSERT INTO vehiculos (placa, disco, cia_vehiculo) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE disco = VALUES(disco)");
        $vehiculo->execute([$placa, $disco, $datosUsuario['cia_empresa'] ?: 'PRINCIPAL']);
    }
    $stmt = $conexion->prepare('INSERT INTO inspeccion_vehiculos (id_usuario, usuario_nombre, placa, disco, kilometraje, observaciones_estado, tipo_inspeccion) VALUES (?, ?, ?, ?, ?, ?, ?)');
    $stmt->execute([$usuarioId ?: null, $usuario, $placa, $disco, $kilometraje, $observaciones, $tipo]);
    $idInspeccion = $conexion->lastInsertId();
    $stmtFoto = $conexion->prepare('INSERT INTO inspeccion_fotos (id_inspeccion, ruta_foto) VALUES (?, NULL)');
    $stmtArchivo = $conexion->prepare('INSERT INTO inspeccion_fotos_archivo (id_foto, tipo_mime, contenido) VALUES (?, ?, ?)');
    foreach ($archivos as $archivo) {
        $stmtFoto->execute([$idInspeccion]);
        $stmtArchivo->bindValue(1, $conexion->lastInsertId(), PDO::PARAM_INT);
        $stmtArchivo->bindValue(2, $archivo['mime']);
        $stmtArchivo->bindValue(3, $archivo['contenido'], PDO::PARAM_LOB);
        $stmtArchivo->execute();
    }
    $conexion->commit();
    echo json_encode(['status' => 'success', 'mensaje' => 'Inspección de patrulla registrada con éxito.']);
} catch (Throwable $e) {
    if (isset($conexion) && $conexion->inTransaction()) $conexion->rollBack();
    error_log('Error de inspección: ' . $e->getMessage());
    http_response_code(500);
    echo json_encode(['status' => 'error', 'mensaje' => 'No se pudo procesar la inspección.']);
}

<?php
header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/conexion.php';

function rechazarReporte(string $mensaje): void {
    echo json_encode(['status' => 'error', 'mensaje' => $mensaje]);
    exit;
}

try {
    $conexion = conectarBD();
    if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['evidencia'])) {
        $id = filter_var($_GET['evidencia'], FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
        if (!$id) { http_response_code(400); rechazarReporte('Evidencia no válida.'); }
        $consulta = $conexion->prepare('SELECT tipo_mime, contenido FROM evidencias_reporte_archivo WHERE id_evidencia = ?');
        $consulta->execute([$id]);
        $archivo = $consulta->fetch(PDO::FETCH_ASSOC);
        if (!$archivo) { http_response_code(404); rechazarReporte('Evidencia no encontrada.'); }
        header('Content-Type: ' . $archivo['tipo_mime']);
        header('X-Content-Type-Options: nosniff');
        header('Cache-Control: private, max-age=3600');
        echo $archivo['contenido'];
        exit;
    }
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        http_response_code(405);
        rechazarReporte('Método no permitido.');
    }

    $numero = trim($_POST['numero_registro'] ?? '');
    $usuario = filter_var($_POST['usuario_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    $origen = trim($_POST['origen'] ?? '');
    $observaciones = trim($_POST['observaciones'] ?? '');
    $tipo = trim($_POST['tipo_reporte'] ?? '');
    $plataformas = json_decode($_POST['data_json'] ?? '[]', true);
    if ($numero === '' || strlen($numero) > 50 || !$usuario) rechazarReporte('Selecciona una ruta asignada.');
    if (strlen($origen) > 100 || !in_array($tipo, ['Inicio de Ruta', 'Pesca', 'Novedad en Vía', 'Arribo a Destino'], true)) rechazarReporte('Datos del reporte no válidos.');
    if ($tipo === 'Novedad en Vía' && $observaciones === '') rechazarReporte('Describe la novedad.');
    if (!is_array($plataformas) || count($plataformas) > 8) rechazarReporte('Las plataformas no son válidas.');
    $esCarga = in_array($tipo, ['Inicio de Ruta', 'Pesca'], true);
    if ($esCarga && count($plataformas) === 0) rechazarReporte('Agrega al menos una plataforma.');
    if (!$esCarga && count($plataformas) !== 0) rechazarReporte('Este tipo de reporte no admite plataformas.');
    $camposFotos = [];
    foreach ($plataformas as $plat) {
        if (!is_array($plat) || !isset($plat['identificador'], $plat['piscinas']) || !is_string($plat['identificador']) || trim($plat['identificador']) === '' || strlen($plat['identificador']) > 50 || !is_array($plat['piscinas']) || count($plat['piscinas']) < 1 || count($plat['piscinas']) > 50) rechazarReporte('Revisa los datos de las plataformas.');
        $campo = $plat['foto_campo'] ?? '';
        if (!is_string($campo) || !preg_match('/^foto_[0-7]$/', $campo) || in_array($campo, $camposFotos, true)) rechazarReporte('Cada plataforma debe tener su propia foto de guía.');
        $camposFotos[] = $campo;
        foreach ($plat['piscinas'] as $piscina) {
            if (!is_array($piscina)) rechazarReporte('Revisa los datos de las piscinas.');
            $num = $piscina['numero'] ?? '';
            $libras = $piscina['libraje'] ?? '';
            if (!is_string($num) || trim($num) === '' || strlen($num) > 50 || !is_string($libras) || !preg_match('/^\d{1,8}(?:\.\d{1,2})?$/', $libras) || (float)$libras <= 0 || (float)$libras > 99999999.99) rechazarReporte('Indica la piscina y un libraje mayor que cero, con hasta dos decimales.');
        }
    }

    $archivos = [];
    foreach ($_FILES as $key => $file) {
        if (!preg_match('/^foto_[0-7]$/', $key) || !is_array($file) || $file['error'] !== UPLOAD_ERR_OK || $file['size'] > 2 * 1024 * 1024) rechazarReporte('No se pudo subir una foto (máximo 2 MB por imagen).');
        $info = getimagesize($file['tmp_name']);
        if (!$info || !in_array($info['mime'], ['image/jpeg', 'image/png', 'image/webp'], true)) rechazarReporte('Las evidencias deben ser imágenes JPEG, PNG o WebP.');
        $contenido = file_get_contents($file['tmp_name']);
        if ($contenido === false) throw new RuntimeException('No se pudo leer una foto.');
        $archivos[$key] = ['mime' => $info['mime'], 'contenido' => $contenido];
    }
    if (count($archivos) > 8) rechazarReporte('Puedes adjuntar hasta 8 fotos.');
    foreach ($camposFotos as $campo) {
        if (!isset($archivos[$campo])) rechazarReporte('Adjunta una foto de la guía de cada plataforma.');
    }

    $conexion->beginTransaction();
    $consulta = $conexion->prepare('SELECT s.id_ruta, s.estado_ruta FROM salvoconductos_rutas s WHERE s.numero_registro = ? AND EXISTS (SELECT 1 FROM asignacion_personal_armas a WHERE a.id_ruta = s.id_ruta AND a.id_usuario = ?) FOR UPDATE');
    $consulta->execute([$numero, $usuario]);
    $ruta = $consulta->fetch(PDO::FETCH_ASSOC);
    if (!$ruta || !in_array($ruta['estado_ruta'], ['Activa', 'En Tránsito'], true)) {
        $conexion->rollBack();
        rechazarReporte('La ruta no está asignada o ya no acepta reportes. Actualiza tus rutas.');
    }
    $idRuta = $ruta['id_ruta'];
    $stmt = $conexion->prepare('INSERT INTO reportes_pesca (id_ruta, origen_camaronera, observaciones, tipo_reporte, fecha_hora_reporte) VALUES (?, ?, ?, ?, NOW())');
    $stmt->execute([$idRuta, $origen, $observaciones, $tipo]);
    $idReporte = $conexion->lastInsertId();

    $idsFotos = [];
    $stmtFoto = $conexion->prepare("INSERT INTO evidencias_reporte (id_reporte, ruta_archivo, tipo_archivo) VALUES (?, NULL, 'foto')");
    $stmtArchivo = $conexion->prepare('INSERT INTO evidencias_reporte_archivo (id_evidencia, tipo_mime, contenido) VALUES (?, ?, ?)');
    foreach ($archivos as $key => $archivo) {
        $stmtFoto->execute([$idReporte]);
        $idFoto = $conexion->lastInsertId();
        $stmtArchivo->bindValue(1, $idFoto, PDO::PARAM_INT);
        $stmtArchivo->bindValue(2, $archivo['mime']);
        $stmtArchivo->bindValue(3, $archivo['contenido'], PDO::PARAM_LOB);
        $stmtArchivo->execute();
        $idsFotos[$key] = $idFoto;
    }
    $stmtGuia = $conexion->prepare('INSERT INTO guias_plataformas (id_reporte, identificador_plataforma, ruta_foto_guia) VALUES (?, ?, ?)');
    $stmtPiscina = $conexion->prepare('INSERT INTO detalle_piscinas (id_guia, numero_piscina, libraje) VALUES (?, ?, ?)');
    foreach ($plataformas as $plat) {
        $fotoCampo = $plat['foto_campo'] ?? '';
        $rutaFoto = isset($idsFotos[$fotoCampo]) ? '../api/reportes_app.php?evidencia=' . $idsFotos[$fotoCampo] : null;
        $stmtGuia->execute([$idReporte, trim($plat['identificador']), $rutaFoto]);
        $idGuia = $conexion->lastInsertId();
        foreach ($plat['piscinas'] as $piscina) {
            $num = trim($piscina['numero'] ?? '');
            $libras = $piscina['libraje'] ?? '';
            if ($num !== '' || $libras !== '') $stmtPiscina->execute([$idGuia, $num, $libras === '' ? null : $libras]);
        }
    }
    if ($tipo === 'Arribo a Destino') {
        $conexion->prepare("UPDATE salvoconductos_rutas SET estado_ruta = 'En espera de confirmación' WHERE id_ruta = ?")->execute([$idRuta]);
    }
    $conexion->commit();
    echo json_encode(['status' => 'success', 'mensaje' => 'Reporte registrado correctamente.']);
} catch (Throwable $e) {
    if (isset($conexion) && $conexion->inTransaction()) $conexion->rollBack();
    error_log('Error al registrar reporte: ' . $e->getMessage());
    http_response_code(500);
    echo json_encode(['status' => 'error', 'mensaje' => 'No se pudo procesar el reporte.']);
}

package com.example.custodia_app.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun rememberCameraCapture(onPhoto: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnPhoto by rememberUpdatedState(onPhoto)
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingUri
        pendingUri = null
        if (success && uri != null) currentOnPhoto(uri)
        else if (uri != null) deleteCameraPhoto(context, uri)
    }
    return {
        try {
            val directory = File(context.cacheDir, "camera")
            if (!directory.exists() && !directory.mkdirs()) error("No se pudo crear la carpeta de fotos")
            val file = File.createTempFile("foto_", ".jpg", directory)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingUri = uri.toString()
            launcher.launch(uri)
        } catch (e: Exception) {
            pendingUri?.let { deleteCameraPhoto(context, it) }
            pendingUri = null
            Toast.makeText(context, "No se pudo abrir la cámara. Comprueba que haya una aplicación de cámara disponible.", Toast.LENGTH_LONG).show()
        }
    }
}

fun deleteCameraPhoto(context: Context, uri: String) {
    try { context.contentResolver.delete(Uri.parse(uri), null, null) } catch (_: Exception) { }
}

// Reducir las fotos limita el tamaño total del formulario y conserva más detalle que una miniatura.
fun photoParts(context: Context, fotos: List<Pair<String, String>>): List<MultipartBody.Part> = fotos.map { (campo, uriTexto) ->
    val uri = Uri.parse(uriTexto)
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds); Unit } ?: error("No se puede leer la foto")
    check(bounds.outWidth > 0 && bounds.outHeight > 0) { "La foto no es válida" }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    var bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: error("La foto no es válida")
    val orientation = resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } ?: ExifInterface.ORIENTATION_NORMAL
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
    }
    if (!matrix.isIdentity) {
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        bitmap = rotated
    }
    var bytes: ByteArray
    while (true) {
        var quality = 85
        do {
            val output = ByteArrayOutputStream()
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) { "No se pudo preparar la foto" }
            bytes = output.toByteArray()
            quality -= 10
        } while (bytes.size > 500 * 1024 && quality >= 35)
        if (bytes.size <= 500 * 1024) break
        check(bitmap.width > 1 && bitmap.height > 1) { "No se pudo reducir la foto" }
        val smaller = Bitmap.createScaledBitmap(bitmap, maxOf(1, bitmap.width * 3 / 4), maxOf(1, bitmap.height * 3 / 4), true)
        if (smaller !== bitmap) bitmap.recycle()
        bitmap = smaller
    }
    bitmap.recycle()
    MultipartBody.Part.createFormData(campo, "$campo.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
}

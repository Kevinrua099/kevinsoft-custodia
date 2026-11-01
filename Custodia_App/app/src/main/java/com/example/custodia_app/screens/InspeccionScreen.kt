package com.example.custodia_app.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.custodia_app.network.RetrofitClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspeccionScreen(navController: NavController) {
    var placa by rememberSaveable { mutableStateOf("") }
    var disco by rememberSaveable { mutableStateOf("") }
    var kilometraje by rememberSaveable { mutableStateOf("") }
    var observaciones by rememberSaveable { mutableStateOf("") }

    val listaFotos = remember { mutableStateListOf<Bitmap>() }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { listaFotos.add(it) }
    }

    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("CustodiaPrefs", 0)
    val nombre = prefs.getString("usuarioLogueado", "Desconocido") ?: "Desconocido"

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recepción de Patrulla") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)) }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            item {
                Text("Registro de recepción", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Ingresa los datos y adjunta fotos del estado de la patrulla.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
                OutlinedTextField(value = disco, onValueChange = { if (it.length <= 50) disco = it }, label = { Text("Disco *") }, placeholder = { Text("Ej: Patrulla 583") }, singleLine = true, enabled = !isLoading, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = placa, onValueChange = { if (it.length <= 20) placa = it }, label = { Text("Placa *") }, placeholder = { Text("Ej: GLT-2201") }, singleLine = true, enabled = !isLoading, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = kilometraje, onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) kilometraje = it }, label = { Text("Kilometraje (opcional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, enabled = !isLoading, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = observaciones, onValueChange = { observaciones = it }, label = { Text("Novedades del vehículo") }, placeholder = { Text("Rayones, llantas u otras novedades") }, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 4)

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { cameraLauncher.launch(null) }, enabled = !isLoading && listaFotos.size < 8, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("📸 AÑADIR FOTO (${listaFotos.size}/8)")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if(listaFotos.isNotEmpty()) {
                item {
                    Text("Fotos capturadas: ${listaFotos.size}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(listaFotos) { index, bmp ->
                            Column {
                                Card(modifier = Modifier.size(100.dp)) {
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "Foto ${index + 1} de la patrulla", modifier = Modifier.fillMaxSize())
                                }
                                TextButton(onClick = { listaFotos.removeAt(index) }, enabled = !isLoading) { Text("Quitar") }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                Button(
                    onClick = {
                        if(disco.isBlank() || placa.isBlank()) { Toast.makeText(context, "Indica el disco y la placa", Toast.LENGTH_SHORT).show(); return@Button }
                        if(kilometraje.isNotEmpty() && kilometraje.toIntOrNull() == null) { Toast.makeText(context, "Indica un kilometraje válido", Toast.LENGTH_SHORT).show(); return@Button }
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val userBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
                                val placaBody = placa.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                                val discoBody = disco.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                                val kmBody = kilometraje.toRequestBody("text/plain".toMediaTypeOrNull())
                                val obsBody = observaciones.toRequestBody("text/plain".toMediaTypeOrNull())

                                val partesFotos = mutableListOf<okhttp3.MultipartBody.Part>()
                                listaFotos.forEachIndexed { index, bmp ->
                                    val stream = ByteArrayOutputStream()
                                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                                    val reqFile = stream.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull())
                                    partesFotos.add(okhttp3.MultipartBody.Part.createFormData("foto_$index", "insp_$index.jpg", reqFile))
                                }

                                val response = RetrofitClient.instance.enviarInspeccion(userBody, placaBody, discoBody, kmBody, obsBody, partesFotos)
                                if(response.status == "success") {
                                    Toast.makeText(context, "✅ Vehículo Recibido", Toast.LENGTH_LONG).show()
                                    navController.popBackStack()
                                } else { Toast.makeText(context, "Error: ${response.mensaje}", Toast.LENGTH_LONG).show() }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo conectar con el servidor. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp), enabled = !isLoading
                ) { Text(if(isLoading) "PROCESANDO..." else "CONFIRMAR RECEPCIÓN DE PATRULLA", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

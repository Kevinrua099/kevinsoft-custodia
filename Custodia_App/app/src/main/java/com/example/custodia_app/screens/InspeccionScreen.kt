package com.example.custodia_app.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.custodia_app.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspeccionScreen(navController: NavController) {
    var placa by remember { mutableStateOf("") }
    var disco by remember { mutableStateOf("") }
    var kilometraje by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

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
                OutlinedTextField(value = disco, onValueChange = { disco = it }, label = { Text("Disco (Ej: Patrulla 583)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = placa, onValueChange = { placa = it }, label = { Text("Placas (Ej: GLT-2201)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = kilometraje, onValueChange = { kilometraje = it }, label = { Text("Kilometraje") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = observaciones, onValueChange = { observaciones = it }, label = { Text("Novedades del vehículo (Rayones, llantas...)") }, modifier = Modifier.fillMaxWidth().height(100.dp), maxLines = 4)

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("📸 AÑADIR FOTO DE ESTADO")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if(listaFotos.isNotEmpty()) {
                item {
                    Text("Fotos Capturadas: ${listaFotos.size}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listaFotos) { bmp ->
                            Card(modifier = Modifier.size(100.dp)) {
                                Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                Button(
                    onClick = {
                        if(disco.isEmpty() || placa.isEmpty()) { Toast.makeText(context, "Disco y Placa requeridos", Toast.LENGTH_SHORT).show(); return@Button }
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val userBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), nombre)
                                val placaBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), placa)
                                val discoBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), disco)
                                val kmBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), kilometraje)
                                val obsBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), observaciones)

                                val partesFotos = mutableListOf<okhttp3.MultipartBody.Part>()
                                listaFotos.forEachIndexed { index, bmp ->
                                    val stream = ByteArrayOutputStream()
                                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                                    val reqFile = okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), stream.toByteArray())
                                    partesFotos.add(okhttp3.MultipartBody.Part.createFormData("foto_$index", "insp_$index.jpg", reqFile))
                                }

                                val response = RetrofitClient.instance.enviarInspeccion(userBody, placaBody, discoBody, kmBody, obsBody, partesFotos)
                                isLoading = false
                                if(response.status == "success") {
                                    Toast.makeText(context, "✅ Vehículo Recibido", Toast.LENGTH_LONG).show()
                                    navController.popBackStack()
                                } else { Toast.makeText(context, "Error: ${response.mensaje}", Toast.LENGTH_LONG).show() }
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp), enabled = !isLoading
                ) { Text(if(isLoading) "PROCESANDO..." else "CONFIRMAR RECEPCIÓN DE PATRULLA", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
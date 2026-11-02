package com.example.custodia_app.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.custodia_app.network.LoginRequest
import com.example.custodia_app.network.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

@Composable
fun LoginScreen(navController: NavController) {
    var cedula by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "KevinSoft", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(text = "App Táctica para Custodios", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = cedula,
            onValueChange = { cedula = it },
            label = { Text("Cédula de Identidad") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if(cedula.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.instance.login(LoginRequest(cedula, password))

                            if(response.status == "success") {
                                Toast.makeText(context, "✅ ${response.mensaje}", Toast.LENGTH_SHORT).show()

                                // Guardamos el nombre para que las otras pantallas lo lean.
                                val nombreReal = response.usuario?.nombres ?: response.mensaje.removePrefix("Bienvenido ")
                                val prefs = context.getSharedPreferences("CustodiaPrefs", 0)
                                prefs.edit().putString("usuarioLogueado", nombreReal)
                                    .putInt("usuarioId", response.usuario?.id ?: 0).apply()

                                // 2. Navegamos al NUEVO MENÚ y borramos el login del historial
                                navController.navigate("menu") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "⚠️ ${response.mensaje}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: SocketTimeoutException) {
                            Toast.makeText(context, "El servidor tardó demasiado en responder. Inténtalo de nuevo en un momento.", Toast.LENGTH_LONG).show()
                        } catch (e: HttpException) {
                            Toast.makeText(context, "El servidor respondió con un error (${e.code()}). Inténtalo de nuevo más tarde.", Toast.LENGTH_LONG).show()
                        } catch (e: IOException) {
                            Toast.makeText(context, "No se pudo conectar con el servidor. Comprueba tu conexión a internet e inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo procesar la respuesta del servidor. Inténtalo de nuevo más tarde.", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    Toast.makeText(context, "Llene todos los campos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "VERIFICANDO..." else "INICIAR SESIÓN", fontWeight = FontWeight.Bold)
        }
    }
}

package com.example.custodia_app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("CustodiaPrefs", 0)
    val nombre = prefs.getString("usuarioLogueado", "Custodio")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Panel Táctico", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Operador: $nombre", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))

            Button(
                onClick = { navController.navigate("patrullas") },
                modifier = Modifier.fillMaxWidth().height(65.dp)
            ) { Text("1. CONTROL DE PATRULLAS") }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("reporte") },
                modifier = Modifier.fillMaxWidth().height(65.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) { Text("2. CUSTODIA EN RUTA") }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = {
                prefs.edit().clear().apply()
                navController.navigate("login") { popUpTo("menu") { inclusive = true } }
            }, modifier = Modifier.fillMaxWidth()) { Text("CERRAR SESIÓN") }
        }
    }
}

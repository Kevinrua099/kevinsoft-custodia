package com.example.custodia_app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Táctico", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3B5998)
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Operador: Kevin Rua",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color(0xFF2C3E50)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { navController.navigate("patrullas") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6595)),
                modifier = Modifier.fillMaxWidth(0.85f).height(60.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("1. CONTROL DE PATRULLAS", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("reporte") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D5A7A)),
                modifier = Modifier.fillMaxWidth(0.85f).height(60.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("2. CUSTODIA EN RUTA", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // NUEVO BOTÓN: Movimiento Interno
            Button(
                onClick = { navController.navigate("movimientos") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)), // Gris Pizarra Oscuro
                modifier = Modifier.fillMaxWidth(0.85f).height(60.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("3. MOVIMIENTO INTERNO", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    navController.navigate("login") { popUpTo(0) }
                },
                modifier = Modifier.fillMaxWidth(0.85f).height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("CERRAR SESIÓN", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}
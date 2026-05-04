package com.presencainteligente.iot.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * LocationSetupScreen: Tela para configurar localização da casa com pin no mapa visual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSetupScreen(
    onLocationSaved: (latitude: Double, longitude: Double) -> Unit,
    onNavigateBack: () -> Unit,
    currentLatitude: Double = -23.5505,
    currentLongitude: Double = -46.6333
) {
    var latitude by remember { mutableStateOf(currentLatitude.toString()) }
    var longitude by remember { mutableStateOf(currentLongitude.toString()) }
    var showMessage by remember { mutableStateOf("") }
    var messageType by remember { mutableStateOf("success") }

    val primaryBlue = Color(0xFF1976D2)
    val errorRed = Color(0xFFD32F2F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== HEADER =====
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Casa",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Configurar Casa")
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "Voltar")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = primaryBlue,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Visualizar No Mapa",
                style = MaterialTheme.typography.titleMedium,
                color = primaryBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LocationCanvas(
                latitude = latitude.toDoubleOrNull() ?: 0.0,
                longitude = longitude.toDoubleOrNull() ?: 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Coordenadas",
                style = MaterialTheme.typography.titleMedium,
                color = primaryBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = latitude,
                onValueChange = { latitude = it },
                label = { Text("Latitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("ex: -23.5505") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = longitude,
                onValueChange = { longitude = it },
                label = { Text("Longitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("ex: -46.6333") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    showMessage = "GPS não implementado neste protótipo"
                    messageType = "error"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90CAF9))
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Usar Localização Atual (GPS)")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()

                    if (lat == null || lng == null || lat == 0.0 || lng == 0.0) {
                        showMessage = "Coordenadas inválidas!"
                        messageType = "error"
                    } else if (lat < -90 || lat > 90) {
                        showMessage = "Latitude deve estar entre -90 e 90"
                        messageType = "error"
                    } else if (lng < -180 || lng > 180) {
                        showMessage = "Longitude deve estar entre -180 e 180"
                        messageType = "error"
                    } else {
                        onLocationSaved(lat, lng)
                        showMessage = "✓ Localização salva com sucesso!"
                        messageType = "success"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text(
                    "SALVAR LOCALIZAÇÃO",
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
            }

            if (showMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (messageType == "success") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            showMessage,
                            color = if (messageType == "success") Color(0xFF2E7D32) else errorRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "💡 Dica",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryBlue
                    )
                    Text(
                        "Use coordenadas reais (ex: -23.5505, -46.6333) para criar uma geofence precisa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LocationCanvas(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val gridColor = Color(0xFFDDDDDD)
        val gridLineCount = 4

        for (i in 0..gridLineCount) {
            val x = (canvasWidth / gridLineCount) * i
            val y = (canvasHeight / gridLineCount) * i
            drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, canvasHeight), strokeWidth = 1f)
            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1f)
        }

        val axisColor = Color(0xFF1976D2)
        drawLine(color = axisColor, start = Offset(canvasWidth / 2, 0f), end = Offset(canvasWidth / 2, canvasHeight), strokeWidth = 2f)
        drawLine(color = axisColor, start = Offset(0f, canvasHeight / 2), end = Offset(canvasWidth, canvasHeight / 2), strokeWidth = 2f)

        val normalizedX = ((longitude + 180) / 360) * canvasWidth
        val normalizedY = ((90 - latitude) / 180) * canvasHeight
        val pinRadius = 12f
        val pinColor = Color(0xFFD32F2F)

        drawCircle(color = pinColor, radius = pinRadius, center = Offset(normalizedX.toFloat(), normalizedY.toFloat()))
        drawCircle(
            color = pinColor.copy(alpha = 0.3f),
            radius = pinRadius * 1.5f,
            center = Offset(normalizedX.toFloat(), normalizedY.toFloat()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun LocationSetupScreenPreview() {
    LocationSetupScreen(
        onLocationSaved = { _, _ -> },
        onNavigateBack = {},
        currentLatitude = -23.5505,
        currentLongitude = -46.6333
    )
}

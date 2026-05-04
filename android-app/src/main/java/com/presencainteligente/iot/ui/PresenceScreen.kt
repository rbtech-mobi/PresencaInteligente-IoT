package com.presencainteligente.iot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale

@Composable
fun PresenceScreen(
    viewModel: PresenceViewModel,
    onNavigateToLocationSetup: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val homeLocation by viewModel.homeLocationFlow.collectAsState(initial = null)
    val primaryBlue = Color(0xFF1976D2)
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(homeLocation?.isActive) {
        if (homeLocation?.isActive == true) {
            snackbarHostState.showSnackbar(
                "✓ Localização da casa atualizada com sucesso!",
                duration = SnackbarDuration.Short
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(PresenceAction.OnConnectClick)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Presença Inteligente",
                style = MaterialTheme.typography.headlineMedium,
                color = primaryBlue,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            ConnectionStatusHeader(state.mqttConnected)

            Spacer(modifier = Modifier.height(16.dp))

            PresenceIndicator(state.isHome)

            Spacer(modifier = Modifier.height(32.dp))

            AcControlPanel(
                state = state,
                onTempChange = { viewModel.onAction(PresenceAction.OnTargetTempChange(it)) },
                onToggle = { viewModel.onAction(PresenceAction.OnToggleAcClick) },
                minTemp = viewModel.minTemp,
                maxTemp = viewModel.maxTemp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.homeLocationSet) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Casa",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    if (homeLocation?.isActive == true && homeLocation?.latitude != 0.0) {
                        Text(
                            "Lat: ${String.format(Locale.ROOT, "%.4f", homeLocation?.latitude ?: 0.0)}°",
                            style = MaterialTheme.typography.bodyMedium,
                            color = primaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Lng: ${String.format(Locale.ROOT, "%.4f", homeLocation?.longitude ?: 0.0)}°",
                            style = MaterialTheme.typography.bodyMedium,
                            color = primaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            "Localização não definida",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF57C00),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToLocationSetup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6))
            ) {
                Text("⚙️ Configurar Casa", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = { viewModel.onAction(PresenceAction.OnSimulatePresence(!state.isHome)) }) {
                Text(
                    text = if (state.isHome) "Simular: Sair de Casa" else "Simular: Chegar em Casa",
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusHeader(isConnected: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF57C00),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isConnected) "✓ Conectado ao Broker" else "✕ Desconectado",
                style = MaterialTheme.typography.labelMedium,
                color = if (isConnected) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PresenceIndicator(isHome: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isHome) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isHome) Icons.Default.Home else Icons.Default.LocationOff,
                contentDescription = null,
                tint = if (isHome) Color(0xFF4CAF50) else Color(0xFFF57C00)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isHome) "Modo: Em Casa (Automação Ativa)" else "Modo: Fora (Controle Remoto)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AcControlPanel(
    state: PresenceUiState,
    onTempChange: (Int) -> Unit,
    onToggle: () -> Unit,
    minTemp: Int,
    maxTemp: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "CONTROLE DO AR-CONDICIONADO",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Ambiente",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    if (state.currentTemp > 0) {
                        Text(
                            "${state.currentTemp}°C",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "---",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.LightGray
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Alvo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        "${state.targetTemp}°C",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = state.targetTemp.toFloat(),
                onValueChange = { onTempChange(it.toInt()) },
                valueRange = minTemp.toFloat()..maxTemp.toFloat(),
                steps = maxTemp - minTemp - 1,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF1976D2),
                    activeTrackColor = Color(0xFF1976D2)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isAcOn) Color(0xFFEF5350) else Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = if (state.isAcOn) "DESLIGAR AR" else "LIGAR AR",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.lastCommandStatus.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (state.lastCommandStatus.contains("sucesso") || state.lastCommandStatus.contains("enviado"))
                        Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = state.lastCommandStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.lastCommandStatus.contains("sucesso") || state.lastCommandStatus.contains("enviado"))
                            Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (state.statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Status de Conexão: ${state.statusMessage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.mqttConnected) Color(0xFF4CAF50) else Color(0xFFF57C00),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

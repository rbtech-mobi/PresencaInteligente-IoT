package com.presencainteligente.iot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.presencainteligente.iot.data.MqttManager

/**
 * RemoteControlScreen: Componente de UI para teste de controle remoto direto via broker público.
 * REQUISITO: Botão que alterna entre LIGADO/DESLIGADO e muda de cor.
 */
@Composable
fun RemoteControlScreen(mqttManager: MqttManager) {
    var isEnabled by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Teste de Controle Remoto (ESP32)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    isEnabled = !isEnabled
                    mqttManager.sendCommand(isEnabled)
                },
                modifier = Modifier.size(160.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFE53935)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = if (isEnabled) "LIGADO" else "DESLIGADO",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Tópico: meu/iot/comando",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            
            if (isEnabled) {
                Text(
                    text = "Sinal ON enviado",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

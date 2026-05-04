package com.presencainteligente.iot.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.annotation.SuppressLint
import com.presencainteligente.iot.data.MqttRepository
import kotlinx.coroutines.*

class PresenceService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mqttRepository = MqttRepository.getInstance()
    private val channelId = "presence_service_channel"

    private var automationStarted = false
    private var monitoringJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d("PresenceService", "Serviço criado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("PresenceService", "onStartCommand: action=$action")

        when (action) {
            "ACTION_START_PRESENCE" -> {
                startForeground(1, createNotification(
                    "Modo Presença Ativo",
                    "Sincronizando com sua casa..."
                ))

                if (monitoringJob == null || monitoringJob?.isCompleted == true) {
                    monitoringJob = serviceScope.launch {
                        startAutomationMonitoring()
                    }
                } else {
                    Log.d("PresenceService", "Monitoramento já está ativo, ignorando novo START")
                }
            }

            "ACTION_STOP_PRESENCE" -> {
                Log.d("PresenceService", "Encerrando automação: desligando AC")
                serviceScope.launch {
                    monitoringJob?.cancel()
                    monitoringJob = null
                    automationStarted = false

                    mqttRepository.publishCommand("off")
                    Log.d("PresenceService", "AC desligado")

                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    Log.d("PresenceService", "Serviço parado")
                }
            }
        }

        return START_STICKY
    }

    private suspend fun startAutomationMonitoring() {
        Log.d("PresenceService", "Iniciando monitoramento de automação")

        try {
            mqttRepository.connect()
            Log.d("PresenceService", "Conectado ao broker")

            mqttRepository.deviceState.collect { state ->
                Log.d("PresenceService", "Estado recebido: power=${state.power}, temp=${state.temperature}")

                if (!automationStarted && !state.power) {
                    automationStarted = true
                    val tempToSet = if (state.temperature > 0) state.temperature else 22
                    Log.d("PresenceService", "AC estava OFF, ligando com temp=$tempToSet")
                    mqttRepository.publishCommand("on", tempToSet)
                    updateNotification("AC Ligado", "Temperatura: ${tempToSet}°C")
                } else if (state.power) {
                    automationStarted = true
                    Log.d("PresenceService", "AC já está ligado, automação concluída")
                    updateNotification("AC Ligado", "Automação de entrada concluída")
                }

                if (automationStarted && state.power) {
                    Log.d("PresenceService", "Automação concluída (AC ligado), mantendo monitoramento")
                }
            }
        } catch (e: CancellationException) {
            Log.d("PresenceService", "Monitoramento cancelado")
            throw e
        } catch (e: Exception) {
            Log.e("PresenceService", "Erro no monitoramento: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d("PresenceService", "onDestroy: cancelando corrotinas")
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            channelId,
            "Serviço de Presença Inteligente",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("NotificationPermission")
    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val manager = getSystemService(NotificationManager::class.java)

        // Verificar permissão de notificação (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                manager.notify(1, notification)
            } else {
                Log.w("PresenceService", "Permissão POST_NOTIFICATIONS não concedida")
            }
        } else {
            manager.notify(1, notification)
        }
    }
}

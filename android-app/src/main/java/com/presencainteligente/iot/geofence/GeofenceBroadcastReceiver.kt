package com.presencainteligente.iot.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.presencainteligente.iot.service.PresenceService

/**
 * GeofenceBroadcastReceiver: Orquestrador de transições de localização.
 *
 * Comportamento:
 * - ENTER: Dispara ACTION_START_PRESENCE para iniciar automação
 * - EXIT: Dispara ACTION_STOP_PRESENCE para desligar AC
 * - Erros: Logado e ignorado (não quebra a aplicação)
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("GeofenceReceiver", "onReceive: intent=$intent")

        val event = GeofencingEvent.fromIntent(intent)
        if (event == null) {
            Log.e("GeofenceReceiver", "GeofencingEvent é null, intent inválida")
            return
        }

        if (event.hasError()) {
            Log.e("GeofenceReceiver", "Erro no evento de Geofence: ${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition
        val triggeringGeofences = event.triggeringGeofences
        Log.d("GeofenceReceiver", "Transição: $transition | Geofences: $triggeringGeofences")

        val serviceIntent = Intent(context, PresenceService::class.java)

        try {
            when (transition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d("GeofenceReceiver", "✓ ENTROU na geofence de casa")
                    serviceIntent.action = "ACTION_START_PRESENCE"
                    context.startForegroundService(serviceIntent)
                }

                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d("GeofenceReceiver", "✗ SAIU da geofence de casa")
                    serviceIntent.action = "ACTION_STOP_PRESENCE"
                    context.startForegroundService(serviceIntent)
                }

                else -> {
                    Log.w("GeofenceReceiver", "Transição desconhecida: $transition")
                }
            }
        } catch (e: Exception) {
            Log.e("GeofenceReceiver", "Erro ao iniciar PresenceService: ${e.message}", e)
        }
    }
}

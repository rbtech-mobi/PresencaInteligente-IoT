package com.presencainteligente.iot.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * GeofenceManager: Gerencia o ciclo de vida da cerca virtual.
 * Raio: 1000 metros (1km) conforme especificação.
 *
 * Comportamento:
 * - setHomeLocation(): remove cercas antigas antes de adicionar nova
 * - updateHomeLocation(): atualiza a localização (remove + adiciona)
 * - removeHomeLocation(): remove completamente
 */
class GeofenceManager(private val context: Context) {
    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun setHomeLocation(latitude: Double, longitude: Double) {
        Log.d("GeofenceManager", "Definindo localização da casa: lat=$latitude, lng=$longitude")

        try {
            removeGeofence()
        } catch (e: Exception) {
            Log.w("GeofenceManager", "Nenhuma geofence anterior para remover: ${e.message}")
        }

        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setLoiteringDelay(LOITERING_DELAY_MS)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener {
                currentLatitude = latitude
                currentLongitude = longitude
                Log.d("GeofenceManager", "Geofence adicionada com sucesso")
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Erro ao adicionar geofence: ${e.message}", e)
            }
    }

    @SuppressLint("MissingPermission")
    fun updateHomeLocation(latitude: Double, longitude: Double) {
        Log.d("GeofenceManager", "Atualizando localização: lat=$latitude, lng=$longitude")
        removeGeofence()
        setHomeLocation(latitude, longitude)
    }

    fun removeGeofence() {
        Log.d("GeofenceManager", "Removendo geofence...")
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener {
                    currentLatitude = null
                    currentLongitude = null
                    Log.d("GeofenceManager", "Geofence removida com sucesso")
                }
                .addOnFailureListener { e ->
                    Log.e("GeofenceManager", "Erro ao remover geofence: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e("GeofenceManager", "Erro ao chamar removeGeofences: ${e.message}", e)
        }
    }

    fun getHomeLocation(): Pair<Double, Double>? {
        return if (currentLatitude != null && currentLongitude != null) {
            Pair(currentLatitude!!, currentLongitude!!)
        } else {
            null
        }
    }

    companion object {
        private const val GEOFENCE_ID = "HOME_GEOFENCE"
        private const val GEOFENCE_REQUEST_CODE = 100
        private const val GEOFENCE_RADIUS_METERS = 1000f
        private const val LOITERING_DELAY_MS = 5 * 60 * 1000
    }
}

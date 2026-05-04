package com.presencainteligente.iot.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val name: String = "Casa",
    val isActive: Boolean = false,
    val lastUpdate: Long = 0
)

class LocationRepository(private val context: Context) {

    companion object {
        @Volatile
        private var instance: LocationRepository? = null

        fun getInstance(context: Context): LocationRepository {
            return instance ?: synchronized(this) {
                instance ?: LocationRepository(context).also { instance = it }
            }
        }

        private const val DATASTORE_NAME = "home_location"
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(DATASTORE_NAME)
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val HOME_LATITUDE = doublePreferencesKey("home_latitude")
        val HOME_LONGITUDE = doublePreferencesKey("home_longitude")
    }

    val homeLocation: Flow<LocationData> = dataStore.data.map { preferences ->
        val lat = preferences[PreferencesKeys.HOME_LATITUDE] ?: 0.0
        val lng = preferences[PreferencesKeys.HOME_LONGITUDE] ?: 0.0

        if (lat == 0.0 && lng == 0.0) {
            Log.d("LocationRepository", "Nenhuma localização salva")
            LocationData()
        } else {
            Log.d("LocationRepository", "Localização carregada: lat=$lat, lng=$lng")
            LocationData(
                latitude = lat,
                longitude = lng,
                isActive = true,
                lastUpdate = System.currentTimeMillis()
            )
        }
    }

    suspend fun saveHomeLocation(latitude: Double, longitude: Double) {
        try {
            if (latitude == 0.0 || longitude == 0.0) {
                Log.w("LocationRepository", "Coordenadas inválidas: lat=$latitude, lng=$longitude")
                return
            }

            Log.d("LocationRepository", "Salvando localização: lat=$latitude, lng=$longitude")

            dataStore.edit { preferences ->
                preferences[PreferencesKeys.HOME_LATITUDE] = latitude
                preferences[PreferencesKeys.HOME_LONGITUDE] = longitude
            }

            Log.d("LocationRepository", "Localização salva com sucesso")
        } catch (e: Exception) {
            Log.e("LocationRepository", "Erro ao salvar localização: ${e.message}", e)
        }
    }

    suspend fun updateHomeLocation(latitude: Double, longitude: Double) {
        Log.d("LocationRepository", "Atualizando localização: lat=$latitude, lng=$longitude")
        clearHomeLocation()
        saveHomeLocation(latitude, longitude)
    }

    suspend fun clearHomeLocation() {
        try {
            Log.d("LocationRepository", "Limpando localização")
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.HOME_LATITUDE] = 0.0
                preferences[PreferencesKeys.HOME_LONGITUDE] = 0.0
            }
            Log.d("LocationRepository", "Localização limpa")
        } catch (e: Exception) {
            Log.e("LocationRepository", "Erro ao limpar localização: ${e.message}", e)
        }
    }

    suspend fun hasHomeLocation(): Boolean {
        try {
            val preferences = dataStore.data.map { prefs ->
                val lat = prefs[PreferencesKeys.HOME_LATITUDE] ?: 0.0
                val lng = prefs[PreferencesKeys.HOME_LONGITUDE] ?: 0.0
                lat != 0.0 && lng != 0.0
            }
            return preferences.map { it }.hashCode() != 0
        } catch (e: Exception) {
            Log.e("LocationRepository", "Erro ao verificar localização: ${e.message}")
            return false
        }
    }
}

package com.presencainteligente.iot.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.presencainteligente.iot.data.LocationRepository
import com.presencainteligente.iot.data.MqttRepository
import com.presencainteligente.iot.geofence.GeofenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PresenceUiState(
    val isHome: Boolean = false,
    val mqttConnected: Boolean = false,
    val isAcOn: Boolean = false,
    val targetTemp: Int = 22,
    val currentTemp: Int = 0,
    val homeLocationSet: Boolean = false,
    val statusMessage: String = "Desconectado",
    val lastCommandStatus: String = ""
)

sealed interface PresenceAction {
    data object OnConnectClick : PresenceAction
    data object OnToggleAcClick : PresenceAction
    data class OnTargetTempChange(val newTemp: Int) : PresenceAction
    data class OnSetHomeLocation(val latitude: Double, val longitude: Double) : PresenceAction
    data object OnRemoveHomeLocation : PresenceAction
    data class OnSimulatePresence(val present: Boolean) : PresenceAction
}

class PresenceViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PresenceUiState())
    val uiState = _uiState.asStateFlow()

    private val mqttRepository = MqttRepository.getInstance()
    private val geofenceManager = GeofenceManager(application)
    private val locationRepository = LocationRepository.getInstance(application)

    val homeLocationFlow = locationRepository.homeLocation
    val minTemp = 19
    val maxTemp = 30

    init {
        observeDeviceState()
        observeHomeLocation()
        connect()
    }

    fun onAction(action: PresenceAction) {
        when (action) {
            PresenceAction.OnConnectClick -> connect()
            PresenceAction.OnToggleAcClick -> toggleAcPower()
            is PresenceAction.OnTargetTempChange -> updateTargetTemp(action.newTemp)
            is PresenceAction.OnSetHomeLocation -> setHomeLocation(action.latitude, action.longitude)
            PresenceAction.OnRemoveHomeLocation -> removeHomeLocation()
            is PresenceAction.OnSimulatePresence -> simulatePresence(action.present)
        }
    }

    private fun observeDeviceState() {
        viewModelScope.launch {
            mqttRepository.deviceState.collect { deviceState ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isAcOn = deviceState.power,
                        currentTemp = deviceState.temperature,
                        targetTemp = if (deviceState.temperature > 0) deviceState.temperature else currentState.targetTemp,
                        mqttConnected = deviceState.isConnected,
                        statusMessage = if (deviceState.isConnected) {
                            if (deviceState.power) "AC Ligado" else "AC Desligado"
                        } else {
                            "Desconectado"
                        }
                    )
                }
            }
        }
    }

    private fun observeHomeLocation() {
        viewModelScope.launch {
            homeLocationFlow.collect { location ->
                _uiState.update { it.copy(homeLocationSet = location.isActive) }
            }
        }
    }

    private fun connect() {
        viewModelScope.launch {
            val success = mqttRepository.connect()
            _uiState.update { it.copy(statusMessage = if (success) "Conectado" else "Erro de conexão") }
        }
    }

    fun setHomeLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            locationRepository.saveHomeLocation(latitude, longitude)
            geofenceManager.setHomeLocation(latitude, longitude)
            _uiState.update { it.copy(homeLocationSet = true) }
        }
    }

    fun updateHomeLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            locationRepository.updateHomeLocation(latitude, longitude)
            geofenceManager.updateHomeLocation(latitude, longitude)
            _uiState.update { it.copy(homeLocationSet = true) }
        }
    }

    fun removeHomeLocation() {
        viewModelScope.launch {
            locationRepository.clearHomeLocation()
            geofenceManager.removeGeofence()
            _uiState.update { it.copy(homeLocationSet = false) }
        }
    }

    private fun updateTargetTemp(newTemp: Int) {
        if (newTemp !in minTemp..maxTemp) return

        _uiState.update { it.copy(targetTemp = newTemp) }

        if (_uiState.value.isAcOn) {
            sendCommand("on", newTemp)
        }
    }

    private fun toggleAcPower() {
        val currentIsOn = _uiState.value.isAcOn
        val action = if (currentIsOn) "off" else "on"

        _uiState.update { it.copy(lastCommandStatus = "Enviando comando: ${action.uppercase()}...") }
        sendCommand(action, _uiState.value.targetTemp)
    }

    private fun sendCommand(action: String, temp: Int) {
        viewModelScope.launch {
            try {
                val success = mqttRepository.publishCommand(action, temp)
                _uiState.update { it.copy(
                    lastCommandStatus = if (success) "Comando enviado" else "Erro ao enviar comando"
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(lastCommandStatus = "Erro: ${e.message}") }
            }
        }
    }

    private fun simulatePresence(present: Boolean) {
        _uiState.update { it.copy(isHome = present) }
    }
}

package com.presencainteligente.iot.data

import android.util.Log
import com.presencainteligente.iot.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import javax.net.ssl.SSLSocketFactory

data class DeviceState(
    val power: Boolean = false,
    val temperature: Int = 0,
    val isConnected: Boolean = false,
    val lastUpdate: Long = 0
)

class MqttRepository private constructor(
    private val host: String = BuildConfig.MQTT_HOST,
    val clientId: String = "Android_App_${UUID.randomUUID().toString().take(5)}"
) {
    private var client: MqttClient? = null
    private val gson = Gson()

    private val _messages = MutableSharedFlow<Pair<String, String>>(replay = 1, extraBufferCapacity = 64)
    val messages = _messages.asSharedFlow()

    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState = _deviceState.asStateFlow()

    companion object {
        @Volatile
        private var instance: MqttRepository? = null

        fun getInstance(): MqttRepository {
            return instance ?: synchronized(this) {
                instance ?: MqttRepository().also { instance = it }
            }
        }
    }

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (client?.isConnected == true) {
                Log.d("MqttRepository", "Já conectado ao broker")
                return@withContext true
            }

            if (client == null) {
                client = MqttClient(host, clientId, MemoryPersistence())
                Log.d("MqttRepository", "Novo cliente MQTT criado: $clientId")
            }

            val options = MqttConnectOptions().apply {
                userName = BuildConfig.MQTT_USER
                password = BuildConfig.MQTT_PASS.toCharArray()
                socketFactory = SSLSocketFactory.getDefault()
                isCleanSession = true
                connectionTimeout = 15
                keepAliveInterval = 60
                isAutomaticReconnect = true
            }

            client?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d("MqttRepository", "Conectado ao broker (reconnect=$reconnect)")
                    try {
                        client?.subscribe("home/ac/status", 1)
                        Log.d("MqttRepository", "Subscrito em 'home/ac/status' com sucesso")
                    } catch (e: Exception) {
                        Log.e("MqttRepository", "Erro ao subscrever: ${e.message}")
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.w("MqttRepository", "Conexão perdida: ${cause?.message}")
                    _deviceState.update { it.copy(isConnected = false) }
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    val payload = String(message.payload)
                    Log.d("MqttRepository", "Mensagem recebida: topic=$topic, payload=$payload")
                    _messages.tryEmit(topic to payload)

                    if (topic == "home/ac/status") {
                        updateInternalState(payload)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MqttRepository", "Entrega concluída do messageId=${token?.messageId}")
                }
            })

            Log.d("MqttRepository", "Conectando ao broker: $host")
            client?.connect(options)
            _deviceState.update { it.copy(isConnected = true) }
            Log.d("MqttRepository", "Conectado com sucesso")
            true
        } catch (e: Exception) {
            Log.e("MqttRepository", "Erro ao conectar: ${e.message}", e)
            _deviceState.update { it.copy(isConnected = false) }
            false
        }
    }

    private fun updateInternalState(json: String) {
        try {
            val map = gson.fromJson(json, Map::class.java) ?: return

            val oldState = _deviceState.value

            val power = when (val p = map["power"]) {
                is Boolean -> p
                "on", "ON" -> true
                "off", "OFF" -> false
                is Double -> p.toInt() != 0
                is Int -> p != 0
                else -> oldState.power
            }

            val temp = (map["temp"] as? Number)?.toInt()
                ?: (map["temperature"] as? Number)?.toInt()
                ?: (map["target_temperature"] as? Number)?.toInt()
                ?: oldState.temperature

            val newState = oldState.copy(
                power = power,
                temperature = temp,
                lastUpdate = System.currentTimeMillis()
            )

            if (newState != oldState) {
                Log.d("MqttRepository", "Estado atualizado: power=$power, temp=$temp")
                _deviceState.update { newState }
            }
        } catch (e: Exception) {
            Log.e("MqttRepository", "Erro ao fazer parse de estado: ${e.message}")
        }
    }

    suspend fun publishCommand(action: String, targetTemperature: Int? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            if (client?.isConnected != true) {
                Log.w("MqttRepository", "Não conectado, tentando conectar antes de publicar")
                if (!connect()) {
                    Log.e("MqttRepository", "Falha ao conectar antes de publicar comando")
                    return@withContext false
                }
            }

            val payload = mutableMapOf<String, Any>("action" to action)
            targetTemperature?.let { payload["temp"] = it }

            val json = gson.toJson(payload)
            val message = MqttMessage(json.toByteArray()).apply {
                qos = 1
            }

            Log.d("MqttRepository", "Publicando comando: $json")
            client?.publish("home/ac/command", message)
            Log.d("MqttRepository", "Comando publicado com sucesso")
            true
        } catch (e: Exception) {
            Log.e("MqttRepository", "Erro ao publicar comando: ${e.message}", e)
            false
        }
    }

    fun isConnected() = client?.isConnected ?: false
    
    fun disconnect() {
        try {
            Log.d("MqttRepository", "Desconectando do broker")
            client?.disconnect()
            _deviceState.update { it.copy(isConnected = false) }
            Log.d("MqttRepository", "Desconectado com sucesso")
        } catch (e: Exception) {
            Log.e("MqttRepository", "Erro ao desconectar: ${e.message}", e)
        }
    }
}

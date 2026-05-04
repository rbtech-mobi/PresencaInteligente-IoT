package com.presencainteligente.iot.data

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

/**
 * MqttManager: Classe utilitária para teste rápido com broker público.
 * Utiliza o broker.hivemq.com na porta 1883 (sem SSL para teste simples).
 */
class MqttManager(
    private val brokerUrl: String = "tcp://broker.hivemq.com:1883",
    private val clientId: String = "AndroidTest_${UUID.randomUUID().toString().take(4)}"
) {
    private var client: MqttClient? = null
    private val commandTopic = "meu/iot/comando"

    fun connect(onConnected: () -> Unit) {
        try {
            if (client?.isConnected == true) {
                onConnected()
                return
            }
            
            client = MqttClient(brokerUrl, clientId, MemoryPersistence())
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 60
            }

            client?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.d("MqttManager", "Conexão perdida: ${cause?.message}")
                }
                override fun messageArrived(topic: String?, message: MqttMessage?) {}
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            client?.connect(options)
            onConnected()
            Log.d("MqttManager", "Conectado ao broker público: $brokerUrl")
        } catch (e: Exception) {
            Log.e("MqttManager", "Erro ao conectar: ${e.message}")
        }
    }

    fun sendCommand(isOn: Boolean) {
        try {
            if (client?.isConnected != true) {
                connect { sendCommand(isOn) }
                return
            }

            val payload = if (isOn) "ON" else "OFF"
            val message = MqttMessage(payload.toByteArray()).apply {
                qos = 1
            }
            client?.publish(commandTopic, message)
            Log.d("MqttManager", "Comando enviado para $commandTopic: $payload")
        } catch (e: Exception) {
            Log.e("MqttManager", "Erro ao enviar comando: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
        } catch (e: Exception) {
            Log.e("MqttManager", "Erro ao desconectar: ${e.message}")
        }
    }
}

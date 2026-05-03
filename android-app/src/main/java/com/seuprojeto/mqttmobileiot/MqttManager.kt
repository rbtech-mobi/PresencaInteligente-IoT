package com.seuprojeto.mqttmobileiot

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.util.UUID

class MqttManager(
    private val onMessageReceived: (String, String) -> Unit,
    private val onStatusChanged: (String) -> Unit
) {
    private var client: Mqtt5AsyncClient? = null
    private val brokerHost = "broker.hivemq.com" // Public broker for testing

    fun connect() {
        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost(brokerHost)
            .buildAsync()

        client?.connect()?.whenComplete { _, throwable ->
            if (throwable != null) {
                onStatusChanged("Connection failed: ${throwable.message}")
            } else {
                onStatusChanged("Connected to $brokerHost")
                subscribe("iot/presence/#")
            }
        }
    }

    private fun subscribe(topic: String) {
        client?.subscribeWith()
            .topicFilter(topic)
            .callback { publish ->
                val message = String(publish.payloadAsBytes)
                onMessageReceived(publish.topic.toString(), message)
            }
            .send()
    }

    fun disconnect() {
        client?.disconnect()
    }
}

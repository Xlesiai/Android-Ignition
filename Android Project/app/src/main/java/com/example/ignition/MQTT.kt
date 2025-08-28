package com.example.ignition

import android.util.Log
import org.eclipse.paho.client.mqttv3.*


class SimpleMqttHelper(
    private val brokerUri: String
) {
    private val clientId = MqttClient.generateClientId()
    private var client: MqttClient = MqttClient(brokerUri, clientId, null)

    init {
            Log.d("MQTT", "MQTT_BROKER_URI: $brokerUri")
            connect()
    }
    fun connect() {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
        }

        Log.d("MQTT", "Connecting to $brokerUri")
        client.connect(options)
        Log.d("MQTT", "Connected to $brokerUri")
    }

    fun publish(topic: String, message: String) {
        if (client.isConnected) {
            client.publish(topic, MqttMessage(message.toByteArray()))
            Log.d("MQTT", "Published to $topic: $message")
        }
    }

    fun subscribe(topic: String) {
        client.subscribe(topic) { _, msg ->
            Log.d("MQTT", "Received from $topic: ${String(msg.payload)}")
        }
    }
}


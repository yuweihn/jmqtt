package org.jmqtt.java;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


public class Producer {
    private static final String broker = "tcp://localhost:1883";
    private static final String topic = "zztopic01";
    private static final String clientId = "2@zztest02";
    private static final String userName = "userb";
    private static final String password = "zzpassword";
    private static final String content = "Message from MqttProducer000哈哈哈2";
    private static final int qos = 1;

    public static void main(String[] args) throws MqttException, InterruptedException {
        MqttClient pubClient = getMqttClient();
        for (int i = 0; i < 3; i++) {
            MqttMessage mqttMessage = getMqttMessage();
            pubClient.publish(topic, mqttMessage);
            System.out.println("Send message success.");
        }
        pubClient.disconnect();
    }

    private static MqttMessage getMqttMessage() {
        MqttMessage mqttMessage = new MqttMessage(content.getBytes());
        mqttMessage.setQos(qos);
        return mqttMessage;
    }

    private static MqttClient getMqttClient() {
        try {
            MqttClient pubClient = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setWill("lwt", "this is a will message".getBytes(), 1, false);
            connectOptions.setCleanSession(false);
            connectOptions.setUserName(userName);
            connectOptions.setPassword(password.toCharArray());
            System.out.println("Connecting to broker: " + broker);
            pubClient.connect(connectOptions);
            System.out.println("Connected to broker: " + broker);
            return pubClient;
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return null;
    }
}

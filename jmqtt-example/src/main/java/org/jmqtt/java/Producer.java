package org.jmqtt.java;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


public class Producer {
    private static final String broker = "tcp://broker-test.agilenaas.net:1883";
    private static final String topic = "zztopic01";
    private static final String clientId = "2@zztest02";
    private static final String userName = "userb";
    private static final String password = "zzpassword";
    private static final String content = "Message from MqttProducer000哈哈哈2";
    private static final int qos = 1;

    public static void main(String[] args) throws MqttException {
        MqttClient client = getMqttClient();
        MqttMessage mqttMessage = getMqttMessage(content);
        client.publish(topic, mqttMessage);
        System.out.println("Send message success.");
        client.disconnect();
    }

    private static MqttMessage getMqttMessage(String content) {
        MqttMessage mqttMessage = new MqttMessage(content.getBytes());
        mqttMessage.setQos(qos);
        return mqttMessage;
    }

    private static MqttClient getMqttClient() throws MqttException {
        MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setUserName(userName);
        opts.setPassword(password.toCharArray());
        opts.setCleanSession(false);
        System.out.println("Connecting to broker: " + broker + ", userName: " + userName);
        client.connect(opts);
        System.out.println("Connected to broker: " + broker + ", userName: " + userName);
        return client;
    }
}

package org.jmqtt.java;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Consumer {
    private static final String broker = "tcp://broker-test.agilenaas.net:1883";
    private static final String topic = "zztopic01";
    private static final String clientId = "1@zztest02";

    public static void main(String[] args) throws MqttException {
        MqttClient subClient = getMqttClient();
        subClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                System.out.println("Connect lost,do some thing to solve it");
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) {
                System.out.println("From topic: " + s);
                System.out.println("Message content: " + new String(mqttMessage.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                System.out.println("deliveryComplete");
            }

        });
        subClient.subscribe(topic,2);
    }


    private static MqttClient getMqttClient() {
        try {
            MqttClient pubClient = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(false);
            connectOptions.setUserName("1@zztest02");
            connectOptions.setPassword("zzpassword".toCharArray());
            System.out.println("Connecting to broker: " + broker);
            pubClient.connect(connectOptions);
            return pubClient;
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return null;
    }
}

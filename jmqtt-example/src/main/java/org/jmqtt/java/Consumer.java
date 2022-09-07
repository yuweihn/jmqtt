package org.jmqtt.java;


import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


public class Consumer {
    private static final String broker = "tcp://broker-test.agilenaas.net:1883";
    private static final String topic = "agstopic001";
    private static final String clientId = "dev001";
    private static final String userName = "we";
    private static final String password = "654321";

    public static void main(String[] args) throws MqttException {
        MqttClient client = getMqttClient();
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                System.out.println("Connect lost, do some thing to solve it. ");
            }

            @Override
            public void messageArrived(String topic, MqttMessage msg) {
                System.out.println("From topic: " + topic + ". Message content: " + new String(msg.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                System.out.println("deliveryComplete");
            }
        });
        client.subscribe(topic);
    }


    private static MqttClient getMqttClient() throws MqttException {
        MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
        MqttConnectOptions opts = new MqttConnectOptions();
//        opts.setUserName(userName);
//        opts.setPassword(password.toCharArray());
        opts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        opts.setCleanSession(false);
        opts.setConnectionTimeout(0);
        opts.setAutomaticReconnect(true);
        System.out.println("Connecting to broker: " + broker + ", ClientId: " + clientId);
        client.connect(opts);
        System.out.println("Connected to broker: " + broker + ", ClientId: " + clientId);
        return client;
    }
}

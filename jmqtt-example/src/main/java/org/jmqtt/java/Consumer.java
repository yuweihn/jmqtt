package org.jmqtt.java;


import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;


public class Consumer {
    private static final String broker = "ssl://localhost:1884";
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
            public void messageArrived(String s, MqttMessage msg) {
                System.out.println("From topic: " + s + ". Message content: " + new String(msg.getPayload()));
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
        opts.setUserName(userName);
        opts.setPassword(password.toCharArray());
        opts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        opts.setCleanSession(false);
        opts.setConnectionTimeout(0);
        opts.setAutomaticReconnect(true);
        opts.setSocketFactory(sslProperties());
        System.out.println("Connecting to broker: " + broker + ", userName: " + userName);
        client.connect(opts);
        System.out.println("Connected to broker: " + broker + ", userName: " + userName);
        return client;
    }

    private static SocketFactory sslProperties() {
        InputStream sslKeyStream = null;
        try {
            sslKeyStream = Consumer.class.getClassLoader().getResourceAsStream("client.cer");
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, password.toCharArray());
            trustStore.setCertificateEntry("Custom CA", CertificateFactory.getInstance("X509").generateCertificate(sslKeyStream));

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            TrustManager[] trustManagers = tmf.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustManagers, null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (sslKeyStream != null) {
                    sslKeyStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

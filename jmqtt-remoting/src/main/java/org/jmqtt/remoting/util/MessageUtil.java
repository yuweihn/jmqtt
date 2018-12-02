package org.jmqtt.remoting.util;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.*;
import org.jmqtt.common.bean.Message;
import org.jmqtt.common.bean.MessageHeader;

import java.util.List;

/**
 * transfer message from Message and MqttMessage
 */
public class MessageUtil {

    public static Message getMessage(MqttMessage mqttMessage){
        Message message = new Message();
        message.setPayload(mqttMessage);
        MqttFixedHeader mqttFixedHead = mqttMessage.fixedHeader();
        message.setType(Message.Type.valueOf(mqttFixedHead.messageType().value()));
        return message;
    }

    public static MqttUnsubAckMessage getUnSubAckMessage(int messageId){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK,false,MqttQoS.AT_MOST_ONCE,false,0);
        MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        return new MqttUnsubAckMessage(fixedHeader,idVariableHeader);
    }

    public static int getMessageId(MqttMessage mqttMessage){
        MqttMessageIdVariableHeader idVariableHeader = (MqttMessageIdVariableHeader) mqttMessage.variableHeader();
        return idVariableHeader.messageId();
    }

    public static int getMinQos(int qos1,int qos2){
        if(qos1 < qos2){
            return qos1;
        }
        return qos2;
    }

    public static MqttMessage getPubRelMessage(int messageId){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL,false,MqttQoS.AT_MOST_ONCE,false,0);
        MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        return new MqttMessage(fixedHeader,idVariableHeader);
    }

    public static MqttPublishMessage getPubMessage(Message message,boolean dup,int qos,int messageId){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH,dup,MqttQoS.valueOf(qos),false,0);
        MqttPublishVariableHeader publishVariableHeader = new MqttPublishVariableHeader((String) message.getHeader(MessageHeader.TOPIC),messageId);
        return new MqttPublishMessage(fixedHeader,publishVariableHeader,(ByteBuf)message.getPayload());
    }

    public static MqttMessage getSubAckMessage(int messageId, List<Integer> qos){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK,false,MqttQoS.AT_MOST_ONCE,false,0);
        MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttSubAckPayload subAckPayload = new MqttSubAckPayload(qos);
        return new MqttSubAckMessage(fixedHeader,idVariableHeader,subAckPayload);
    }

    public static MqttMessage getPingRespMessage(){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP,false,MqttQoS.AT_MOST_ONCE,false,0);
        MqttMessage mqttMessage = new MqttMessage(fixedHeader);
        return mqttMessage;
    }

    public static MqttMessage getPubComMessage(int messageId){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP,false,MqttQoS.AT_MOST_ONCE,false,0);
        MqttMessage mqttMessage = new MqttMessage(fixedHeader,MqttMessageIdVariableHeader.from(messageId));
        return mqttMessage;
    }

    public static MqttMessage getPubRecMessage(int messageId){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC,false,MqttQoS.AT_MOST_ONCE,false,0);
        MqttMessage mqttMessage = new MqttMessage(fixedHeader,MqttMessageIdVariableHeader.from(messageId));
        return mqttMessage;
    }

    public static MqttPubAckMessage getPubAckMessage(int messageId){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK,false,MqttQoS.AT_MOST_ONCE,false,0);
        MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        return new MqttPubAckMessage(fixedHeader,idVariableHeader);
    }

    public static MqttConnAckMessage getConnectAckMessage(MqttConnectReturnCode returnCode,boolean sessionPresent){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnAckVariableHeader variableHeade = new MqttConnAckVariableHeader(returnCode,sessionPresent);
        return new MqttConnAckMessage(fixedHeader,variableHeade);
    }
}

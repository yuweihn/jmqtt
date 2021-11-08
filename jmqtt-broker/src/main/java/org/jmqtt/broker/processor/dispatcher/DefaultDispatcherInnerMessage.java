package org.jmqtt.broker.processor.dispatcher;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.helper.RejectHandler;
import org.jmqtt.broker.common.helper.ThreadFactoryImpl;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.Message.Stage;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.processor.HighPerformanceMessageHandler;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * 默认的消息分发实现类
 */
public class DefaultDispatcherInnerMessage extends HighPerformanceMessageHandler implements
    InnerMessageDispatcher {

    private static final Logger log = JmqttLogger.messageTraceLog;
    private static final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>(100000);
    private boolean stopped = false;
    private ThreadPoolExecutor pollThread;
    private int pollThreadNum;
    private SubscriptionMatcher subscriptionMatcher;
    private SessionStore sessionStore;


    public DefaultDispatcherInnerMessage(BrokerController brokerController) {
        super(brokerController);
        this.pollThreadNum = brokerController.getBrokerConfig().getPollThreadNum();
        this.subscriptionMatcher = brokerController.getSubscriptionMatcher();
        this.sessionStore = brokerController.getSessionStore();
    }

    @Override
    public void start() {
        this.pollThread = new ThreadPoolExecutor(pollThreadNum,
            pollThreadNum,
            60 * 1000,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100000),
            new ThreadFactoryImpl("pollMessage2Subscriber"),
            new RejectHandler("pollMessage", 100000));

        new Thread(() -> {
            int waitTime = 1000;
            while (!stopped) {
                try {
                    List<Message> messageList = new ArrayList<>(32);
                    Message message;
                    for (int i = 0; i < 32; i++) {
                        if (i == 0) {
                            message = messageQueue.poll(waitTime, TimeUnit.MILLISECONDS);
                        } else {
                            message = messageQueue.poll();
                        }
                        if (Objects.nonNull(message)) {
                            messageList.add(message);
                        } else {
                            break;
                        }
                    }
                    if (messageList.size() > 0) {
                        AsyncDispatcher dispatcher = new AsyncDispatcher(messageList);
                        pollThread.submit(dispatcher).get();
                    }
                } catch (InterruptedException e) {
                    LogUtil.warn(log, "poll message wrong.");
                } catch (ExecutionException e) {
                    LogUtil.warn(log, "AsyncDispatcher get() wrong.");
                }
            }
        }).start();
    }

    @Override
    public boolean appendMessage(Message message) {
        ClientSession clientSession = ConnectManager.getInstance().getClient(message.getClientId());
        message.setTenantCode(clientSession.getTenantCode());
        message.setBizCode(clientSession.getBizCode());
        boolean isNotFull = messageQueue.offer(message);
        if (!isNotFull) {
            LogUtil.warn(log, "[PubMessage] -> the buffer queue is full");
        }
        return isNotFull;
    }

    @Override
    public void shutdown() {
        this.stopped = true;
        this.pollThread.shutdown();
    }

    class AsyncDispatcher implements Runnable {

        private List<Message> messages;

        AsyncDispatcher(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public void run() {
            if (Objects.nonNull(messages)) {
                try {
                    for (Message message : messages) {
                        Set<Subscription> subscriptions = null;
                        if (message.getStage() == Stage.NEW_ARRIVED) {
                            //新消息，按照本地非共享订阅关系进行匹配调度
                            subscriptions = subscriptionMatcher
                                .match((String) message.getHeader(MessageHeader.TOPIC),
                                    message.getClientId());
                        } else if (message.getStage() == Stage.GROUP_DISPATHER) {
                            //路由后的消息，直接按已在共享订阅关系中设置的匹配调度
                            subscriptions = new HashSet<>();
                            subscriptions.add(message.getDispatcher());
                        }

                        for (Subscription subscription : subscriptions) {
                            String clientId = subscription.getClientId();
                            if (ConnectManager.getInstance().containClient(clientId)) {
                                ClientSession clientSession = ConnectManager.getInstance()
                                    .getClient(clientId);
                                int qos = MessageUtil
                                    .getMinQos((int) message.getHeader(MessageHeader.QOS),
                                        subscription.getQos());
                                int messageId = clientSession.generateMessageId();
                                message.putHeader(MessageHeader.QOS, qos);
                                message.setMsgId(messageId);
                                if (qos > 0) {
                                    cacheOutflowMsg(clientId, message);
                                }
                                MqttPublishMessage publishMessage = MessageUtil
                                    .getPubMessage(message, false);
                                clientSession.getCtx().writeAndFlush(publishMessage);
                            } else {
                                sessionStore.storeOfflineMsg(clientId, message);
                            }
                        }
                    }
                } catch (Exception ex) {
                    LogUtil.warn(log, "Dispatcher message failure,cause={}", ex);
                }
            }
        }

    }
}

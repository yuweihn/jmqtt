package org.jmqtt.broker.remoting.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.timeout.IdleStateHandler;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.config.NettyConfig;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.helper.Pair;
import org.jmqtt.broker.common.helper.ThreadFactoryImpl;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.processor.RequestProcessor;
import org.jmqtt.broker.remoting.RemotingService;
import org.jmqtt.broker.remoting.netty.codec.ByteBuf2WebSocketEncoder;
import org.jmqtt.broker.remoting.netty.codec.WebSocket2ByteBufDecoder;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;


public class NettyRemotingServer implements RemotingService {
    int coreThreadNum = Runtime.getRuntime().availableProcessors();
    private static final Logger                                           log = JmqttLogger.remotingLog;
    private NettyConfig                                                   nettyConfig;
    private EventLoopGroup                                                selectorGroup;
    private EventLoopGroup                                                ioGroup;
    private Class<? extends ServerChannel>                                clazz;
    private Map<MqttMessageType, Pair<RequestProcessor, ExecutorService>> processorTable;
    private NettyEventExecutor                                            nettyEventExecutor;
    private BrokerConfig                                                  brokerConfig;

    public NettyRemotingServer(BrokerConfig brokerConfig, NettyConfig nettyConfig, ChannelEventListener listener) {
        this.nettyConfig = nettyConfig;
        this.processorTable = new HashMap<>();
        this.brokerConfig = brokerConfig;
        this.nettyEventExecutor = new NettyEventExecutor(listener);

        if (!nettyConfig.getUseEpoll()) {
            selectorGroup = new NioEventLoopGroup(coreThreadNum,
                    new ThreadFactoryImpl("SelectorEventGroup"));
            ioGroup = new NioEventLoopGroup(coreThreadNum * 2,
                    new ThreadFactoryImpl("IOEventGroup"));
            clazz = NioServerSocketChannel.class;
        } else {
            selectorGroup = new EpollEventLoopGroup(coreThreadNum,
                    new ThreadFactoryImpl("SelectorEventGroup"));
            ioGroup = new EpollEventLoopGroup(coreThreadNum * 2,
                    new ThreadFactoryImpl("IOEventGroup"));
            clazz = EpollServerSocketChannel.class;
        }
    }


    @Override
    public void start() {
        //Netty event executor start
        this.nettyEventExecutor.start();
        // start TCP server
        if (nettyConfig.getStartTcp()) {
            startTcpServer(false, nettyConfig.getTcpPort());
        }

        if (nettyConfig.getStartSslTcp()) {
            startTcpServer(true, nettyConfig.getSslTcpPort());
        }

        // start Websocket server
        if (nettyConfig.getStartWebsocket()) {
            startWebsocketServer(false, nettyConfig.getWebsocketPort());
        }

        if (nettyConfig.getStartSslWebsocket()) {
            startWebsocketServer(true, nettyConfig.getSslWebsocketPort());
        }
    }

    private void startWebsocketServer(boolean useSsl, Integer port) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(selectorGroup, ioGroup)
                .channel(clazz)
                .option(ChannelOption.SO_BACKLOG, nettyConfig.getTcpBackLog())
                .childOption(ChannelOption.TCP_NODELAY, nettyConfig.getTcpNoDelay())
                .childOption(ChannelOption.SO_SNDBUF, nettyConfig.getTcpSndBuf())
                .option(ChannelOption.SO_RCVBUF, nettyConfig.getTcpRcvBuf())
                .option(ChannelOption.SO_REUSEADDR, nettyConfig.getTcpReuseAddr())
                .childOption(ChannelOption.SO_KEEPALIVE, nettyConfig.getTcpKeepAlive())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        if (useSsl) {
                            pipeline.addLast("ssl", NettySslHandler.getSslHandler(
                                    socketChannel,
                                    nettyConfig.getUseClientCA(),
                                    nettyConfig.getSslKeyStoreType(),
                                    brokerConfig.getJmqttHome() + nettyConfig.getSslKeyFilePath(),
                                    nettyConfig.getSslManagerPwd(),
                                    nettyConfig.getSslStorePwd()
                            ));
                        }
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, 60))
                                .addLast("httpCodec", new HttpServerCodec())
                                .addLast("aggregator", new HttpObjectAggregator(65535))
                                .addLast("compressor ", new HttpContentCompressor())
                                .addLast("webSocketHandler", new WebSocketServerProtocolHandler("/mqtt", MixAll.MQTT_VERSION_SUPPORT, false,65536))
                                .addLast("webSocket2ByteBufDecoder", new WebSocket2ByteBufDecoder())
                                .addLast("byteBuf2WebSocketEncoder", new ByteBuf2WebSocketEncoder())
                                .addLast("mqttDecoder", new MqttDecoder(nettyConfig.getMaxMsgSize()))
                                .addLast("mqttEncoder", MqttEncoder.INSTANCE)
                                .addLast("nettyConnectionManager", new NettyConnectHandler(
                                        nettyEventExecutor))
                                .addLast("nettyMqttHandler", new NettyMqttHandler());
                    }
                });
        if (nettyConfig.getPooledByteBufAllocatorEnable()) {
            bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
        try {
            ChannelFuture future = bootstrap.bind(port).sync();
            LogUtil.info(log,"Start webSocket server {}  success,port = {}", useSsl ? "with ssl" : "", port);
        } catch (InterruptedException ex) {
            LogUtil.error(log,"Start webSocket server {} failure.cause={}", useSsl ? "with ssl" : "", ex);
        }
    }

    private void startTcpServer(boolean useSsl, Integer port) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(selectorGroup, ioGroup)
                .channel(clazz)
                .option(ChannelOption.SO_BACKLOG, nettyConfig.getTcpBackLog())
                .childOption(ChannelOption.TCP_NODELAY, nettyConfig.getTcpNoDelay())
                .childOption(ChannelOption.SO_SNDBUF, nettyConfig.getTcpSndBuf())
                .option(ChannelOption.SO_RCVBUF, nettyConfig.getTcpRcvBuf())
                .option(ChannelOption.SO_REUSEADDR, nettyConfig.getTcpReuseAddr())
                .childOption(ChannelOption.SO_KEEPALIVE, nettyConfig.getTcpKeepAlive())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        if (useSsl) {
                            pipeline.addLast("ssl", NettySslHandler.getSslHandler(
                                    socketChannel,
                                    nettyConfig.getUseClientCA(),
                                    nettyConfig.getSslKeyStoreType(),
                                    brokerConfig.getJmqttHome() + nettyConfig.getSslKeyFilePath(),
                                    nettyConfig.getSslManagerPwd(),
                                    nettyConfig.getSslStorePwd()
                            ));
                        }
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(60, 0, 0))
                                .addLast("mqttEncoder", MqttEncoder.INSTANCE)
                                .addLast("mqttDecoder", new MqttDecoder(nettyConfig.getMaxMsgSize()))
                                .addLast("nettyConnectionManager", new NettyConnectHandler(
                                        nettyEventExecutor))
                                .addLast("nettyMqttHandler", new NettyMqttHandler());
                    }
                });
        if (nettyConfig.getPooledByteBufAllocatorEnable()) {
            bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
        try {
            ChannelFuture future = bootstrap.bind(port).sync();
            LogUtil.info(log,"Start tcp server {} success,port = {}", useSsl ? "with ssl" : "", port);
        } catch (InterruptedException ex) {
            LogUtil.error(log,"Start tcp server {} failure.cause={}", useSsl ? "with ssl" : "", ex);
        }
    }


    @Override
    public void shutdown() {
        if (selectorGroup != null) {
            selectorGroup.shutdownGracefully();
        }
        if (ioGroup != null) {
            ioGroup.shutdownGracefully();
        }
    }

    public void registerProcessor(MqttMessageType mqttType, RequestProcessor processor, ExecutorService executorService) {
        this.processorTable.put(mqttType, new Pair<>(processor, executorService));
    }

    class NettyMqttHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object obj) {
            MqttMessage mqttMessage = (MqttMessage) obj;
            if (mqttMessage != null && mqttMessage.decoderResult().isSuccess()) {
                MqttMessageType messageType = mqttMessage.fixedHeader().messageType();
                LogUtil.debug(log,"[Remoting] -> receive mqtt code,type:{},name:{}", messageType.value(), messageType.name());
                Runnable runnable = () -> processorTable.get(messageType).getObject1().processRequest(ctx, mqttMessage);
                try {
                    processorTable.get(messageType).getObject2().submit(runnable);
                } catch (RejectedExecutionException ex) {
                    LogUtil.warn(log,"Reject mqtt request,cause={}", ex.getMessage());
                }
            } else {
                ctx.close();
            }
        }
    }
}

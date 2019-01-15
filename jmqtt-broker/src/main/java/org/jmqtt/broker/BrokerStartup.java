package org.jmqtt.broker;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.exception.BrokerException;
import org.jmqtt.common.config.BrokerConfig;
import org.jmqtt.common.config.NettyConfig;
import org.jmqtt.common.config.StoreConfig;
import org.jmqtt.common.helper.MixAll;
import org.jmqtt.common.log.LoggerName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class BrokerStartup {

    public static void main(String[] args) {
        try {
            start(args);
        } catch (Exception e) {
            System.out.println("Jmqtt start failure,cause = " + e);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static BrokerController start(String[] args) throws Exception {

        Options options = buildOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options,args);
        String jmqttHome = null;
        String jmqttConfigPath = null;
        BrokerConfig brokerConfig = new BrokerConfig();
        NettyConfig nettyConfig = new NettyConfig();
        StoreConfig storeConfig = new StoreConfig();
        if(commandLine != null){
            jmqttHome = commandLine.getOptionValue("h");
            jmqttConfigPath = commandLine.getOptionValue("c");
        }
        if(StringUtils.isNotEmpty(jmqttConfigPath)){
            initConfig(jmqttConfigPath,brokerConfig,nettyConfig);
        }
        if(StringUtils.isEmpty(jmqttHome)){
            jmqttHome = brokerConfig.getJmqttHome();
        }
        if(StringUtils.isEmpty(jmqttHome)){
            throw new Exception("please set JMQTT_HOME.");
        }
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        configurator.doConfigure(jmqttHome + "/conf/logback_broker.xml");

        BrokerController brokerController = new BrokerController(brokerConfig,nettyConfig, storeConfig);
        brokerController.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                brokerController.shutdown();
            }
        }));

        return brokerController;
    }

    private static Options buildOptions(){
        Options options = new Options();
        Option opt = new Option("h",true,"jmqttHome,eg: /wls/xxx");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("c",true,"jmqtt.properties path,eg: /wls/xxx/xxx.properties");
        opt.setRequired(false);
        options.addOption(opt);

        return options;
    }

    private static void initConfig(String jmqttConfigPath,BrokerConfig brokerConfig,NettyConfig nettyConfig){
        Properties properties = new Properties();
        BufferedReader  bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(jmqttConfigPath));
            properties.load(bufferedReader);
            MixAll.properties2POJO(properties,brokerConfig);
            MixAll.properties2POJO(properties,nettyConfig);
        } catch (FileNotFoundException e) {
            System.out.println("jmqtt.properties cannot find,cause = " + e);
        } catch (IOException e) {
            System.out.println("Handle jmqttConfig IO exception,cause = " + e);
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                System.out.println("Handle jmqttConfig IO exception,cause = " + e);
            }
        }
    }

}

package org.jmqtt.broker;


import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.jmqtt.support.config.BrokerConfig;
import org.jmqtt.support.config.NettyConfig;
import org.jmqtt.support.helper.MixAll;

import java.io.*;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;


/**
 * 技术问题，二次开发问题：请加 qq群：578185385
 */
public class BrokerStartup {

//    public static void main(String[] args) {
//        try {
//            start(args);
//        } catch (Exception e) {
//            System.out.println("Jmqtt start failure,cause = " + e);
//            e.printStackTrace();
//            System.exit(-1);
//        }
//    }

    public static BrokerController start(BrokerConfig brokerConfig, NettyConfig nettyConfig) throws Exception {
//        String jmqttHome = brokerConfig.getJmqttHome();
//        String logLevel = null;
//        if (StringUtils.isEmpty(jmqttHome)) {
//            throw new Exception("please set JMQTT_HOME.");
//        }
//        String jmqttConfigPath = jmqttHome + File.separator + "jmqtt.properties";
//        initConfig(jmqttConfigPath, brokerConfig, nettyConfig);

//        // 日志配置加载
//        try {
//            LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
//            File file = new File(jmqttHome + File.separator + "log4j2.xml");
//            context.setConfigLocation(file.toURI());
//            Configuration configuration = context.getConfiguration();
//            Map<String, LoggerConfig> loggerConfigMap = configuration.getLoggers();
//            Level newLevel = Level.INFO;
//            for (LoggerConfig value : loggerConfigMap.values()) {
//                value.setLevel(newLevel);
//            }
//            context.updateLoggers(configuration);
//        } catch (Exception ex) {
//            System.err.print("Log4j2 load error,ex:" + ex);
//        }

        InputStream sslKeyStream = BrokerStartup.class.getClassLoader().getResourceAsStream("conf/server.pfx");
        nettyConfig.setSslKeyFileContent(read(sslKeyStream));
        if (sslKeyStream != null) {
            sslKeyStream.close();
        }

        // 启动服务，线程等
        BrokerController brokerController = new BrokerController(brokerConfig, nettyConfig);
        brokerController.start();

        Runtime.getRuntime().addShutdownHook(new Thread(brokerController::shutdown));
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

        opt = new Option("l",true,"DEBUG");
        opt.setRequired(false);
        options.addOption(opt);

        return options;
    }

    private static byte[] read(InputStream in) {
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * convert properties to java config class
     * @param jmqttConfigPath
     * @param brokerConfig
     * @param nettyConfig
     */
    private static void initConfig(String jmqttConfigPath, BrokerConfig brokerConfig, NettyConfig nettyConfig) {
        Properties properties = new Properties();
        BufferedReader  bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(jmqttConfigPath));
            properties.load(bufferedReader);
            MixAll.properties2POJO(properties, brokerConfig);
            MixAll.properties2POJO(properties, nettyConfig);
        } catch (FileNotFoundException e) {
            System.out.println("jmqtt.properties cannot find,cause + " + e + ",path:"+jmqttConfigPath);
        } catch (IOException e) {
            System.out.println("Handle jmqttConfig IO exception,cause = " + e);
        } finally {
            try {
                if(Objects.nonNull(bufferedReader)){
                    bufferedReader.close();
                }
            } catch (IOException e) {
                System.out.println("Handle jmqttConfig IO exception,cause = " + e);
            }
        }
    }

}

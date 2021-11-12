package org.jmqtt.broker.common.helper;

import org.jmqtt.broker.common.log.LogUtil;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Properties;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;


public class MixAll {

    public static String MQTT_VERSION_SUPPORT = "mqtt,mqtt3.1,mqttv3.1.1,mqttv5.0";

    public static boolean createIfNotExistsDir(File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    public static String dateFormat(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(time);
    }


    public static <T> T pluginInit(String classFullName){
        try {
            Class<?> clazz = Class.forName(classFullName);
            return (T) clazz.newInstance();
        } catch (Exception e) {
            System.err.println("Load class and init error," + e);
            System.exit(-1);
        }
        return null;
    }


    public static boolean isEmpty(Collection collection) {
        if (collection == null || collection.size() == 0) {
            return true;
        }
        return false;
    }

    public static String getLocalIp(){
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "UN_KNOW";
    }

    public static byte[] uncompress(final byte[] src) throws IOException {
        byte[] result = src;
        byte[] uncompressData = new byte[src.length];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(src);
        InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(src.length);

        try {
            while (true) {
                int len = inflaterInputStream.read(uncompressData, 0, uncompressData.length);
                if (len <= 0) {
                    break;
                }
                byteArrayOutputStream.write(uncompressData, 0, len);
            }
            byteArrayOutputStream.flush();
            result = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                byteArrayInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inflaterInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                byteArrayOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static byte[] compress(final byte[] src, final int level) throws IOException {
        byte[] result = src;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(src.length);
        Deflater deflater = new Deflater(level);
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);
        try {
            deflaterOutputStream.write(src);
            deflaterOutputStream.flush();
            deflaterOutputStream.close();
            result = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            deflater.end();
            throw e;
        } finally {
            try {
                byteArrayOutputStream.close();
            } catch (IOException ignored) {
            }
            deflater.end();
        }
        return result;
    }

    public static void printProperties(Logger log, Object obj) {
        Class clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        if (fields != null) {
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    String key = field.getName();
                    Object value = field.get(obj);
                    LogUtil.info(log,"{} = {}", key, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * transfer properties 2 pojo
     */
    public static void properties2POJO(Properties properties, Object obj) {
        Method[] methods = obj.getClass().getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("set")) {
                try {
                    String tmp = methodName.substring(4);
                    String firstChar = methodName.substring(3, 4);
                    String key = firstChar.toLowerCase() + tmp;
                    String value = properties.getProperty(key);
                    if (value != null) {
                        Class<?>[] types = method.getParameterTypes();
                        if (types != null && types.length > 0) {
                            String type = types[0].getSimpleName();
                            Object arg = null;
                            if (type.equals("int") || type.equals("Integer")) {
                                arg = Integer.parseInt(value);
                            } else if (type.equals("float") || type.equals("Float")) {
                                arg = Float.parseFloat(value);
                            } else if (type.equals("double") || type.equals("Double")) {
                                arg = Double.parseDouble(value);
                            } else if (type.equals("long") || type.equals("Long")) {
                                arg = Long.parseLong(value);
                            } else if (type.equals("boolean") || type.equals("Boolean")) {
                                arg = Boolean.parseBoolean(value);
                            } else if (type.equals("String")) {
                                arg = value;
                            } else {
                                continue;
                            }
                            method.invoke(obj, arg);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

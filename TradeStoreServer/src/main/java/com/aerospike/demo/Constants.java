package com.aerospike.demo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Constants {
    public static final String PROPERTIES_PATH = "src/main/resources/TradeStoreServer.properties";

    static Properties properties = new Properties();

    static
    {
        try {
            FileInputStream in = new FileInputStream(PROPERTIES_PATH);
            properties.load(in);
        }
        catch(FileNotFoundException e){
            System.err.println("Properties file "+PROPERTIES_PATH+" not found");
            System.exit(1);
        }
        catch (IOException e){
            System.err.println("Error reading properties file "+PROPERTIES_PATH);
        }
    }

    public static final String AEROSPIKE_HOST_PROPERTY_NAME = "AEROSPIKE_HOST";
    public static String AEROSPIKE_HOST = getPropertyValue(AEROSPIKE_HOST_PROPERTY_NAME);

    public static final String TRADE_NAMESPACE_PROPERTY_NAME = "TRADE_NAMESPACE";
    public static String TRADE_NAMESPACE = getPropertyValue(TRADE_NAMESPACE_PROPERTY_NAME);

    public static final String TRADE_SET_PROPERTY_NAME = "TRADE_SET";
    public static String TRADE_SET = getPropertyValue(TRADE_SET_PROPERTY_NAME);

    public static final String WEBSERVER_PORT_PROPERTY_NAME = "WEBSERVER_PORT";
    public static int WEBSERVER_PORT = Integer.parseInt(getPropertyValue(WEBSERVER_PORT_PROPERTY_NAME));

    public static final String DEBUG_PROPERTY_NAME = "DEBUG";
    public static boolean DEBUG = Boolean.parseBoolean(getPropertyValue(DEBUG_PROPERTY_NAME));

    public static final String CONTRACT_SUMMARY_NAMESPACE_PROPERTY_NAME="CONTRACT_SUMMARY_NAMESPACE";
    public static String CONTRACT_SUMMARY_NAMESPACE = getPropertyValue(CONTRACT_SUMMARY_NAMESPACE_PROPERTY_NAME);

    public static final String CONTRACT_SUMMARY_SET_PROPERTY_NAME = "CONTRACT_SUMMARY_SET";
    public static String CONTRACT_SUMMARY_SET = getPropertyValue(CONTRACT_SUMMARY_SET_PROPERTY_NAME);

    public static final String CONTRACT_RECORD_SHARD_COUNT_PROPERTY_NAME = "CONTRACT_RECORD_SHARD_COUNT";
    public static int CONTRACT_RECORD_SHARD_COUNT = Integer.parseInt(getPropertyValue(CONTRACT_RECORD_SHARD_COUNT_PROPERTY_NAME));

    public static final String AERO_MAX_CONNS_PER_NODE_PROPERTY_NAME = "AERO_MAX_CONNS_PER_NODE";
    public static int AERO_MAX_CONNS_PER_NODE = Integer.parseInt(getPropertyValue(AERO_MAX_CONNS_PER_NODE_PROPERTY_NAME));


    public static final String CONTRACT_RECORD_BIN = "contractRecord";
    public static final String CONTRACT_PRICE_SUMMARY_BIN = "cntrctPriceSum";
    public static final String TICKER_FIELD_NAME = "ticker";
    public static final String VOLUME_FIELD_NAME = "volume";
    public static final String PRICE_FIELD_NAME = "price";
    public static final String TIMESTAMP_FIELD_NAME = "timestamp";

    /**
     * Get property value - but allow this to be overridden with a value from the environment
     * @param propertyName
     * @return
     */
    private static String getPropertyValue(String propertyName){
        String propertyValue = System.getenv(propertyName) != null ? System.getenv(propertyName) : (String)properties.get(propertyName);
        System.out.println(String.format("Property %s set to value %s",propertyName,propertyValue));
        return propertyValue;
    }
}

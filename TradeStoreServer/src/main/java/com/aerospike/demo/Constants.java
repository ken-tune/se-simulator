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
    public static String AEROSPIKE_HOST = (String)properties.get(AEROSPIKE_HOST_PROPERTY_NAME);

    public static final String AEROSPIKE_NAMESPACE_PROPERTY_NAME = "AEROSPIKE_NAMESPACE";
    public static String AEROSPIKE_NAMESPACE = (String)properties.get(AEROSPIKE_NAMESPACE_PROPERTY_NAME);

    public static final String AEROSPIKE_TRADE_SET_PROPERTY_NAME = "AEROSPIKE_SET";
    public static String AEROSPIKE_TRADE_SET = (String)properties.get(AEROSPIKE_TRADE_SET_PROPERTY_NAME);

    public static final String WEBSERVER_PORT_PROPERTY_NAME = "WEBSERVER_PORT";
    public static int WEBSERVER_PORT = Integer.parseInt((String)properties.get(WEBSERVER_PORT_PROPERTY_NAME));

    public static final String DEBUG_PROPERTY_NAME = "DEBUG";
    public static boolean DEBUG = Boolean.parseBoolean((String)properties.get(DEBUG_PROPERTY_NAME));

    public static final String AEROSPIKE_CONTRACT_SUMMARY_SET_PROPERTY_NAME = "AEROSPIKE_CONTRACT_SUMMARY_SET";
    public static String AEROSPIKE_CONTRACT_SUMMARY_SET = (String)properties.get(AEROSPIKE_CONTRACT_SUMMARY_SET_PROPERTY_NAME);

    public static final String CONTRACT_RECORD_SHARD_COUNT_PROPERTY_NAME = "CONTRACT_RECORD_SHARD_COUNT";
    public static int CONTRACT_RECORD_SHARD_COUNT = Integer.parseInt((String)properties.get(CONTRACT_RECORD_SHARD_COUNT_PROPERTY_NAME));

    public static final String CONTRACT_RECORD_BIN = "contractRecord";
    public static final String CONTRACT_PRICE_SUMMARY_BIN = "cntrctPriceSum";
    public static final String TICKER_FIELD_NAME = "ticker";
    public static final String VOLUME_FIELD_NAME = "volume";
    public static final String PRICE_FIELD_NAME = "price";
    public static final String TIMESTAMP_FIELD_NAME = "timestamp";
}

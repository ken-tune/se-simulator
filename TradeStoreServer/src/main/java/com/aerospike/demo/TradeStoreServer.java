package com.aerospike.demo;

import com.aerospike.client.*;
import com.aerospike.client.cdt.*;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.Statement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.*;

public class TradeStoreServer {
    AerospikeClient aerospikeClient;

    // Constructor
    public TradeStoreServer() throws IOException{
        //httpServer = HttpServer.create(new InetSocketAddress(Constants.WEBSERVER_PORT), 0);
        aerospikeClient = new AerospikeClient(Constants.AEROSPIKE_HOST,3000);
    }

    // Save trade. Will throw ParseException if JSON does not fit required schema
    public void saveTrade(JsonNode jsonNode) throws ParseException{
        // Put the Aerospike object together
        int fields = jsonNode.size();
        Bin[] bins = new Bin[fields];
        int fieldCounter = 0;
        Iterator<String> fieldsIterator = jsonNode.fieldNames();
        while(fieldsIterator.hasNext()){
            String fieldName = fieldsIterator.next();
            Value value = null;
            switch(jsonNode.get(fieldName).getNodeType()){
                case BOOLEAN: value = new Value.BooleanValue(jsonNode.get(fieldName).asBoolean());break;
                case STRING: value = new Value.StringValue(jsonNode.get(fieldName).asText());break;
                case NUMBER: value = new Value.DoubleValue(jsonNode.get(fieldName).asDouble());break;
                default: throw new ParseException("JSON in wrong format",0);
            }
            bins[fieldCounter] = new Bin(fieldName,value);
            fieldCounter++;
        }
        // Create a key
        UUID u = UUID.randomUUID();
        Key key  = new Key(Constants.AEROSPIKE_NAMESPACE,Constants.AEROSPIKE_TRADE_SET,u.toString());
        // Save
        aerospikeClient.put(new WritePolicy(),key,bins);
        updateContractRecord(jsonNode);
    }

    void updateContractRecord(JsonNode trade){
        Key key = contractRecordASKeyForTicker(trade.get(Constants.TICKER_FIELD_NAME).asText());
        long tradeVolume = trade.get(Constants.VOLUME_FIELD_NAME).asLong();
        double tradePrice = trade.get(Constants.PRICE_FIELD_NAME).asDouble();
        long timestamp = trade.get(Constants.TIMESTAMP_FIELD_NAME).asLong();
        ListPolicy p = new ListPolicy();
        aerospikeClient.operate(aerospikeClient.writePolicyDefault, key,
                // Increment the trade volume field
                MapOperation.increment(new MapPolicy(),Constants.CONTRACT_RECORD_BIN,Value.get(Constants.VOLUME_FIELD_NAME),Value.get(tradeVolume)),
                // Store the price in an ordered list - need to create the list first if it doesn't exist
                ListOperation.create(Constants.CONTRACT_RECORD_BIN,ListOrder.ORDERED,false,CTX.mapKey(Value.get(Constants.PRICE_FIELD_NAME))),
                ListOperation.append(Constants.CONTRACT_RECORD_BIN,Value.get(tradePrice),CTX.mapKey(Value.get(Constants.PRICE_FIELD_NAME))),
                // Trim the ordered price list - keep only the rightmost value, which will be the maximum
                ListOperation.trim(Constants.CONTRACT_RECORD_BIN,-1,1,CTX.mapKey(Value.get(Constants.PRICE_FIELD_NAME))),
                // Store the timestamp in an ordered list - need to create the list first if it doesn't exist
                ListOperation.create(Constants.CONTRACT_RECORD_BIN,ListOrder.ORDERED,false,CTX.mapKey(Value.get(Constants.TIMESTAMP_FIELD_NAME))),
                ListOperation.append(Constants.CONTRACT_RECORD_BIN,Value.get(timestamp),CTX.mapKey(Value.get(Constants.TIMESTAMP_FIELD_NAME))),
                // Trim the ordered price list - keep only the rightmost value, which will be the maximum
                ListOperation.trim(Constants.CONTRACT_RECORD_BIN,-1,1,CTX.mapKey(Value.get(Constants.TIMESTAMP_FIELD_NAME))),
                // Make sure there's a map associated with the supplied price
                MapOperation.create(Constants.CONTRACT_PRICE_SUMMARY_BIN,MapOrder.KEY_VALUE_ORDERED,CTX.mapKey(Value.get(tradePrice))),
                // Increment the volume for this price
                MapOperation.increment(new MapPolicy(),Constants.CONTRACT_PRICE_SUMMARY_BIN,Value.get(Constants.VOLUME_FIELD_NAME),Value.get(tradeVolume),CTX.mapKey(Value.get(tradePrice))),
                // Store the timestamp in an ordered list in the above map - need to create the list first if it doesn't exist
                ListOperation.create(Constants.CONTRACT_PRICE_SUMMARY_BIN,ListOrder.ORDERED,false,CTX.mapKey(Value.get(tradePrice)),CTX.mapKey(Value.get(Constants.TIMESTAMP_FIELD_NAME))),
                ListOperation.append(Constants.CONTRACT_PRICE_SUMMARY_BIN,Value.get(timestamp),CTX.mapKey(Value.get(tradePrice)),CTX.mapKey(Value.get(Constants.TIMESTAMP_FIELD_NAME))),
                // Trim the ordered price list - keep only the rightmost value, which will be the maximum
                ListOperation.trim(Constants.CONTRACT_PRICE_SUMMARY_BIN,-1,1,CTX.mapKey(Value.get(tradePrice)),CTX.mapKey(Value.get(Constants.TIMESTAMP_FIELD_NAME)))


        );
    }
    /**
     * Given a serialized json object, return it's equivalent representation as a JsonNode.
     *
     * @param jsonString A given JSON as a String.
     * @return The given JSON as a JsonNode.
     * @throws IOException an IOException will be thrown in case of an error.
     */
    public static JsonNode convertStringToJsonNode(String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, JsonNode.class);
    }

    /**
     * Return the Aerospike Key for a contract record with a given ticker
     * @param ticker
     * @return
     */
    static Key contractRecordASKeyForTicker(String ticker){
        return new Key(Constants.AEROSPIKE_NAMESPACE,Constants.AEROSPIKE_CONTRACT_SUMMARY_SET,ticker);

    }

    /**
     * Get the aggregate volume traded for a given ticker
     * @param ticker
     * @return
     */
    public long getAggregateVolumeForTicker(String ticker){
        Record r = aerospikeClient.operate(aerospikeClient.writePolicyDefault,TradeStoreServer.contractRecordASKeyForTicker(ticker),
                MapOperation.getByKey(Constants.CONTRACT_RECORD_BIN,Value.get(Constants.VOLUME_FIELD_NAME),MapReturnType.VALUE));
        return r.getLong(Constants.CONTRACT_RECORD_BIN);

    }

    /**
     * Get the highest price recorded for a given ticker
     * @param ticker
     * @return
     */
    public double getHighestPriceTradedForTicker(String ticker){
        Record r = aerospikeClient.operate(aerospikeClient.writePolicyDefault,TradeStoreServer.contractRecordASKeyForTicker(ticker),
                ListOperation.getByIndex(Constants.CONTRACT_RECORD_BIN,0,ListReturnType.VALUE, CTX.mapKey(Value.get(Constants.PRICE_FIELD_NAME))));
        return r.getDouble(Constants.CONTRACT_RECORD_BIN);
    }

    /**
     * Get the most recent timestamp for a trade on a given ticker
     * @param ticker
     * @return
     */
    public long getMostRecentTradeTimestampForTicker(String ticker){
        Record r = aerospikeClient.operate(aerospikeClient.writePolicyDefault,TradeStoreServer.contractRecordASKeyForTicker(ticker),
                ListOperation.getByIndex(Constants.CONTRACT_RECORD_BIN,0,ListReturnType.VALUE, CTX.mapKey(Value.get(Constants.TIMESTAMP_FIELD_NAME))));

        Bin newBin = new Bin("newBin", 100);
        Bin otherBin = new Bin("otherBin", 100);
        Key key = new Key("test", "test", "key");
        aerospikeClient.operate(null, key, Operation.delete(), Operation.add(newBin), Operation.add(otherBin));
        return r.getLong(Constants.CONTRACT_RECORD_BIN);
    }

    /**
     * Get the aggregate volume traded for a given ticker / price combination
     * @param ticker
     * @param price
     * @return
     */
    public long getAggregateVolumeForTickerAndPrice(String ticker,double price){
        Record r = aerospikeClient.operate(aerospikeClient.writePolicyDefault,TradeStoreServer.contractRecordASKeyForTicker(ticker),
                MapOperation.getByKey(Constants.CONTRACT_PRICE_SUMMARY_BIN,Value.get(Constants.VOLUME_FIELD_NAME),MapReturnType.VALUE,CTX.mapKey(Value.get(price))));

        return r.getLong(Constants.CONTRACT_PRICE_SUMMARY_BIN);

    }

    /**
     * Get the most recent timestamp for trades on a given ticker at a specific price
     * @param ticker
     * @return
     */
    public long getMostRecentTradeTimestampForTickerAndPrice(String ticker,double price){
        Record r = aerospikeClient.operate(aerospikeClient.writePolicyDefault,TradeStoreServer.contractRecordASKeyForTicker(ticker),
                ListOperation.getByIndex(Constants.CONTRACT_PRICE_SUMMARY_BIN,0,ListReturnType.VALUE, CTX.mapKey(Value.get(price)),CTX.mapKey(Value.get(Constants.TIMESTAMP_FIELD_NAME))));
        return r.getLong(Constants.CONTRACT_PRICE_SUMMARY_BIN);
    }

}

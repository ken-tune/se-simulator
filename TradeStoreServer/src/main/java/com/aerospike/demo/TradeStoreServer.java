package com.aerospike.demo;

import com.aerospike.client.*;
import com.aerospike.client.cdt.*;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;

public class TradeStoreServer {
    AerospikeClient aerospikeClient;
    HttpServer httpServer;

    // Initialise TradeStore server
    public static void main(String[] args) throws IOException{
        TradeStoreServer s = new TradeStoreServer();
        s.startServer();
    }

    // Constructor
    public TradeStoreServer() throws IOException{
        httpServer = HttpServer.create(new InetSocketAddress(Constants.WEBSERVER_PORT), 0);
        aerospikeClient = new AerospikeClient(Constants.AEROSPIKE_HOST,3000);
    }

    // Start Server
    public void startServer(){
        httpServer.createContext("/", new RootHandler());
        httpServer.setExecutor(null);
        httpServer.start();
        System.out.println("Running on "+Constants.WEBSERVER_PORT);
    }

    // Handle trade messages
    public class RootHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            // Get trade message
            InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String inputString = br.readLine();
            br.close();
            // Set up JSON & output objects
            JsonNode jsonNode = null;
            OutputStream os = he.getResponseBody();
            // Parse as JSON
            try {
                jsonNode = convertStringToJsonNode(inputString);
                if (Constants.DEBUG) System.out.println(jsonNode.toString());
            }
            // Return exception if we can't parse JSON
            catch(Exception e){
                System.out.println("Can't parse "+inputString);
                he.sendResponseHeaders(400,0);
                os.write(("Can't parse "+inputString+"\n").getBytes());
                os.close();
                return;
            }
            try{
                saveTrade(jsonNode);
                he.sendResponseHeaders(200,0);
                os.write(("Trade " + inputString+ "saved\n").getBytes());
            }
            catch(ParseException e){
                System.out.println(e.toString());
                System.out.println(e.getMessage());
                he.sendResponseHeaders(400,0);
                os.write(("Incorrect JSON format\n").getBytes());
            }
            catch(Exception e){
                System.out.println(e.toString());
                System.out.println(e.getMessage());
                he.sendResponseHeaders(500,0);
                os.write(("System error\n").getBytes());
            }
            os.close();
        }
    }

    // Save trade. Will throw ParseException if JSON does not fit required schema
    void saveTrade(JsonNode jsonNode) throws ParseException{
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
        Key key = contractRecordASKeyForTrade(trade);
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
     * @param trade
     * @return
     */
    static Key contractRecordASKeyForTrade(JsonNode trade){
        String stringToHash = String.format("%s%f%d%d",trade.get(Constants.TICKER_FIELD_NAME).asText(),trade.get(Constants.PRICE_FIELD_NAME).asDouble(),
                trade.get(Constants.VOLUME_FIELD_NAME).asLong(),trade.get(Constants.TIMESTAMP_FIELD_NAME).asLong());

        // Hash the trade into a number between 0 & CONTRACT_RECORD_SHARD_COUNT -1
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        }
        catch(NoSuchAlgorithmException e){
            System.err.println(e.getMessage());
            System.exit(1);
        }
        md5.update(stringToHash.getBytes());
        BigInteger digest = new BigInteger(1,md5.digest());
        int shardNo = digest.mod(BigInteger.valueOf(Constants.CONTRACT_RECORD_SHARD_COUNT)).intValue();
        return new Key(Constants.AEROSPIKE_NAMESPACE,Constants.AEROSPIKE_CONTRACT_SUMMARY_SET,
                String.format("%s-%d",trade.get(Constants.TICKER_FIELD_NAME).asText(),shardNo));
    }

    static Key[] contractRecordASKeysForTicker(String ticker){
        Key[] keys = new Key[Constants.CONTRACT_RECORD_SHARD_COUNT];
        for(int shardNo=0;shardNo<Constants.CONTRACT_RECORD_SHARD_COUNT;shardNo++)
        keys[shardNo] = new Key(Constants.AEROSPIKE_NAMESPACE,Constants.AEROSPIKE_CONTRACT_SUMMARY_SET,
                String.format("%s-%d",ticker,shardNo));
        return keys;
    }

    /**
     * Get the aggregate volume traded for a given ticker
     * @param ticker
     * @return
     */
    public long getAggregateVolumeForTicker(String ticker){
        Record[] records = aerospikeClient.get(aerospikeClient.batchPolicyDefault,contractRecordASKeysForTicker(ticker));
        long volume = 0;
        for(int i=0;i<records.length;i++){
            if(records[i] != null) {
                volume += (Long) records[i].getMap(Constants.CONTRACT_RECORD_BIN).get(Constants.VOLUME_FIELD_NAME);
            }
        }
        return volume;
    }

    /**
     * Get the highest price recorded for a given ticker
     * @param ticker
     * @return
     */
    public double getHighestPriceTradedForTicker(String ticker){
        Record[] records = aerospikeClient.get(aerospikeClient.batchPolicyDefault,contractRecordASKeysForTicker(ticker));
        double maxPrice = 0;
        for(int i=0;i<records.length;i++){
            if(records[i] != null) {
                double candidateMaxPrice = (Double) ((List)records[i].getMap(Constants.CONTRACT_RECORD_BIN).get(Constants.PRICE_FIELD_NAME)).get(0);
                maxPrice = Math.max(candidateMaxPrice, maxPrice);
            }
        }
        return maxPrice;
    }

    /**
     * Get the most recent timestamp for a trade on a given ticker
     * @param ticker
     * @return
     */
    public long getMostRecentTradeTimestampForTicker(String ticker){
        Record[] records = aerospikeClient.get(aerospikeClient.batchPolicyDefault,contractRecordASKeysForTicker(ticker));
        long maxTimestamp = 0;
        for(int i=0;i<records.length;i++){
            if(records[i] != null) {
                long candidateMaxTimestamp = (Long) ((List)records[i].getMap(Constants.CONTRACT_RECORD_BIN).get(Constants.TIMESTAMP_FIELD_NAME)).get(0);
                maxTimestamp = Math.max(candidateMaxTimestamp, maxTimestamp);
            }
        }
        return maxTimestamp;
    }

    /**
     * Get the aggregate volume traded for a given ticker / price combination
     * @param ticker
     * @param price
     * @return
     */
    public long getAggregateVolumeForTickerAndPrice(String ticker,double price){
        Record[] records = aerospikeClient.get(aerospikeClient.batchPolicyDefault,contractRecordASKeysForTicker(ticker));
        long volume = 0;
        for(int i=0;i<records.length;i++) {
            if(records[i] != null) {
                Map<String, Object> contractPriceMap = (Map<String, Object>) records[i].getMap(Constants.CONTRACT_PRICE_SUMMARY_BIN).get(price);
                volume += (Long) contractPriceMap.get(Constants.VOLUME_FIELD_NAME);
            }
        }
        return volume;
    }

    /**
     * Get the most recent timestamp for trades on a given ticker at a specific price
     * @param ticker
     * @return
     */
    public long getMostRecentTradeTimestampForTickerAndPrice(String ticker,double price){
        Record[] records = aerospikeClient.get(aerospikeClient.batchPolicyDefault,contractRecordASKeysForTicker(ticker));
        long maxTimestamp = 0;
        for(int i=0;i<records.length;i++){
            if(records[i] != null) {
                Map<String, Object> contractPriceMap = (Map<String, Object>) records[i].getMap(Constants.CONTRACT_PRICE_SUMMARY_BIN).get(price);
                maxTimestamp = Math.max((Long) ((List)contractPriceMap.get(Constants.TIMESTAMP_FIELD_NAME)).get(0), maxTimestamp);
            }
        }
        return maxTimestamp;
    }
}

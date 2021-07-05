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
import java.net.InetSocketAddress;
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

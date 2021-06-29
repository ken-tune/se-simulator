package com.aerospike.demo;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
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
    private void saveTrade(JsonNode jsonNode) throws ParseException{
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
        Key key  = new Key(Constants.AEROSPIKE_NAMESPACE,Constants.AEROSPIKE_SET,u.toString());
        // Save
        aerospikeClient.put(new WritePolicy(),key,bins);
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
}

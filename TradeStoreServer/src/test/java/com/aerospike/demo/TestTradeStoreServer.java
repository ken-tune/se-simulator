package com.aerospike.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TestTradeStoreServer {
    private static TradeStoreServer tradeStoreServer;
    // Object mapper to create json nodes
    private static ObjectMapper objectMapper = new ObjectMapper();


    @BeforeAll
    public static void testSetup() throws IOException{
        Constants.AEROSPIKE_HOST = TestConstants.AEROSPIKE_HOST;
        tradeStoreServer = new TradeStoreServer();
    }

    @BeforeEach
    public void cleanData() throws IOException{
        tradeStoreServer.aerospikeClient.truncate(tradeStoreServer.aerospikeClient.infoPolicyDefault,Constants.TRADE_NAMESPACE,Constants.TRADE_SET,null);
        tradeStoreServer.aerospikeClient.truncate(tradeStoreServer.aerospikeClient.infoPolicyDefault,Constants.CONTRACT_SUMMARY_NAMESPACE,Constants.CONTRACT_SUMMARY_SET,null);

    }

    /**
     * check we are correctly keeping track of the aggregate volume for a given ticker
     */
    @Test
    public void checkVolumeAggregation() throws ParseException {
        int tradeVolume1 = 125343;
        int tradeVolume2 = 136332;
        // First Trade
        ObjectNode tradeNode = objectMapper.createObjectNode();
        tradeNode.put(Constants.TICKER_FIELD_NAME,TestConstants.TEST_TICKER);
        tradeNode.put(Constants.PRICE_FIELD_NAME,12.25);
        tradeNode.put(Constants.VOLUME_FIELD_NAME,tradeVolume1);
        tradeNode.put(Constants.TIMESTAMP_FIELD_NAME,System.currentTimeMillis());

        // Save Trade
        tradeStoreServer.saveTrade(tradeNode);

        // Second trade
        tradeNode.put(Constants.VOLUME_FIELD_NAME,tradeVolume2);

        // Save second trade
        tradeStoreServer.saveTrade(tradeNode);

        // Check aggregate trade volume is equal to the sum of the trade volumes
        Assertions.assertEquals(tradeStoreServer.getAggregateVolumeForTicker(TestConstants.TEST_TICKER),tradeVolume1 + tradeVolume2);

    }

    /**
     * check we are correctly keeping track of the best price for a given ticker
     */
    @Test
    public void checkBestPriceKept() throws ParseException {
        // Create trades with a variety of prices
        double[] prices = {12.0,11.9,12.1,12.2,11.8};
        // First Trade
        ObjectNode tradeNode = objectMapper.createObjectNode();
        tradeNode.put(Constants.TICKER_FIELD_NAME,TestConstants.TEST_TICKER);
        tradeNode.put(Constants.VOLUME_FIELD_NAME,12345);
        tradeNode.put(Constants.TIMESTAMP_FIELD_NAME,System.currentTimeMillis());

        double maxPrice = prices[0];
        // Incrementally save them
        for(int i=0;i<prices.length;i++){
            tradeNode.put(Constants.PRICE_FIELD_NAME,prices[i]);
            tradeStoreServer.saveTrade(tradeNode);
            // Keeping track of the max price so far
            maxPrice = Math.max(maxPrice,prices[i]);
        }

        // Ensure the max price in the db is as expected
        Assertions.assertEquals(tradeStoreServer.getHighestPriceTradedForTicker(TestConstants.TEST_TICKER),maxPrice);
    }

    /**
     * check we are correctly keeping track of the most recent timestamp for a given ticker
     */
    @Test
    public void checkMostRecentTimestampKept() throws ParseException {
        // Create trades with a variety of timestamps by using pre-prepared offsets
        int[] timestampOffsets = {4,2,-4,6,3,5};
        // First Trade
        ObjectNode tradeNode = objectMapper.createObjectNode();
        tradeNode.put(Constants.TICKER_FIELD_NAME,TestConstants.TEST_TICKER);
        tradeNode.put(Constants.VOLUME_FIELD_NAME,12345);
        tradeNode.put(Constants.PRICE_FIELD_NAME,12.1);

        long currentTime = System.currentTimeMillis();
        double maxTimestamp = currentTime + timestampOffsets[0];
        // Incrementally save them
        for(int i=0;i<timestampOffsets.length;i++){
            long timestamp = currentTime + timestampOffsets[i];
            tradeNode.put(Constants.TIMESTAMP_FIELD_NAME,timestamp);

            tradeStoreServer.saveTrade(tradeNode);
            // Keeping track of the max timestamp so far
            maxTimestamp = Math.max(maxTimestamp,timestamp);
        }

        // Ensure the recorded last timestamp is as expected
        Assertions.assertEquals(maxTimestamp,tradeStoreServer.getMostRecentTradeTimestampForTicker(TestConstants.TEST_TICKER));
    }

    @Test
    /**
     * Check the contract summary is correctly tracking timestamps, volumes and prices
     *
     * Do this by randomly creating trades and cross checking the results
     */
    public void manyTradesContractSummary() throws ParseException{
        // Keep track of volume, timestamps and max prices independently
        long volume = 0;
        long maxTimestamp = 0;
        double maxPrice = 0;
        // Randomly generate 1000 trades
        for(int i=0;i<1000;i++){
            // Random trade
            JsonNode tradeNode = generateRandomTrade();
            // Tracking volume, timestamp, max price
            volume = volume + tradeNode.get(Constants.VOLUME_FIELD_NAME).asLong();
            maxTimestamp = Math.max(maxTimestamp,tradeNode.get(Constants.TIMESTAMP_FIELD_NAME).asLong());
            maxPrice = Math.max(maxPrice,tradeNode.get(Constants.PRICE_FIELD_NAME).asDouble());
            // Saving trade to server
            tradeStoreServer.saveTrade(tradeNode);
        }
        // Check aggregate trade volume is equal to the sum of the trade volumes
        Assertions.assertEquals(tradeStoreServer.getAggregateVolumeForTicker(TestConstants.TEST_TICKER),volume);
        // Ensure the max price in the db is as expected
        Assertions.assertEquals(tradeStoreServer.getHighestPriceTradedForTicker(TestConstants.TEST_TICKER),maxPrice);
        // Ensure the recorded last timestamp is as expected
        Assertions.assertEquals(tradeStoreServer.getMostRecentTradeTimestampForTicker(TestConstants.TEST_TICKER),maxTimestamp);
    }

    @Test
    /**
     * Check the price contract summary is correctly tracking timestamps and volumes for each price
     *
     * Do this by randomly creating trades and cross checking the price contract summary
     */
    public void manyTradesPriceContractSummary() throws ParseException{
        // Keep track of priceContractSummary independently
        Map<Double,Map> contractPriceSummaryMap = new HashMap<Double,Map>();
        // Randomly generate 1000 trades
        for(int i=0;i<1000;i++){
            // Random trade
            JsonNode tradeNode = generateRandomTrade();
            if(contractPriceSummaryMap.get(tradeNode.get(Constants.PRICE_FIELD_NAME).asDouble()) == null) {
                Map<String,Object> summaryMap = new HashMap<String, Object>();
                summaryMap.put(Constants.VOLUME_FIELD_NAME,(long)0);
                summaryMap.put(Constants.TIMESTAMP_FIELD_NAME,(long)0);
                contractPriceSummaryMap.put(tradeNode.get(Constants.PRICE_FIELD_NAME).asDouble(), summaryMap);
            }
            contractPriceSummaryMap.get(tradeNode.get(Constants.PRICE_FIELD_NAME).asDouble()).put(Constants.VOLUME_FIELD_NAME,
                    (Long)(contractPriceSummaryMap.get(tradeNode.get(Constants.PRICE_FIELD_NAME).asDouble()).get(Constants.VOLUME_FIELD_NAME)) +
                        tradeNode.get(Constants.VOLUME_FIELD_NAME).asLong());
            contractPriceSummaryMap.get(tradeNode.get(Constants.PRICE_FIELD_NAME).asDouble()).put(Constants.TIMESTAMP_FIELD_NAME,
                    Math.max((Long)(contractPriceSummaryMap.get(tradeNode.get(Constants.PRICE_FIELD_NAME).asDouble()).get(Constants.TIMESTAMP_FIELD_NAME)),
                            tradeNode.get(Constants.TIMESTAMP_FIELD_NAME).asLong()));
            // Saving trade to server
            tradeStoreServer.saveTrade(tradeNode);
        }
        Iterator<Double> prices = contractPriceSummaryMap.keySet().iterator();
        while(prices.hasNext()){
            double price = prices.next();
            Map<String,Object> summaryMap = contractPriceSummaryMap.get(price);
            Assertions.assertEquals((long)summaryMap.get(Constants.VOLUME_FIELD_NAME),
                    tradeStoreServer.getAggregateVolumeForTickerAndPrice(TestConstants.TEST_TICKER,price));
            Assertions.assertEquals((long)summaryMap.get(Constants.TIMESTAMP_FIELD_NAME),
                    tradeStoreServer.getMostRecentTradeTimestampForTickerAndPrice(TestConstants.TEST_TICKER,price));
        }
    }

    @AfterEach
    public void cleanup(){
//        tradeStoreServer.aerospikeClient.truncate(tradeStoreServer.aerospikeClient.infoPolicyDefault,Constants.TRADE_NAMESPACE,Constants.TRADE_SET,null);
//        tradeStoreServer.aerospikeClient.truncate(tradeStoreServer.aerospikeClient.infoPolicyDefault,Constants.TRADE_NAMESPACE,Constants.CONTRACT_SUMMARY_SET,null);

    }

    private static JsonNode generateRandomTrade(){
        ObjectNode tradeNode = objectMapper.createObjectNode();
        long volume = 1000000 + (int)(Math.random() * 10000);
        double price = 100 + Math.floor(Math.random() * 100) ;
        long timestamp  = System.currentTimeMillis() + (int)(Math.random() * 1000);
        tradeNode.put(Constants.TICKER_FIELD_NAME,TestConstants.TEST_TICKER);
        tradeNode.put(Constants.VOLUME_FIELD_NAME,volume);
        tradeNode.put(Constants.PRICE_FIELD_NAME,price);
        tradeNode.put(Constants.TIMESTAMP_FIELD_NAME,timestamp);
        return tradeNode;
    }
}

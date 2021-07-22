package com.aerospike.demo.controllers;

import com.aerospike.demo.WebServer;
import com.aerospike.demo.TradeStoreServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.*;

import java.io.IOException;

public class TickerDataController {
    @OpenApi(
            summary = "Create Ticker data",
            operationId = "tickerDetails",
            path = "/tickerDetails",
            method = HttpMethod.POST,
            tags = {"Ticker"},
            requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = TickerData.class)}),
            responses = {
                    @OpenApiResponse(status = "200"),
                    @OpenApiResponse(status = "500", content = {@OpenApiContent(from = WebServer.ErrorResponse.class)})
            }
    )

    public static JsonNode convertStringToJsonNode(String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, JsonNode.class);
    }

    public static void create(Context ctx) {
        try {
            String data = ctx.body();
            TradeStoreServer server = new TradeStoreServer();
            JsonNode jsonNode = convertStringToJsonNode(data);
            server.saveTrade(jsonNode);
            ctx.status(200);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500);
        }
    }

    public static void get(Context ctx) {
        String ticker = ctx.pathParam("ticker");
        try {
            TradeStoreServer server = new TradeStoreServer();
            long value = server.getAggregateVolumeForTicker(ticker);
            ctx.json(value);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500);
        }

    }
}

final class TickerData {
    public String stockTicker;
    public int meanPrice;
    public double sqrtPriceVariance;
    public int minTradeVol;
    public int maxTradeVo1;
    public long timestamp;

}
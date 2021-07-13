package com.aerospike.demo.controllers;

import com.aerospike.demo.TradeStoreServer;
import io.javalin.http.Context;

public class TickerHighestPriceController {
    public static void get(Context ctx) {
        String ticker = ctx.pathParam("ticker");
        try {
            TradeStoreServer server = new TradeStoreServer();
            double value = server.getHighestPriceTradedForTicker(ticker);
            ctx.json(value);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500);
        }
    }
}

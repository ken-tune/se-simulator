package com.aerospike.demo.controllers;

import com.aerospike.demo.TradeStoreServer;
import io.javalin.http.Context;

public class TickerAggregateVolumeController {
    public static void get(Context ctx) {
        String ticker = ctx.pathParam("ticker");
        Double price = Double.parseDouble(ctx.pathParam("price"));
        try {
            TradeStoreServer server = new TradeStoreServer();
            long value = server.getAggregateVolumeForTickerAndPrice(ticker, price);
            ctx.json(value);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500);
        }

    }
}

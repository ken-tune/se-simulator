package com.aerospike.demo;
import com.aerospike.demo.controllers.TickerAggregateVolumeController;
import com.aerospike.demo.controllers.TickerDataController;
import com.aerospike.demo.controllers.TickerHighestPriceController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;
import io.javalin.plugin.openapi.annotations.*;
import org.eclipse.jetty.util.ajax.JSON;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.ui.ReDocOptions;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.swagger.v3.oas.models.info.Info;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static org.apache.http.client.methods.RequestBuilder.post;

public class WebServer {
    public static void main(String[] args) {
        ConcurrentLinkedDeque<SseClient> clients = new ConcurrentLinkedDeque<>();

        Javalin app = Javalin.create(config -> {
            config.registerPlugin(getConfiguredOpenApiPlugin());
            config.defaultContentType = "application/json";
            config.enableCorsForAllOrigins();
        });
        /* Place holder for Server side sent events
        app.sse("/aggregatePrice/:ticker", (client) -> {
            clients.add(client);
            client.sendEvent("connected" , "Hello SSE");
            client.onClose(new Runnable() {
                @Override
                public void run() {
                    clients.remove(client);
                }
            });
        }).start(7000);

        while (true) {
            for (SseClient client : clients) {
                System.out.println("Sending");
                try {
                    long value = new TradeStoreServer().getAggregateVolumeForTicker(client.ctx.pathParam("ticker"));
                    client.sendEvent(Long.toString(value) + ": " + System.currentTimeMillis());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */
        app.routes(() -> {
            path("tickerAggregatePrice", () -> {
                path(":ticker", () -> {
                    get(TickerDataController::get);
                });
            });
            path("tickerHighestPrice", () -> {
                path(":ticker", () -> {
                    get(TickerHighestPriceController::get);
                });
            });
        }).start("0.0.0.0", Constants.WEBSERVER_PORT);

    }

    private static OpenApiPlugin getConfiguredOpenApiPlugin() {
        Info info = new Info().version("1.0").description("User API");
        OpenApiOptions options = new OpenApiOptions(info)
                .activateAnnotationScanningFor("io.javalin.example.java")
                .path("/swagger-docs") // endpoint for OpenAPI json
                .swagger(new SwaggerOptions("/swagger-ui")) // endpoint for swagger-ui
                .reDoc(new ReDocOptions("/redoc")) // endpoint for redoc
                .defaultDocumentation(doc -> {
                    doc.json("500", ErrorResponse.class);
                    doc.json("200", Success.class);
                });
        return new OpenApiPlugin(options);
    }

    class Success {
        public int status;
        public String code;
    }
    public class ErrorResponse {
        public String title;
        public int status;

    }
}
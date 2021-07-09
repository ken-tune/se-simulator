package com.aerospike.demo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.*;
import org.eclipse.jetty.util.ajax.JSON;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.ui.ReDocOptions;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.swagger.v3.oas.models.info.Info;

import java.io.IOException;
import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static org.apache.http.client.methods.RequestBuilder.post;

public class TradeStoreReader {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
                    config.registerPlugin(getConfiguredOpenApiPlugin());
                    config.defaultContentType = "application/json";
        }).routes(() -> {
            path("tickerPrice", () -> {
                ApiBuilder.post(TickerDataController::create);
                path(":ticker", () -> {
                   get(TickerDataController::get);
                });
            });
        }).start(7000);

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
    class ErrorResponse {
        public String title;
        public int status;

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

class TickerDataController {
    @OpenApi(
            summary = "Create Ticker data",
            operationId = "tickerDetails",
            path = "/tickerDetails",
            method = HttpMethod.POST,
            tags = {"Ticker"},
            requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = TickerData.class)}),
            responses = {
                    @OpenApiResponse(status = "200"),
                    @OpenApiResponse(status = "500", content = {@OpenApiContent(from = TradeStoreReader.ErrorResponse.class)})
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

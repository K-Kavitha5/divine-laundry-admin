package com.divinelaundry.service;

import com.divinelaundry.config.WhatsappProviderProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappCloudApiClientTest {
    @Test
    void uploadsPngThenSendsApprovedImageTemplate() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<String> mediaRequest = new AtomicReference<>();
        AtomicReference<String> messageRequest = new AtomicReference<>();
        server.createContext("/v-test/123/media", exchange -> {
            mediaRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            respond(exchange, "{\"id\":\"media-1\"}");
        });
        server.createContext("/v-test/123/messages", exchange -> {
            messageRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, "{\"messages\":[{\"id\":\"wamid.1\"}]}");
        });
        server.start();
        try {
            WhatsappProviderProperties properties = new WhatsappProviderProperties(
                    true, "http://127.0.0.1:" + server.getAddress().getPort(), "v-test", "123",
                    "test-token", "laundry_invoice_payment", "en");
            WhatsappCloudApiClient client = new WhatsappCloudApiClient(properties);

            WhatsappCloudApiClient.DeliveryResult result = client.sendInvoiceAndPaymentImage(
                    "9876543210", new byte[]{1, 2, 3, 4}, "INV-1.png",
                    new WhatsappCloudApiClient.TemplateValues(
                            "Test Customer", "INV-1", "SO-1",
                            new BigDecimal("240.00"), BigDecimal.ZERO, new BigDecimal("240.00")));

            assertThat(result.mediaId()).isEqualTo("media-1");
            assertThat(result.providerMessageId()).isEqualTo("wamid.1");
            assertThat(mediaRequest.get()).contains("messaging_product", "image/png", "INV-1.png");
            assertThat(messageRequest.get()).contains(
                    "laundry_invoice_payment", "919876543210", "media-1",
                    "Test Customer", "INV-1", "240.00");
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}

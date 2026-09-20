package com.divinelaundry.service;

import com.divinelaundry.config.WhatsappProviderProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsappCloudApiClientTest {
    private static final WhatsappCloudApiClient.TemplateValues VALUES =
            new WhatsappCloudApiClient.TemplateValues("Test Customer", "INV-1", "SO-1",
                    new BigDecimal("240.00"), BigDecimal.ZERO, new BigDecimal("240.00"));

    @Test
    void classifiesProviderHttpFailuresWithoutPersistingResponseBodies() throws Exception {
        for (int status : new int[]{401, 403, 429, 400, 422, 500, 502, 503}) {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/v-test/123/media", exchange -> respond(exchange,
                    status, "provider-secret-body-token"));
            server.start();
            try {
                WhatsappCloudApiClient client = client(server, Duration.ofSeconds(1));
                assertThatThrownBy(() -> client.sendInvoiceAndPaymentImage(
                        "9876543210", new byte[]{1}, "INV-1.png", VALUES))
                        .isInstanceOf(WhatsappCloudApiClient.WhatsappProviderException.class)
                        .satisfies(error -> assertThat(((WhatsappCloudApiClient.WhatsappProviderException) error)
                                .classification()).isEqualTo(classificationFor(status)))
                        .hasMessageNotContaining("provider-secret-body-token")
                        .hasMessageNotContaining("Bearer");
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    void classifiesConnectionFailureAsNetworkFailure() {
        WhatsappProviderProperties properties = new WhatsappProviderProperties(
                true, "http://127.0.0.1:1", "v-test", "123", "token",
                "template", "en");
        WhatsappCloudApiClient client = new WhatsappCloudApiClient(properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(100)).build(), Duration.ofMillis(200));

        assertThatThrownBy(() -> client.sendInvoiceAndPaymentImage("9876543210", new byte[]{1}, "INV-1.png", VALUES))
                .isInstanceOf(WhatsappCloudApiClient.WhatsappProviderException.class)
                .satisfies(error -> assertThat(((WhatsappCloudApiClient.WhatsappProviderException) error)
                        .classification()).isEqualTo(WhatsappFailureClassification.NETWORK_FAILURE));
    }

    @Test
    void classifiesRequestTimeout() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v-test/123/media", exchange -> {
            try {
                Thread.sleep(500);
                respond(exchange, 200, "{\"id\":\"media-1\"}");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        });
        server.start();
        try {
            WhatsappCloudApiClient client = client(server, Duration.ofMillis(50));
            assertThatThrownBy(() -> client.sendInvoiceAndPaymentImage("9876543210", new byte[]{1}, "INV-1.png", VALUES))
                    .isInstanceOf(WhatsappCloudApiClient.WhatsappProviderException.class)
                    .satisfies(error -> assertThat(((WhatsappCloudApiClient.WhatsappProviderException) error)
                            .classification()).isEqualTo(WhatsappFailureClassification.TIMEOUT));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void classifiesMalformedSuccessfulResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v-test/123/media", exchange -> respond(exchange, 200, "{\"unexpected\":true}"));
        server.start();
        try {
            WhatsappCloudApiClient client = client(server, Duration.ofSeconds(1));
            assertThatThrownBy(() -> client.sendInvoiceAndPaymentImage("9876543210", new byte[]{1}, "INV-1.png", VALUES))
                    .isInstanceOf(WhatsappCloudApiClient.WhatsappProviderException.class)
                    .satisfies(error -> assertThat(((WhatsappCloudApiClient.WhatsappProviderException) error)
                            .classification()).isEqualTo(WhatsappFailureClassification.MALFORMED_PROVIDER_RESPONSE));
        } finally {
            server.stop(0);
        }
    }

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
                    "9876543210", new byte[]{1, 2, 3, 4}, "INV-1.png", VALUES);

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

    @Test
    void uploadsPdfAndSendsDocumentTemplateWithoutExposingCredentials() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<String> mediaRequest = new AtomicReference<>();
        AtomicReference<String> messageRequest = new AtomicReference<>();
        server.createContext("/v-test/123/media", exchange -> {
            mediaRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            respond(exchange, "{\"id\":\"media-pdf\"}");
        });
        server.createContext("/v-test/123/messages", exchange -> {
            messageRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, "{\"messages\":[{\"id\":\"wamid.pdf\"}]}");
        });
        server.start();
        try {
            String token = "token-never-in-request-body";
            WhatsappProviderProperties properties = new WhatsappProviderProperties(
                    true, "http://127.0.0.1:" + server.getAddress().getPort(), "v-test", "123",
                    token, "image_template", "en", "document_template", "en");
            WhatsappCloudApiClient client = new WhatsappCloudApiClient(properties);

            WhatsappCloudApiClient.DeliveryResult result = client.sendInvoiceAndPaymentDocument(
                    "9876543210", new byte[]{'%', 'P', 'D', 'F'}, "PAY-1.pdf",
                    new WhatsappCloudApiClient.TemplateValues(
                            "Receipt Customer", "PAY-1", "SO-1",
                            new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("600.00")));

            assertThat(result.mediaId()).isEqualTo("media-pdf");
            assertThat(result.providerMessageId()).isEqualTo("wamid.pdf");
            assertThat(mediaRequest.get()).contains("application/pdf", "PAY-1.pdf", "%PDF");
            assertThat(messageRequest.get()).contains("document_template", "\"type\":\"document\"", "media-pdf", "919876543210");
            assertThat(mediaRequest.get()).doesNotContain(token);
            assertThat(messageRequest.get()).doesNotContain(token);
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        respond(exchange, 200, body);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static WhatsappCloudApiClient client(HttpServer server, Duration timeout) {
        WhatsappProviderProperties properties = new WhatsappProviderProperties(
                true, "http://127.0.0.1:" + server.getAddress().getPort(), "v-test", "123",
                "test-token", "template", "en");
        return new WhatsappCloudApiClient(properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(), timeout);
    }

    private static WhatsappFailureClassification classificationFor(int status) {
        if (status == 401 || status == 403) return WhatsappFailureClassification.AUTHENTICATION_FAILURE;
        if (status == 429) return WhatsappFailureClassification.RATE_LIMITED;
        if (status < 500) return WhatsappFailureClassification.PROVIDER_CLIENT_ERROR;
        return WhatsappFailureClassification.PROVIDER_SERVER_ERROR;
    }
}

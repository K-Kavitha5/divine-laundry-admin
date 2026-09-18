package com.divinelaundry.service;

import com.divinelaundry.config.WhatsappProviderProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WhatsappCloudApiClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(35);
    private static final Pattern RESPONSE_ID = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final WhatsappProviderProperties properties;
    private final HttpClient http;

    public WhatsappCloudApiClient(WhatsappProviderProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public String configurationMessage() {
        return properties.configurationMessage();
    }

    public DeliveryResult sendInvoiceAndPaymentImage(
            String recipientPhone,
            byte[] png,
            String filename,
            TemplateValues values) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(properties.configurationMessage());
        }
        String mediaId = uploadPng(png, filename);
        String messageId = sendUtilityTemplate(normalizeIndianPhone(recipientPhone), mediaId, values);
        return new DeliveryResult(mediaId, messageId);
    }

    private String uploadPng(byte[] png, String filename) {
        String boundary = "----DivineLaundry" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = multipart(boundary, png, filename);
        HttpRequest request = request(properties.endpoint("media"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return responseId(execute(request, "WhatsApp media upload"), "WhatsApp media upload");
    }

    private String sendUtilityTemplate(String phone, String mediaId, TemplateValues values) {
        String payload = """
                {
                  "messaging_product":"whatsapp",
                  "recipient_type":"individual",
                  "to":%s,
                  "type":"template",
                  "template":{
                    "name":%s,
                    "language":{"code":%s},
                    "components":[
                      {"type":"header","parameters":[{"type":"image","image":{"id":%s}}]},
                      {"type":"body","parameters":[
                        {"type":"text","text":%s},
                        {"type":"text","text":%s},
                        {"type":"text","text":%s},
                        {"type":"text","text":%s},
                        {"type":"text","text":%s},
                        {"type":"text","text":%s}
                      ]}
                    ]
                  }
                }
                """.formatted(
                jsonString(phone),
                jsonString(properties.templateName()),
                jsonString(properties.templateLanguage()),
                jsonString(mediaId),
                jsonString(values.customerName()),
                jsonString(values.invoiceNumber()),
                jsonString(values.orderNumber()),
                jsonString(amount(values.total())),
                jsonString(amount(values.amountPaid())),
                jsonString(amount(values.balance())));
        HttpRequest request = request(properties.endpoint("messages"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return responseId(execute(request, "WhatsApp template send"), "WhatsApp template send");
    }

    private HttpRequest.Builder request(String endpoint) {
        return HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + properties.accessToken())
                .header("Accept", "application/json");
    }

    private String execute(HttpRequest request, String operation) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new WhatsappProviderException("%s failed (%d): %s".formatted(
                        operation, response.statusCode(), abbreviate(response.body())));
            }
            return response.body();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new WhatsappProviderException(operation + " was interrupted", error);
        } catch (IOException error) {
            throw new WhatsappProviderException(operation + " failed", error);
        }
    }

    private static byte[] multipart(String boundary, byte[] png, String filename) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            field(output, boundary, "messaging_product", "whatsapp");
            field(output, boundary, "type", "image/png");
            output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                    + safeFilename(filename) + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            output.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(png);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return output.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("Could not build WhatsApp media request", impossible);
        }
    }

    private static void field(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String responseId(String body, String operation) {
        Matcher matcher = RESPONSE_ID.matcher(body == null ? "" : body);
        if (!matcher.find() || matcher.group(1).isBlank()) {
            throw new WhatsappProviderException(operation + " did not return an ID");
        }
        return matcher.group(1);
    }

    private static String jsonString(String value) {
        String input = value == null ? "" : value;
        StringBuilder escaped = new StringBuilder(input.length() + 2).append('"');
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) escaped.append("\\u%04x".formatted((int) character));
                    else escaped.append(character);
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static String amount(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    static String normalizeIndianPhone(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() == 10) digits = "91" + digits;
        if (digits.length() == 11 && digits.startsWith("0")) digits = "91" + digits.substring(1);
        if (digits.length() < 11 || digits.length() > 15) {
            throw new IllegalArgumentException("Customer WhatsApp number must include a valid country code");
        }
        return digits;
    }

    private static String safeFilename(String filename) {
        return (filename == null ? "invoice.png" : filename).replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String abbreviate(String value) {
        if (value == null) return "No response body";
        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.length() <= 450 ? cleaned : cleaned.substring(0, 447) + "...";
    }

    public record TemplateValues(
            String customerName,
            String invoiceNumber,
            String orderNumber,
            BigDecimal total,
            BigDecimal amountPaid,
            BigDecimal balance) {}

    public record DeliveryResult(String mediaId, String providerMessageId) {}

    public static class WhatsappProviderException extends RuntimeException {
        public WhatsappProviderException(String message) { super(message); }
        public WhatsappProviderException(String message, Throwable cause) { super(message, cause); }
    }
}

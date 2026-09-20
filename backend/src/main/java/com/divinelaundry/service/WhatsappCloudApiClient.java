package com.divinelaundry.service;

import com.divinelaundry.config.WhatsappProviderProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WhatsappCloudApiClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(35);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern RESPONSE_ID = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final WhatsappProviderProperties properties;
    private final HttpClient http;
    private final Duration requestTimeout;

    @Autowired
    public WhatsappCloudApiClient(WhatsappProviderProperties properties) {
        this(properties, HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(), REQUEST_TIMEOUT);
    }

    WhatsappCloudApiClient(WhatsappProviderProperties properties, HttpClient http, Duration requestTimeout) {
        this.properties = properties;
        this.http = http;
        this.requestTimeout = requestTimeout;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public String configurationMessage() {
        return properties.configurationMessage();
    }

    public boolean isDocumentConfigured() {
        return properties.isDocumentConfigured();
    }

    public DeliveryResult sendInvoiceAndPaymentImage(
            String recipientPhone,
            byte[] png,
            String filename,
            TemplateValues values) {
        return sendMediaTemplate(recipientPhone,
                new WhatsAppMedia(png, filename, "image/png", WhatsAppMediaType.IMAGE),
                properties.templateName(), properties.templateLanguage(), values);
    }

    public DeliveryResult sendInvoiceAndPaymentDocument(
            String recipientPhone,
            byte[] pdf,
            String filename,
            TemplateValues values) {
        if (!properties.isDocumentConfigured()) {
            throw new IllegalStateException(properties.documentConfigurationMessage());
        }
        return sendMediaTemplate(recipientPhone,
                new WhatsAppMedia(pdf, filename, "application/pdf", WhatsAppMediaType.DOCUMENT),
                properties.documentTemplateName(), properties.documentTemplateLanguage(), values);
    }

    private DeliveryResult sendMediaTemplate(
            String recipientPhone,
            WhatsAppMedia media,
            String templateName,
            String templateLanguage,
            TemplateValues values) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(properties.configurationMessage());
        }
        String mediaId = upload(media);
        String messageId = sendUtilityTemplate(normalizeIndianPhone(recipientPhone), mediaId,
                media.mediaType(), templateName, templateLanguage, values);
        return new DeliveryResult(mediaId, messageId);
    }

    private String upload(WhatsAppMedia media) {
        String boundary = "----DivineLaundry" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = multipart(boundary, media);
        HttpRequest request = request(properties.endpoint("media"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return responseId(execute(request, "WhatsApp media upload"), "WhatsApp media upload");
    }

    private String sendUtilityTemplate(String phone, String mediaId, WhatsAppMediaType mediaType,
            String templateName, String templateLanguage, TemplateValues values) {
        String headerType = mediaType == WhatsAppMediaType.DOCUMENT ? "document" : "image";
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
                      {"type":"header","parameters":[{"type":"%s","%s":{"id":%s}}]},
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
                jsonString(templateName),
                jsonString(templateLanguage),
                headerType,
                headerType,
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
        try {
            return HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + properties.accessToken())
                .header("Accept", "application/json");
        } catch (IllegalArgumentException error) {
            throw new WhatsappProviderException(
                    WhatsappFailureClassification.CONFIGURATION_FAILURE,
                    "WhatsApp endpoint configuration is invalid", error);
        }
    }

    private String execute(HttpRequest request, String operation) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new WhatsappProviderException(classificationFor(response.statusCode()),
                        "%s failed with HTTP %d".formatted(operation, response.statusCode()));
            }
            return response.body();
        } catch (HttpTimeoutException error) {
            throw new WhatsappProviderException(WhatsappFailureClassification.TIMEOUT,
                    operation + " timed out", error);
        } catch (ConnectException error) {
            throw new WhatsappProviderException(WhatsappFailureClassification.NETWORK_FAILURE,
                    operation + " could not connect", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new WhatsappProviderException(WhatsappFailureClassification.NETWORK_FAILURE,
                    operation + " was interrupted", error);
        } catch (IOException error) {
            throw new WhatsappProviderException(WhatsappFailureClassification.NETWORK_FAILURE,
                    operation + " failed", error);
        }
    }

    private static byte[] multipart(String boundary, WhatsAppMedia media) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            field(output, boundary, "messaging_product", "whatsapp");
                field(output, boundary, "type", media.contentType());
            output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                    + safeFilename(media.filename()) + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(("Content-Type: " + media.contentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(media.bytes());
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
            throw new WhatsappProviderException(WhatsappFailureClassification.MALFORMED_PROVIDER_RESPONSE,
                    operation + " did not return a message ID");
        }
        return matcher.group(1);
    }

    private static WhatsappFailureClassification classificationFor(int statusCode) {
        if (statusCode == 401 || statusCode == 403) return WhatsappFailureClassification.AUTHENTICATION_FAILURE;
        if (statusCode == 429) return WhatsappFailureClassification.RATE_LIMITED;
        if (statusCode >= 400 && statusCode < 500) return WhatsappFailureClassification.PROVIDER_CLIENT_ERROR;
        if (statusCode >= 500) return WhatsappFailureClassification.PROVIDER_SERVER_ERROR;
        return WhatsappFailureClassification.UNKNOWN_FAILURE;
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
        if (digits.length() == 10 && validIndianLocalNumber(digits)) digits = "91" + digits;
        if (digits.length() == 11 && digits.startsWith("0") && validIndianLocalNumber(digits.substring(1))) {
            digits = "91" + digits.substring(1);
        }
        if (digits.length() == 12 && digits.startsWith("91") && validIndianLocalNumber(digits.substring(2))) {
            return digits;
        }
        if (digits.length() < 11 || digits.length() > 15
            || digits.length() == 10
            || (digits.length() == 11 && digits.startsWith("0"))
            || (digits.length() == 12 && digits.startsWith("91"))) {
            throw new IllegalArgumentException("Customer WhatsApp number must include a valid country code");
        }
        return digits;
    }

    private static boolean validIndianLocalNumber(String digits) {
        return digits.length() == 10 && digits.charAt(0) >= '6' && digits.charAt(0) <= '9';
    }

    private static String safeFilename(String filename) {
        return (filename == null ? "invoice.png" : filename).replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public record TemplateValues(
            String customerName,
            String invoiceNumber,
            String orderNumber,
            BigDecimal total,
            BigDecimal amountPaid,
            BigDecimal balance) {}

    public record DeliveryResult(String mediaId, String providerMessageId) {}

    public record WhatsAppMedia(byte[] bytes, String filename, String contentType, WhatsAppMediaType mediaType) {}

    public enum WhatsAppMediaType { IMAGE, DOCUMENT }

    public static class WhatsappProviderException extends RuntimeException {
        private final WhatsappFailureClassification classification;

        public WhatsappProviderException(WhatsappFailureClassification classification, String message) {
            super(message);
            this.classification = classification;
        }

        public WhatsappProviderException(WhatsappFailureClassification classification, String message, Throwable cause) {
            super(message, cause);
            this.classification = classification;
        }

        public WhatsappFailureClassification classification() {
            return classification;
        }
    }
}

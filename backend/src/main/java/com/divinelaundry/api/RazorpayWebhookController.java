package com.divinelaundry.api;

import com.divinelaundry.domain.PaymentRequest;
import com.divinelaundry.repository.PaymentRequestRepository;
import com.divinelaundry.service.PaymentRequestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class RazorpayWebhookController {
    private final PaymentRequestService paymentRequestService;
    private final PaymentRequestRepository paymentRequests;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public RazorpayWebhookController(
            PaymentRequestService paymentRequestService,
            PaymentRequestRepository paymentRequests,
            ObjectMapper objectMapper,
            @Value("${razorpay.webhook-secret:}") String webhookSecret) {
        this.paymentRequestService = paymentRequestService;
        this.paymentRequests = paymentRequests;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<?> handle(
            @RequestBody(required = false) byte[] payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) throws Exception {
        if (payload == null || payload.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("status", "missing_payload"));
        }
        if (!StringUtils.hasText(signature) || !verifySignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "invalid_signature"));
        }

        JsonNode root = objectMapper.readTree(payload);
        String event = root.path("event").asText(null);
        if (!"payment_link.paid".equalsIgnoreCase(event)) {
            return ResponseEntity.ok(Map.of("status", "ignored"));
        }

        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        if (paymentEntity.isMissingNode() || paymentEntity.isNull()) {
            return ResponseEntity.ok(Map.of("status", "ignored"));
        }

        String paymentStatus = paymentEntity.path("status").asText(null);
        if (!"paid".equalsIgnoreCase(paymentStatus)
                && !"captured".equalsIgnoreCase(paymentStatus)
                && !"success".equalsIgnoreCase(paymentStatus)) {
            return ResponseEntity.ok(Map.of("status", "ignored"));
        }

        JsonNode paymentLinkEntity = root.path("payload").path("payment_link").path("entity");
        String referenceId = paymentLinkEntity.path("reference_id").asText(null);
        if (!StringUtils.hasText(referenceId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "missing_reference"));
        }

        PaymentRequest request = paymentRequests.findByIdempotencyKey(referenceId).orElse(null);
        if (request == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "unknown_reference"));
        }

        BigDecimal amount = parseAmount(paymentEntity.path("amount"));
        String currency = paymentEntity.path("currency").asText(null);
        if (amount == null || !StringUtils.hasText(currency)) {
            amount = parseAmount(paymentLinkEntity.path("amount"));
            currency = paymentLinkEntity.path("currency").asText(null);
        }
        if (amount == null || !StringUtils.hasText(currency)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "missing_payment_details"));
        }
        if (amount.compareTo(request.getRequestedAmount()) != 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "amount_mismatch"));
        }
        if (!request.getCurrency().equalsIgnoreCase(currency)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "currency_mismatch"));
        }

        String providerPaymentId = paymentEntity.path("id").asText(null);
        if (StringUtils.hasText(providerPaymentId)) {
            var duplicate = paymentRequests.findByProviderPaymentId(providerPaymentId);
            if (duplicate.isPresent() && !duplicate.get().getId().equals(request.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "duplicate_payment"));
            }
            if (duplicate.isPresent() && duplicate.get().getId().equals(request.getId())) {
                return ResponseEntity.ok(Map.of("status", "accepted"));
            }
        }

        if (!StringUtils.hasText(request.getProviderReference())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "missing_provider_reference"));
        }

        paymentRequestService.confirmVerifiedPayment(
                request.getOrderNumber(),
                request.getProviderReference(),
                request.getRequestedAmount(),
                request.getCurrency(),
                "razorpay-webhook");

        return ResponseEntity.ok(Map.of("status", "accepted"));
    }

    static String computeSignature(byte[] payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            sb.append(String.format(Locale.ROOT, "%02x", value));
        }
        return sb.toString();
    }

    private boolean verifySignature(byte[] payload, String signature) {
        if (!StringUtils.hasText(webhookSecret) || !StringUtils.hasText(signature)) {
            return false;
        }
        try {
            String expected = computeSignature(payload, webhookSecret);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return false;
        }
    }

    private BigDecimal parseAmount(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        long paise = node.asLong(-1L);
        if (paise < 0) {
            return null;
        }
        return BigDecimal.valueOf(paise, 2);
    }
}

package com.divinelaundry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RazorpayPaymentProviderTest {

    private MockRestServiceServer server;
    private RazorpayPaymentProvider provider;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        provider = new RazorpayPaymentProvider(restTemplate, "https://api.razorpay.com", "rzp_test_123", "secret_123", new ObjectMapper());
    }

    @Test
    void createPaymentRequestUsesExactInrAmountAndReturnsShortUrl() throws Exception {
        String body = "{\"id\":\"plink_123\",\"amount\":100000,\"currency\":\"INR\",\"reference_id\":\"req-1000\",\"short_url\":\"https://razorpay.me/test\",\"accept_partial\":false,\"status\":\"created\"}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        PaymentProvider.ProviderPaymentResponse response = provider.createPaymentRequest(
                new PaymentProvider.PaymentRequestContext(
                        "SO-2026-000101",
                        "INV-2026-000101",
                        "Test Customer",
                        new BigDecimal("1000.00"),
                        "INR",
                        "admin",
                        "req-1000"));

        assertThat(response.provider()).isEqualTo("razorpay");
        assertThat(response.providerReference()).isEqualTo("plink_123");
        assertThat(response.paymentUrl()).isEqualTo("https://razorpay.me/test");
        assertThat(response.requestedAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.expiresAt()).isNotNull();
        assertThat(response.expiresAt().isAfter(Instant.now().plusSeconds(900))).isTrue();
    }

    @Test
    void createPaymentRequestIncludesExpireByFarInTheFuture() throws Exception {
        String body = "{\"id\":\"plink_456\",\"amount\":100000,\"currency\":\"INR\",\"reference_id\":\"req-2000\",\"short_url\":\"https://razorpay.me/future\",\"accept_partial\":false,\"status\":\"created\"}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links"))
                .andExpect(method(HttpMethod.POST))
                .andRespond((ClientHttpRequest request) -> {
                    String payload = ((MockClientHttpRequest) request).getBodyAsString();
                    JsonNode node = new ObjectMapper().readTree(payload);
                    long expireBy = node.path("expire_by").asLong(-1L);
                    assertThat(node.has("expire_by")).isTrue();
                    assertThat(expireBy).isPositive();
                    assertThat(expireBy).isGreaterThan(Instant.now().plusSeconds(900).getEpochSecond());
                    return new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), org.springframework.http.HttpStatus.OK);
                });

        PaymentProvider.ProviderPaymentResponse response = provider.createPaymentRequest(
                new PaymentProvider.PaymentRequestContext(
                        "SO-2026-000102",
                        "INV-2026-000102",
                        "Future Customer",
                        new BigDecimal("1000.00"),
                        "INR",
                        "admin",
                        "req-2000"));

        assertThat(response.expiresAt()).isNotNull();
        assertThat(response.expiresAt().getEpochSecond()).isGreaterThan(Instant.now().plusSeconds(900).getEpochSecond());
    }

    @Test
    void verifyPaymentSucceedsWhenPaymentLinkPaymentsHaveNoCurrencyAndPaymentIsCaptured() {
        String linkBody = "{\"id\":\"plink_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"paid\",\"payments\":[{\"payment_id\":\"pay_123\",\"plink_id\":\"plink_123\",\"amount\":100000,\"status\":\"captured\"}]}";
        String paymentBody = "{\"id\":\"pay_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"captured\",\"captured\":true}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links/plink_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(linkBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(paymentBody, MediaType.APPLICATION_JSON));

        PaymentProvider.ProviderPaymentResponse response = provider.verifyPayment("plink_123", new BigDecimal("1000.00"), "INR");

        assertThat(response.status()).isEqualTo(com.divinelaundry.domain.PaymentRequestStatus.PAID);
        assertThat(response.providerPaymentId()).isEqualTo("pay_123");
        assertThat(response.providerReference()).isEqualTo("plink_123");
    }

    @Test
    void verifyPaymentRejectsWrongLinkCurrency() {
        String linkBody = "{\"id\":\"plink_123\",\"amount\":100000,\"currency\":\"USD\",\"status\":\"paid\",\"payments\":[{\"payment_id\":\"pay_123\",\"plink_id\":\"plink_123\",\"amount\":100000,\"status\":\"captured\"}]}";
        String paymentBody = "{\"id\":\"pay_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"captured\",\"captured\":true}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links/plink_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(linkBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(paymentBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.verifyPayment("plink_123", new BigDecimal("1000.00"), "INR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void verifyPaymentRejectsLinkNotPaid() {
        String linkBody = "{\"id\":\"plink_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"created\",\"payments\":[{\"payment_id\":\"pay_123\",\"plink_id\":\"plink_123\",\"amount\":100000,\"status\":\"captured\"}]}";
        String paymentBody = "{\"id\":\"pay_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"captured\",\"captured\":true}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links/plink_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(linkBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(paymentBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.verifyPayment("plink_123", new BigDecimal("1000.00"), "INR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment link status");
    }

    @Test
    void verifyPaymentRejectsWrongCapturedPaymentCurrency() {
        String linkBody = "{\"id\":\"plink_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"paid\",\"payments\":[{\"payment_id\":\"pay_123\",\"plink_id\":\"plink_123\",\"amount\":100000,\"status\":\"captured\"}]}";
        String paymentBody = "{\"id\":\"pay_123\",\"amount\":100000,\"currency\":\"USD\",\"status\":\"captured\",\"captured\":true}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links/plink_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(linkBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(paymentBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.verifyPayment("plink_123", new BigDecimal("1000.00"), "INR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void verifyPaymentRejectsWrongCapturedPaymentAmount() {
        String linkBody = "{\"id\":\"plink_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"paid\",\"payments\":[{\"payment_id\":\"pay_123\",\"plink_id\":\"plink_123\",\"amount\":100000,\"status\":\"captured\"}]}";
        String paymentBody = "{\"id\":\"pay_123\",\"amount\":100001,\"currency\":\"INR\",\"status\":\"captured\",\"captured\":true}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links/plink_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(linkBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(paymentBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.verifyPayment("plink_123", new BigDecimal("1000.00"), "INR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Amount mismatch");
    }

    @Test
    void verifyPaymentRejectsCapturedPaymentNotCaptured() {
        String linkBody = "{\"id\":\"plink_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"paid\",\"payments\":[{\"payment_id\":\"pay_123\",\"plink_id\":\"plink_123\",\"amount\":100000,\"status\":\"paid\"}]}";
        String paymentBody = "{\"id\":\"pay_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"paid\",\"captured\":false}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links/plink_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(linkBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(paymentBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.verifyPayment("plink_123", new BigDecimal("1000.00"), "INR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not captured");
    }

    @Test
    void verifyPaymentRejectsMismatchedCapturedPaymentLinkId() {
        String linkBody = "{\"id\":\"plink_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"paid\",\"payments\":[{\"payment_id\":\"pay_123\",\"plink_id\":\"plink_999\",\"amount\":100000,\"status\":\"captured\"}]}";
        String paymentBody = "{\"id\":\"pay_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"captured\",\"captured\":true}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links/plink_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(linkBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(paymentBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.verifyPayment("plink_123", new BigDecimal("1000.00"), "INR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different Razorpay payment link");
    }

    @Test
    void verifyPaymentRejectsPaymentIdMismatch() {
        String linkBody = "{\"id\":\"plink_123\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"paid\",\"payments\":[{\"payment_id\":\"pay_123\",\"plink_id\":\"plink_123\",\"amount\":100000,\"status\":\"captured\"}]}";
        String paymentBody = "{\"id\":\"pay_999\",\"amount\":100000,\"currency\":\"INR\",\"status\":\"captured\",\"captured\":true}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links/plink_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(linkBody, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(paymentBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.verifyPayment("plink_123", new BigDecimal("1000.00"), "INR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("payment ID");
    }

    @Test
    void conversionToPaiseUsesHundredTimesRupeesForExactLinkCreation() {
        String body = "{\"id\":\"plink_999\",\"amount\":72500,\"currency\":\"INR\",\"reference_id\":\"req-725\",\"short_url\":\"https://razorpay.me/725\",\"accept_partial\":false,\"status\":\"created\"}";
        server.expect(requestTo("https://api.razorpay.com/v1/payment_links"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        PaymentProvider.ProviderPaymentResponse response = provider.createPaymentRequest(
                new PaymentProvider.PaymentRequestContext(
                        "SO-2026-000102",
                        "INV-2026-000102",
                        "Another Customer",
                        new BigDecimal("725.00"),
                        "INR",
                        "admin",
                        "req-725"));

        assertThat(response.requestedAmount()).isEqualByComparingTo("725.00");
        assertThat(response.paymentUrl()).isEqualTo("https://razorpay.me/725");
    }
}

package com.divinelaundry.web;

import com.divinelaundry.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:webtests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "app.admin.username=admin",
        "app.admin.password=TestPassword123!", "app.whatsapp.enabled=false", "app.payment.upi-id="})
class AdminWebFlowTest {
    @Autowired WebApplicationContext context;
    @Autowired CustomerRepository customers;
    @Autowired LaundryOrderRepository orders;
    @Autowired LaundryServiceRepository catalog;
    MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test void loginAndCsrfAreEnforced() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection());
                mvc.perform(get("/api/documents/orders/SO-2026-000001")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
        mvc.perform(post("/customers").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        var result = mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "TestPassword123!"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login?logout"));
    }

    @Test void customerOrderInvoiceAndPaymentFlowUsesDatabase() throws Exception {
        String phone = "9" + String.format("%09d", Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 1000000000L));
        long customersBefore = customers.count();
        mvc.perform(post("/customers").with(user("admin").roles("ADMIN")).with(csrf())
                .param("name", "Test Customer").param("phone", "+91 " + phone).param("area", "Trichy").param("addressLine", "Test street"))
                .andExpect(status().is3xxRedirection());
        assertThat(customers.count()).isEqualTo(customersBefore + 1);
        var customer = customers.findByPhone(phone).orElseThrow();
        mvc.perform(post("/customers").with(user("admin").roles("ADMIN")).with(csrf())
                .param("name", "Duplicate").param("phone", phone)).andExpect(view().name("customer-form"))
                .andExpect(model().attributeHasErrors("customerForm"));
        var service = catalog.findByActiveTrueOrderByCategoryAscNameAsc().stream()
                .filter(s -> s.getCode().equals("SHIRT_IRON")).findFirst().orElseThrow();
        String requestId = UUID.randomUUID().toString();
        long before = orders.count();
        String redirect = createOrder(customer.getId(), service.getId(), requestId);
        assertThat(createOrder(customer.getId(), service.getId(), requestId)).isEqualTo(redirect);
        assertThat(orders.count()).isEqualTo(before + 1);
        String number = redirect.substring("/orders/".length());
        var order = orders.findByOrderNumber(number).orElseThrow();
        assertThat(order.getTotal()).isEqualByComparingTo("28.00");
        assertThat(order.getInvoiceNumber()).isNotBlank();
        mvc.perform(get(redirect).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get(redirect + "/invoice").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(order.getInvoiceNumber())));
        mvc.perform(get(redirect + "/invoice.png").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mvc.perform(post(redirect + "/payments").with(user("admin").roles("ADMIN")).with(csrf())
                .param("requestId", UUID.randomUUID().toString()).param("amount", "29.00").param("mode", "CASH"))
                .andExpect(model().attributeHasErrors("paymentForm"));
        String paymentId = UUID.randomUUID().toString();
        for (int i = 0; i < 2; i++) mvc.perform(post(redirect + "/payments").with(user("admin").roles("ADMIN")).with(csrf())
                .param("requestId", paymentId).param("amount", "28.00").param("mode", "CASH"))
                .andExpect(redirectedUrl(redirect));
        assertThat(orders.findByOrderNumber(number).orElseThrow().getPaymentStatus().name()).isEqualTo("PAID");
        assertThat(createOrder(customer.getId(), service.getId(), UUID.randomUUID().toString())).isNotEqualTo(redirect);
        assertThat(orders.count()).isEqualTo(before + 2);
    }

    @Test void restOrderUsesAuthenticatedActorAndCustomerPhoneNormalization() throws Exception {
        String digits = "9" + String.format("%09d", Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 1000000000L));
        mvc.perform(post("/api/customers").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(("{\"name\":\"REST Customer\",\"phone\":\"+91 %s\",\"addressLine\":\"Test\",\"area\":\"Trichy\"}").formatted(digits)))
                .andExpect(status().isCreated());
        assertThat(customers.findByPhone(digits)).isPresent();

        var customer = customers.findByPhone(digits).orElseThrow();
        var service = catalog.findByActiveTrueOrderByCategoryAscNameAsc().stream()
                .filter(item -> item.getCode().equals("SHIRT_IRON")).findFirst().orElseThrow();
        String requestId = UUID.randomUUID().toString();
        String body = ("{\"clientRequestId\":\"%s\",\"customerId\":%d,\"deliveryAt\":\"2027-01-15T18:00:00Z\",\"notes\":\"REST\",\"createdBy\":\"spoofed\",\"discount\":0,\"tax\":0,\"items\":[{\"serviceId\":%d,\"billableQuantity\":2,\"pieceCount\":2,\"noPrint\":false}]}")
                .formatted(requestId, customer.getId(), service.getId());
        var result = mvc.perform(post("/api/orders").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        String orderNumber = result.getResponse().getContentAsString()
                .replaceAll(".*\\\"orderNumber\\\":\\\"([^\\\"]+)\\\".*", "$1");
        assertThat(orders.findByOrderNumber(orderNumber).orElseThrow().getCreatedBy()).isEqualTo("admin");
    }

    private String createOrder(Long customerId, Long serviceId, String requestId) throws Exception {
        return mvc.perform(post("/orders").with(user("admin").roles("ADMIN")).with(csrf())
                .param("requestId", requestId).param("customerId", customerId.toString()).param("deliveryAt", "2027-01-15T18:00")
                .param("items[0].serviceId", serviceId.toString()).param("items[0].quantity", "2").param("items[0].pieces", "2")
                .param("discount", "0").param("tax", "0"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
    }
}

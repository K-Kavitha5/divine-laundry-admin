package com.divinelaundry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPaymentProviderBeanTest {

    @Test
    void defaultProfileUsesProductionProvider() {
        assertThat(ProductionPaymentProvider.class.getAnnotation(Profile.class).value()).contains("!test");
        assertThat(MockPaymentProvider.class.getAnnotation(Profile.class).value()).contains("test");
    }

    @Test
    void razorpayProviderIsDisabledUntilConfigured() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("provider-test", Map.of("razorpay.enabled", "false")));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment);
            context.register(TestConfig.class, ProductionPaymentProvider.class, RazorpayPaymentProvider.class, MockPaymentProvider.class);
            context.refresh();

            assertThat(context.getBeanProvider(ProductionPaymentProvider.class).getIfAvailable()).isNotNull();
            assertThat(context.getBeanProvider(RazorpayPaymentProvider.class).getIfAvailable()).isNull();
            assertThat(context.getBeanProvider(MockPaymentProvider.class).getIfAvailable()).isNull();
        }
    }

    @Test
    void razorpayProviderIsSelectedWhenEnabled() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("provider-test", Map.of("razorpay.enabled", "true")));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment);
            context.register(TestConfig.class, ProductionPaymentProvider.class, RazorpayPaymentProvider.class, MockPaymentProvider.class);
            context.refresh();

            assertThat(context.getBeanProvider(RazorpayPaymentProvider.class).getIfAvailable()).isNotNull();
            assertThat(context.getBeanProvider(ProductionPaymentProvider.class).getIfAvailable()).isNull();
            assertThat(context.getBeanProvider(MockPaymentProvider.class).getIfAvailable()).isNull();
        }
    }

    @Configuration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}

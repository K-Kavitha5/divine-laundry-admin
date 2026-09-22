package com.divinelaundry.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.StandardEnvironment;

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
        environment.setActiveProfiles("default");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment);
            context.register(ProductionPaymentProvider.class, MockPaymentProvider.class);
            context.refresh();
            assertThat(context.getBeanProvider(ProductionPaymentProvider.class).getIfAvailable()).isNotNull();
            assertThat(context.getBeanProvider(MockPaymentProvider.class).getIfAvailable()).isNull();
        }
    }

    @Configuration
    static class TestConfig {}
}

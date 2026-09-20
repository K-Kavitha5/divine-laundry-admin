package com.divinelaundry.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappProviderPropertiesTest {
    @Test
    void disabledModeDoesNotRequireCredentials() {
        WhatsappProviderProperties properties = new WhatsappProviderProperties(
                false, "", "", "", "", "", "", "", "");

        assertThat(properties.isConfigured()).isFalse();
        assertThat(properties.configurationMessage()).isEqualTo("WhatsApp automatic sending is disabled");
        assertThat(properties.configurationState()).isEqualTo("DISABLED");
    }

    @Test
    void enabledModeRejectsInvalidUrlAndPhoneIdSafely() {
        WhatsappProviderProperties properties = new WhatsappProviderProperties(
                true, "not-a-url?token=secret", "v1", "123/456", "token-value", "image", "en",
                "document", "en");

        assertThat(properties.isConfigured()).isFalse();
        assertThat(properties.configurationMessage()).isEqualTo("WHATSAPP_GRAPH_BASE_URL must be an HTTP(S) URL");
        assertThat(properties.configurationMessage()).doesNotContain("token-value", "secret");
    }

    @Test
    void surroundingWhitespaceIsTrimmedFromConfigurationValues() {
        WhatsappProviderProperties properties = new WhatsappProviderProperties(
                true, " https://graph.example ", " v1 ", " 123 ", " token ", " image ", " en ",
                " document ", " en ");

        assertThat(properties.isConfigured()).isTrue();
        assertThat(properties.endpoint("media")).isEqualTo("https://graph.example/v1/123/media");
    }
}

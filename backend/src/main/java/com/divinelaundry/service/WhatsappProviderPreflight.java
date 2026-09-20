package com.divinelaundry.service;

import com.divinelaundry.config.WhatsappProviderProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class WhatsappProviderPreflight {
    private final WhatsappProviderProperties properties;
    private final String pendingTimeout;

    public WhatsappProviderPreflight(
            WhatsappProviderProperties properties,
            @Value("${app.whatsapp.pending-timeout:PT15M}") String pendingTimeout) {
        this.properties = properties;
        this.pendingTimeout = pendingTimeout;
    }

    public PreflightResult validate() {
        if (!properties.enabled()) return new PreflightResult(true, "DISABLED", null);
        if (!properties.isConfigured()) return new PreflightResult(false, "INCOMPLETE", properties.configurationMessage());
        if (!properties.isDocumentConfigured()) {
            return new PreflightResult(false, "INCOMPLETE", properties.documentConfigurationMessage());
        }
        try {
            Duration timeout = WhatsappMessageClaimService.parsePendingTimeout(pendingTimeout);
            return new PreflightResult(true, "READY", "Pending timeout " + timeout);
        } catch (IllegalArgumentException error) {
            return new PreflightResult(false, "INCOMPLETE", "WHATSAPP_PENDING_TIMEOUT is invalid");
        }
    }

    public record PreflightResult(boolean valid, String state, String message) {}
}
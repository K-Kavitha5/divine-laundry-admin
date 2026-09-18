package com.divinelaundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsappProviderProperties(
        boolean enabled,
        String graphBaseUrl,
        String graphApiVersion,
        String phoneNumberId,
        String accessToken,
        String templateName,
        String templateLanguage) {

    public boolean isConfigured() {
        return enabled
                && hasText(graphBaseUrl)
                && hasText(graphApiVersion)
                && hasText(phoneNumberId)
                && hasText(accessToken)
                && hasText(templateName)
                && hasText(templateLanguage);
    }

    public String configurationMessage() {
        if (!enabled) return "WhatsApp automatic sending is disabled";
        if (!hasText(graphApiVersion)) return "WHATSAPP_GRAPH_API_VERSION is required";
        if (!hasText(phoneNumberId)) return "WHATSAPP_PHONE_NUMBER_ID is required";
        if (!hasText(accessToken)) return "WHATSAPP_ACCESS_TOKEN is required";
        if (!hasText(templateName)) return "WHATSAPP_TEMPLATE_NAME is required";
        if (!hasText(templateLanguage)) return "WHATSAPP_TEMPLATE_LANGUAGE is required";
        return "WhatsApp provider configuration is incomplete";
    }

    public String endpoint(String resource) {
        String base = graphBaseUrl == null ? "" : graphBaseUrl.replaceAll("/+$", "");
        String version = graphApiVersion == null ? "" : graphApiVersion.replaceAll("^/+|/+$", "");
        return "%s/%s/%s/%s".formatted(base, version, phoneNumberId, resource);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

package com.divinelaundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsappProviderProperties(
        boolean enabled,
        String graphBaseUrl,
        String graphApiVersion,
        String phoneNumberId,
        String accessToken,
        String templateName,
        String templateLanguage,
        String documentTemplateName,
        String documentTemplateLanguage) {

    @ConstructorBinding
    public WhatsappProviderProperties {
    }

    public WhatsappProviderProperties(
            boolean enabled,
            String graphBaseUrl,
            String graphApiVersion,
            String phoneNumberId,
            String accessToken,
            String templateName,
            String templateLanguage) {
        this(enabled, graphBaseUrl, graphApiVersion, phoneNumberId, accessToken,
                templateName, templateLanguage, "", templateLanguage);
    }

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

    public String documentConfigurationMessage() {
        if (!enabled) return "WhatsApp automatic sending is disabled";
        if (!hasText(documentTemplateName)) return "WHATSAPP_DOCUMENT_TEMPLATE_NAME is required";
        if (!hasText(documentTemplateLanguage)) return "WHATSAPP_DOCUMENT_TEMPLATE_LANGUAGE is required";
        if (!isConfigured()) return configurationMessage();
        return "WhatsApp document provider configuration is incomplete";
    }

    public boolean isDocumentConfigured() {
        return isConfigured() && hasText(documentTemplateName) && hasText(documentTemplateLanguage);
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

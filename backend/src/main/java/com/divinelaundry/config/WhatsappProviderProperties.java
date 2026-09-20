package com.divinelaundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.net.URI;

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
        graphBaseUrl = trim(graphBaseUrl);
        graphApiVersion = trim(graphApiVersion);
        phoneNumberId = trim(phoneNumberId);
        accessToken = trim(accessToken);
        templateName = trim(templateName);
        templateLanguage = trim(templateLanguage);
        documentTemplateName = trim(documentTemplateName);
        documentTemplateLanguage = trim(documentTemplateLanguage);
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
                && validGraphBaseUrl()
                && hasText(graphApiVersion)
                && validPhoneNumberId()
                && hasText(accessToken)
                && hasText(templateName)
                && hasText(templateLanguage);
    }

    public String configurationMessage() {
        if (!enabled) return "WhatsApp automatic sending is disabled";
        if (!validGraphBaseUrl()) return "WHATSAPP_GRAPH_BASE_URL must be an HTTP(S) URL";
        if (!hasText(graphApiVersion)) return "WHATSAPP_GRAPH_API_VERSION is required";
        if (!validPhoneNumberId()) return "WHATSAPP_PHONE_NUMBER_ID is invalid";
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

    public String configurationState() {
        if (!enabled) return "DISABLED";
        return isConfigured() && isDocumentConfigured() ? "ENABLED_CONFIGURED" : "ENABLED_CONFIGURATION_INCOMPLETE";
    }

    private boolean validGraphBaseUrl() {
        if (!hasText(graphBaseUrl)) return false;
        try {
            URI uri = URI.create(graphBaseUrl);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && hasText(uri.getHost()) && uri.getUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private boolean validPhoneNumberId() {
        return hasText(phoneNumberId) && phoneNumberId.matches("[A-Za-z0-9_-]+");
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}

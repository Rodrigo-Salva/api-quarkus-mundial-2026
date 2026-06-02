package com.mundial2026.predictions.ai.provider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Selecciona el proveedor de IA según la propiedad:
 *   ai.provider=gemini   → Google Gemini
 *   ai.provider=chatgpt  → OpenAI ChatGPT
 *   ai.provider=claude   → Anthropic Claude
 *   ai.provider=grok     → xAI Grok
 *
 * Cambiar de modelo = cambiar UNA línea en application.properties.
 */
@ApplicationScoped
public class AiProviderSelector {

    @ConfigProperty(name = "ai.provider", defaultValue = "gemini")
    String activeProvider;

    @Inject GeminiProvider  gemini;
    @Inject ChatGptProvider chatGpt;
    @Inject ClaudeProvider  claude;
    @Inject GrokProvider    grok;

    public AiProvider get() {
        return switch (activeProvider.toLowerCase().trim()) {
            case "chatgpt", "openai" -> chatGpt;
            case "claude", "anthropic" -> claude;
            case "grok", "xai" -> grok;
            default -> gemini;
        };
    }
}

package com.mundial2026.predictions.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ClaudeProvider implements AiProvider {

    @ConfigProperty(name = "ai.claude.api-key", defaultValue = "")
    String apiKey;

    @ConfigProperty(name = "ai.claude.model", defaultValue = "claude-haiku-4-5-20251001")
    String model;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String complete(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 500,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            var json = objectMapper.readTree(response.body());
            return json.at("/content/0/text").asText("No response from Claude");
        } catch (Exception e) {
            return "Claude error: " + e.getMessage();
        }
    }

    @Override
    public String providerName() { return "claude"; }
}

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
public class ChatGptProvider implements AiProvider {

    @ConfigProperty(name = "ai.chatgpt.api-key", defaultValue = "")
    String apiKey;

    @ConfigProperty(name = "ai.chatgpt.model", defaultValue = "gpt-4o-mini")
    String model;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String complete(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 500
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            var json = objectMapper.readTree(response.body());
            return json.at("/choices/0/message/content").asText("No response from ChatGPT");
        } catch (Exception e) {
            return "ChatGPT error: " + e.getMessage();
        }
    }

    @Override
    public String providerName() { return "chatgpt"; }
}

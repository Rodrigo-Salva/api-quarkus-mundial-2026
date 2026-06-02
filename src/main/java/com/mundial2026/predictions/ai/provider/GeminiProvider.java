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
public class GeminiProvider implements AiProvider {

    @ConfigProperty(name = "ai.gemini.api-key", defaultValue = "")
    String apiKey;

    @ConfigProperty(name = "ai.gemini.model", defaultValue = "gemini-1.5-flash")
    String model;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String complete(String prompt) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    ))
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            var json = objectMapper.readTree(response.body());
            return json.at("/candidates/0/content/parts/0/text").asText("No response from Gemini");
        } catch (Exception e) {
            return "Gemini error: " + e.getMessage();
        }
    }

    @Override
    public String providerName() { return "gemini"; }
}

package com.fitness.aiservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.service.AiResponseException.Reason;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/** Calls the Gemini generateContent endpoint and returns the first candidate's text. */
@Service
public class GeminiService implements TextGenerator {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String geminiApiUrl;
    private final String geminiApiKey;
    private final Duration timeout;

    public GeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                         @Value("${gemini.api.url:}") String geminiApiUrl,
                         @Value("${gemini.api.key:}") String geminiApiKey,
                         @Value("${gemini.api.timeout:20s}") Duration timeout) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.geminiApiUrl = geminiApiUrl;
        this.geminiApiKey = geminiApiKey;
        this.timeout = timeout;
    }

    @Override
    public String generate(String prompt) {
        if (geminiApiUrl.isBlank() || geminiApiKey.isBlank()) {
            throw new AiResponseException(Reason.NOT_CONFIGURED, "GEMINI_URL or GEMINI_KEY is not set");
        }
        Map<String, Object> requestBody = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        String body;
        try {
            body = webClient.post()
                    .uri(geminiApiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-goog-api-key", geminiApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout)
                    .block();
        } catch (WebClientResponseException e) {
            throw new AiResponseException(Reason.MODEL_ERROR, "Gemini returned HTTP " + e.getStatusCode().value());
        } catch (WebClientRequestException e) {
            throw new AiResponseException(Reason.UNAVAILABLE, "Gemini request failed: " + e.getClass().getSimpleName());
        } catch (RuntimeException e) {
            if (Exceptions.unwrap(e) instanceof TimeoutException) {
                throw new AiResponseException(Reason.TIMEOUT, "no reply from Gemini within " + timeout.toSeconds() + " s");
            }
            throw e;
        }
        return firstCandidateText(body);
    }

    /** The text of the first candidate's first part. Package-private for tests. */
    String firstCandidateText(String body) {
        JsonNode root;
        try {
            root = body == null ? null : objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new AiResponseException(Reason.MALFORMED_RESPONSE, "Gemini reply is not JSON");
        }
        if (root == null || root.isMissingNode()) {
            throw new AiResponseException(Reason.MALFORMED_RESPONSE, "empty reply from Gemini");
        }
        JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!text.isTextual() || text.asText().isBlank()) {
            String blocked = root.path("promptFeedback").path("blockReason").asText("");
            String finish = root.path("candidates").path(0).path("finishReason").asText("");
            throw new AiResponseException(Reason.NO_CANDIDATES, "no candidate text"
                    + (blocked.isEmpty() ? "" : ", prompt blocked: " + blocked)
                    + (finish.isEmpty() ? "" : ", finish reason: " + finish));
        }
        return text.asText();
    }
}

package com.fitness.aiservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fitness.aiservice.service.AiResponseException.Reason;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the model's text into the four sections the prompt asks for. Anything that isn't that JSON
 * shape is rejected with MALFORMED_RESPONSE, so a half-parsed reply is never stored as analysis.
 */
final class RecommendationParser {

    record Advice(String analysis, List<String> improvements, List<String> suggestions, List<String> safety) {
    }

    private static final String[][] ANALYSIS_SECTIONS = {
            {"overall", "Overall:"}, {"pace", "Pace:"}, {"heartRate", "Heart Rate:"}, {"caloriesBurned", "Calories:"}};

    private RecommendationParser() {
    }

    static Advice parse(ObjectMapper mapper, String modelText) {
        JsonNode root;
        try {
            root = mapper.readTree(stripCodeFence(modelText));
        } catch (JsonProcessingException e) {
            throw new AiResponseException(Reason.MALFORMED_RESPONSE, "model reply is not JSON");
        }
        if (root == null || !root.path("analysis").isObject()) {
            throw new AiResponseException(Reason.MALFORMED_RESPONSE, "model reply has no analysis object");
        }

        StringBuilder analysis = new StringBuilder();
        for (String[] section : ANALYSIS_SECTIONS) {
            JsonNode value = root.path("analysis").path(section[0]);
            if (value.isTextual() && !value.asText().isBlank()) {
                analysis.append(section[1]).append(' ').append(value.asText().trim()).append("\n\n");
            }
        }
        if (analysis.isEmpty()) {
            throw new AiResponseException(Reason.MALFORMED_RESPONSE, "model reply has an empty analysis");
        }

        return new Advice(analysis.toString().trim(),
                pairs(root, "improvements", "area", "recommendation", "No specific improvements provided"),
                pairs(root, "suggestions", "workout", "description", "No specific suggestions provided"),
                strings(root, "safety", "Follow general safety guidelines"));
    }

    /** Models often wrap JSON in a Markdown code fence even when told not to. */
    static String stripCodeFence(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            trimmed = firstNewline < 0 ? "" : trimmed.substring(firstNewline + 1);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private static List<String> pairs(JsonNode root, String field, String title, String detail, String fallback) {
        List<String> items = new ArrayList<>();
        list(root, field).forEach(item -> {
            String head = item.path(title).asText("").trim();
            String body = item.path(detail).asText("").trim();
            if (!head.isEmpty() && !body.isEmpty()) {
                items.add(head + ": " + body);
            }
        });
        return items.isEmpty() ? List.of(fallback) : items;
    }

    private static List<String> strings(JsonNode root, String field, String fallback) {
        List<String> items = new ArrayList<>();
        list(root, field).forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) {
                items.add(item.asText().trim());
            }
        });
        return items.isEmpty() ? List.of(fallback) : items;
    }

    /** A missing list is allowed (the defaults above apply); a list of the wrong type is not. */
    private static JsonNode list(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return JsonNodeFactory.instance.arrayNode();
        }
        if (!node.isArray()) {
            throw new AiResponseException(Reason.MALFORMED_RESPONSE, "model reply field '" + field + "' is not a list");
        }
        return node;
    }
}

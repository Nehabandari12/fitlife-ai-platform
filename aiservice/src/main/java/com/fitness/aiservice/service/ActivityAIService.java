package com.fitness.aiservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.model.RecommendationStatus;
import com.fitness.aiservice.service.AiResponseException.Reason;
import com.fitness.aiservice.service.RecommendationParser.Advice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds recommendations from the model's reply. When the model fails or replies with something
 * unusable, the result is a fixed fallback marked FALLBACK with the reason, never an exception: the
 * Kafka listener must not retry (and pay for) a model call because of a bad reply. Prompts and
 * replies are not logged, because they describe a person's workouts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {
    static final String USER_SUMMARY = "USER_SUMMARY";

    private final TextGenerator textGenerator;
    private final ObjectMapper objectMapper;

    public Recommendation generateRecommendation(Activity activity) {
        String type = activity.getType() == null ? "OTHER" : activity.getType().toString();
        try {
            Advice advice = RecommendationParser.parse(objectMapper, textGenerator.generate(createPromptForActivity(activity)));
            return generated(activity.getId(), activity.getUserId(), type, advice);
        } catch (AiResponseException e) {
            log.warn("Recommendation for activity {} uses the fallback: {} ({})", activity.getId(), e.getReason(), e.getMessage());
            return fallback(activity.getId(), activity.getUserId(), type, e.getReason(),
                    "Unable to generate detailed analysis",
                    List.of("Continue with your current routine"),
                    List.of("Consider consulting a fitness consultant"));
        }
    }

    /** One summary over a user's generated recommendations. The user id is not sent to the model. */
    public Recommendation generateUserCombinedRecommendation(String userId, List<Recommendation> recs) {
        try {
            String prompt = createPromptForUserFromRecommendations(objectMapper.writeValueAsString(recs.stream().map(ActivityAIService::forPrompt).toList()));
            Advice advice = RecommendationParser.parse(objectMapper, textGenerator.generate(prompt));
            return generated(null, userId, USER_SUMMARY, advice);
        } catch (AiResponseException e) {
            log.warn("Summary for user {} uses the fallback: {} ({})", userId, e.getReason(), e.getMessage());
            return fallback(null, userId, USER_SUMMARY, e.getReason(),
                    "Unable to generate combined recommendation from existing records.",
                    List.of("Review individual activity recommendations."),
                    List.of("Continue tracking workouts and recommendations."));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize recommendations", e);
        }
    }

    private static Map<String, Object> forPrompt(Recommendation rec) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", rec.getType());
        item.put("recommendation", rec.getRecommendation());
        item.put("improvements", rec.getImprovements());
        item.put("suggestions", rec.getSuggestions());
        item.put("safety", rec.getSafety());
        return item;
    }

    private static Recommendation generated(String activityId, String userId, String type, Advice advice) {
        return Recommendation.builder()
                .activityId(activityId)
                .userId(userId)
                .type(type)
                .recommendation(advice.analysis())
                .improvements(advice.improvements())
                .suggestions(advice.suggestions())
                .safety(advice.safety())
                .status(RecommendationStatus.GENERATED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static Recommendation fallback(String activityId, String userId, String type, Reason reason,
                                           String text, List<String> improvements, List<String> suggestions) {
        return Recommendation.builder()
                .activityId(activityId)
                .userId(userId)
                .type(type)
                .recommendation(text)
                .improvements(improvements)
                .suggestions(suggestions)
                .safety(List.of("Always warm up before exercise", "Stay hydrated", "Listen to your body"))
                .status(RecommendationStatus.FALLBACK)
                .fallbackReason(reason.name())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String createPromptForActivity(Activity activity) {
        return String.format("""
        Analyze this fitness activity and provide detailed recommendations in the following EXACT JSON format:
        {
          "analysis": {
            "overall": "Overall analysis here",
            "pace": "Pace analysis here",
            "heartRate": "Heart rate analysis here",
            "caloriesBurned": "Calories analysis here"
          },
          "improvements": [
            {
              "area": "Area name",
              "recommendation": "Detailed recommendation"
            }
          ],
          "suggestions": [
            {
              "workout": "Workout name",
              "description": "Detailed workout description"
            }
          ],
          "safety": [
            "Safety point 1",
            "Safety point 2"
          ]
        }

        Analyze this activity:
        Activity Type: %s
        Duration: %d minutes
        Calories Burned: %d
        Additional Metrics: %s

        Provide detailed analysis focusing on performance, improvements, next workout suggestions, and safety guidelines.
        Ensure the response follows the EXACT JSON format shown above.
        """,
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                activity.getAdditionalMetrics()
        );
    }

    private String createPromptForUserFromRecommendations(String recsJsonArray) {
        return String.format("""
        You are an expert fitness coach.

        Below is the FULL list of activity-level recommendations for a user.
        Each item already contains:
        - activity type
        - detailed recommendation text
        - improvements
        - suggestions
        - safety tips

        ACTIVITY-LEVEL RECOMMENDATIONS (JSON ARRAY):
        %s

        Using ALL of the information above, create ONE combined recommendation for this user.

        Return the result in the EXACT JSON format below (NO extra text, NO markdown):

        {
          "analysis": {
            "overall": "Overall analysis for the user",
            "pace": "Overall comments on user's pace across activities",
            "heartRate": "Overall comments on heart rate / intensity consistency",
            "caloriesBurned": "Overall comments on calorie burn patterns"
          },
          "improvements": [
            {
              "area": "Key area to improve (e.g., Intensity, Consistency, Data Tracking)",
              "recommendation": "Detailed combined recommendation for this area"
            }
          ],
          "suggestions": [
            {
              "workout": "Suggested workout type",
              "description": "Detailed description of what the user should do"
            }
          ],
          "safety": [
            "Important global safety guideline for this user",
            "Another key safety point"
          ]
        }

        Focus on patterns across ALL activities (e.g., low intensity, poor tracking, consistency).
        """, recsJsonArray);
    }
}

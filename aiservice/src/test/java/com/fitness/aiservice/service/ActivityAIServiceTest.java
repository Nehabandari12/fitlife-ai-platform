package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.ActivityType;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.model.RecommendationStatus;
import com.fitness.aiservice.service.AiResponseException.Reason;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Parsing the model's reply, and the explicit fallback when the reply is missing or unusable. */
class ActivityAIServiceTest {

    static final String GOOD_REPLY = """
            ```json
            {
              "analysis": {"overall": "Solid steady run.", "pace": "Even splits.", "heartRate": "Zone 2.", "caloriesBurned": "As expected."},
              "improvements": [{"area": "Cadence", "recommendation": "Aim for 170 steps per minute."}],
              "suggestions": [{"workout": "Intervals", "description": "6 x 400 m with 90 s rest."}],
              "safety": ["Warm up for 10 minutes."]
            }
            ```""";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Activity run = Activity.builder().id("act-1").userId("user-a").type(ActivityType.RUNNING)
            .duration(30).caloriesBurned(300).additionalMetrics(Map.of("distanceKm", 5)).build();

    private ActivityAIService service(TextGenerator generator) {
        return new ActivityAIService(generator, mapper);
    }

    @Test
    void aGoodReplyBecomesAGeneratedRecommendation() {
        Recommendation rec = service(prompt -> GOOD_REPLY).generateRecommendation(run);

        assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.GENERATED);
        assertThat(rec.getFallbackReason()).isNull();
        assertThat(rec.getActivityId()).isEqualTo("act-1");
        assertThat(rec.getUserId()).isEqualTo("user-a");
        assertThat(rec.getRecommendation()).startsWith("Overall: Solid steady run.").contains("Heart Rate: Zone 2.");
        assertThat(rec.getImprovements()).containsExactly("Cadence: Aim for 170 steps per minute.");
        assertThat(rec.getSuggestions()).containsExactly("Intervals: 6 x 400 m with 90 s rest.");
        assertThat(rec.getSafety()).containsExactly("Warm up for 10 minutes.");
    }

    @Test
    void aModelFailureIsAMarkedFallbackNotAnException() {
        Recommendation rec = service(prompt -> {
            throw new AiResponseException(Reason.TIMEOUT, "no reply");
        }).generateRecommendation(run);

        assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.FALLBACK);
        assertThat(rec.getFallbackReason()).isEqualTo("TIMEOUT");
        assertThat(rec.getActivityId()).isEqualTo("act-1");
        assertThat(rec.getSafety()).isNotEmpty();
    }

    @Test
    void prosePassedOffAsJsonIsAMarkedFallback() {
        Recommendation rec = service(prompt -> "Great job! Keep it up.").generateRecommendation(run);
        assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.FALLBACK);
        assertThat(rec.getFallbackReason()).isEqualTo("MALFORMED_RESPONSE");
    }

    @Test
    void jsonWithoutTheAnalysisSectionIsRejected() {
        assertThatThrownBy(() -> RecommendationParser.parse(mapper, "{\"improvements\": []}"))
                .isInstanceOf(AiResponseException.class);
        assertThatThrownBy(() -> RecommendationParser.parse(mapper, "{\"analysis\": {}}"))
                .isInstanceOf(AiResponseException.class);
        assertThatThrownBy(() -> RecommendationParser.parse(mapper, "{\"analysis\": {\"overall\": \"ok\"}, \"safety\": \"not a list\"}"))
                .isInstanceOf(AiResponseException.class);
    }

    @Test
    void missingListsGetNeutralDefaults() {
        RecommendationParser.Advice advice = RecommendationParser.parse(mapper, "{\"analysis\": {\"overall\": \"ok\"}}");
        assertThat(advice.analysis()).isEqualTo("Overall: ok");
        assertThat(advice.improvements()).containsExactly("No specific improvements provided");
        assertThat(advice.safety()).containsExactly("Follow general safety guidelines");
    }

    @Test
    void theUserSummaryPromptDoesNotContainTheUserId() {
        AtomicReference<String> sent = new AtomicReference<>();
        Recommendation existing = Recommendation.builder().activityId("act-1").userId("user-a").type("RUNNING")
                .recommendation("Overall: fine").improvements(List.of("x")).suggestions(List.of("y")).safety(List.of("z")).build();

        Recommendation summary = service(prompt -> {
            sent.set(prompt);
            return GOOD_REPLY;
        }).generateUserCombinedRecommendation("user-a", List.of(existing));

        assertThat(sent.get()).doesNotContain("user-a").doesNotContain("act-1").contains("Overall: fine");
        assertThat(summary.getType()).isEqualTo("USER_SUMMARY");
        assertThat(summary.getUserId()).isEqualTo("user-a");
        assertThat(summary.getStatus()).isEqualTo(RecommendationStatus.GENERATED);
    }
}

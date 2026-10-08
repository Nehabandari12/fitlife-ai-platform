package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.ActivityType;
import com.fitness.aiservice.model.DeletedActivity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.model.RecommendationStatus;
import com.fitness.aiservice.respository.DeletedActivityRepository;
import com.fitness.aiservice.respository.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** At-least-once delivery: duplicates update one record, and events after a delete are dropped. */
class ActivityMessageListenerTest {

    private final ActivityAIService ai = mock(ActivityAIService.class);
    private final RecommendationRepository recommendations = mock(RecommendationRepository.class);
    private final DeletedActivityRepository deleted = mock(DeletedActivityRepository.class);
    private final ActivityMessageListener listener = new ActivityMessageListener(ai, recommendations, deleted);
    private final ActivityDeleteListener deleteListener = new ActivityDeleteListener(recommendations, deleted);

    private final Activity run = Activity.builder().id("act-1").userId("user-a").type(ActivityType.RUNNING).build();

    private static Recommendation generated(String text) {
        return Recommendation.builder().activityId("act-1").userId("user-a").type("RUNNING")
                .recommendation(text).status(RecommendationStatus.GENERATED).build();
    }

    @Test
    void anUpdateRewritesTheExistingRecordInsteadOfAddingOne() {
        Recommendation existing = generated("old");
        existing.setId("rec-1");
        when(recommendations.findByActivityId("act-1")).thenReturn(Optional.of(existing));
        when(ai.generateRecommendation(run)).thenReturn(generated("new"));

        listener.processActivity(run);

        ArgumentCaptor<Recommendation> saved = ArgumentCaptor.forClass(Recommendation.class);
        verify(recommendations).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo("rec-1");
        assertThat(saved.getValue().getRecommendation()).isEqualTo("new");
    }

    @Test
    void aDuplicateDeliveryThatLosesTheInsertRaceUpdatesTheWinner() {
        Recommendation winner = generated("first");
        winner.setId("rec-1");
        when(recommendations.findByActivityId("act-1")).thenReturn(Optional.empty(), Optional.of(winner));
        when(recommendations.save(any(Recommendation.class)))
                .thenThrow(new DuplicateKeyException("activityId"))
                .thenAnswer(call -> call.getArgument(0));
        when(ai.generateRecommendation(run)).thenReturn(generated("second"));

        listener.processActivity(run);

        ArgumentCaptor<Recommendation> saved = ArgumentCaptor.forClass(Recommendation.class);
        verify(recommendations, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(1).getId()).isEqualTo("rec-1");
        assertThat(saved.getAllValues().get(1).getRecommendation()).isEqualTo("second");
    }

    @Test
    void anEventForADeletedActivityIsDroppedWithoutAModelCall() {
        when(deleted.existsById("act-1")).thenReturn(true);

        listener.processActivity(run);

        verify(ai, never()).generateRecommendation(any());
        verify(recommendations, never()).save(any());
    }

    @Test
    void aDeleteDuringGenerationIsHonoured() {
        when(deleted.existsById("act-1")).thenReturn(false, true);
        when(ai.generateRecommendation(run)).thenReturn(generated("late"));

        listener.processActivity(run);

        verify(recommendations, never()).save(any());
    }

    @Test
    void aDeleteRecordsATombstoneAndRemovesTheRecommendation() {
        deleteListener.handleActivityDelete(run);

        ArgumentCaptor<DeletedActivity> tombstone = ArgumentCaptor.forClass(DeletedActivity.class);
        verify(deleted).save(tombstone.capture());
        assertThat(tombstone.getValue().getActivityId()).isEqualTo("act-1");
        verify(recommendations).deleteByActivityId("act-1");
    }
}

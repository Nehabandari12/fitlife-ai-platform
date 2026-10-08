package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.DeletedActivity;
import com.fitness.aiservice.respository.DeletedActivityRepository;
import com.fitness.aiservice.respository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityDeleteListener {

    private final RecommendationRepository recommendationRepository;
    private final DeletedActivityRepository deletedActivityRepository;

    /** Records the deletion first, so a create or update event that arrives later is dropped. */
    @KafkaListener(
            topics = "${kafka.topic.delete-name}",
            groupId = "activity-delete-processor-group"
    )
    public void handleActivityDelete(Activity activity) {
        if (activity == null || activity.getId() == null) {
            log.warn("Skipping a delete event without an activity id");
            return;
        }
        String activityId = activity.getId();
        deletedActivityRepository.save(new DeletedActivity(activityId, Instant.now()));
        recommendationRepository.deleteByActivityId(activityId);
        log.info("Deleted the recommendation for activity {}", activityId);
    }
}

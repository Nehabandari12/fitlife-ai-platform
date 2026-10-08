package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.respository.DeletedActivityRepository;
import com.fitness.aiservice.respository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Create and update events. Kafka delivers at least once, so the same event can arrive twice: the
 * recommendation is upserted by activity id (unique index), and a duplicate costs one extra model
 * call but never a second record. Events for an activity that was already deleted are dropped.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAIService activityAIService;
    private final RecommendationRepository recommendationRepository;
    private final DeletedActivityRepository deletedActivityRepository;

    @KafkaListener(topics = "${kafka.topic.name}", groupId = "activity-processor-group")
    public void processActivity(Activity activity) {
        if (activity == null || activity.getId() == null) {
            log.warn("Skipping an activity event without an id");
            return;
        }
        if (isDeleted(activity)) {
            return;
        }
        Recommendation fresh = activityAIService.generateRecommendation(activity);
        if (isDeleted(activity)) { // deleted while the model was answering
            return;
        }
        upsert(fresh);
    }

    private boolean isDeleted(Activity activity) {
        if (deletedActivityRepository.existsById(activity.getId())) {
            log.info("Skipping event for deleted activity {}", activity.getId());
            return true;
        }
        return false;
    }

    void upsert(Recommendation fresh) {
        var existing = recommendationRepository.findByActivityId(fresh.getActivityId());
        try {
            recommendationRepository.save(existing.map(current -> copyInto(current, fresh)).orElse(fresh));
        } catch (DuplicateKeyException e) {
            // Another delivery of the same activity inserted first; update that record instead.
            recommendationRepository.findByActivityId(fresh.getActivityId())
                    .ifPresent(current -> recommendationRepository.save(copyInto(current, fresh)));
        }
    }

    private static Recommendation copyInto(Recommendation current, Recommendation fresh) {
        current.setType(fresh.getType());
        current.setRecommendation(fresh.getRecommendation());
        current.setImprovements(fresh.getImprovements());
        current.setSuggestions(fresh.getSuggestions());
        current.setSafety(fresh.getSafety());
        current.setStatus(fresh.getStatus());
        current.setFallbackReason(fresh.getFallbackReason());
        current.setCreatedAt(fresh.getCreatedAt());
        return current;
    }
}

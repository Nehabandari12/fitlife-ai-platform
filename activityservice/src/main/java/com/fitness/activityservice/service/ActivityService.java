package com.fitness.activityservice.service;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserValidationService userValidationService;
    private final KafkaTemplate<String, Activity> kafkaTemplate;

    @Value("${kafka.topic.name}")
    private String topicName;

    @Value("${kafka.topic.delete-name}")
    private String deleteTopicName;

    public ActivityResponse trackActivity(ActivityRequest request, String bearerToken) {

        if (!userValidationService.validateUser(request.getUserId(), bearerToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user");
        }

        Activity activity = Activity.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .duration(request.getDuration())
                .caloriesBurned(request.getCaloriesBurned())
                .startTime(request.getStartTime())
                .additionalMetrics(request.getAdditionalMetrics())
                .build();

        Activity savedActivity = activityRepository.save(activity);
        publish(topicName, savedActivity.getUserId(), savedActivity, "create");
        return mapToResponse(savedActivity);
    }

    private ActivityResponse mapToResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setUserId(activity.getUserId());
        response.setType(activity.getType());
        response.setDuration(activity.getDuration());
        response.setCaloriesBurned(activity.getCaloriesBurned());
        response.setStartTime(activity.getStartTime());
        response.setAdditionalMetrics(activity.getAdditionalMetrics());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());
        return response;

    }


    public List<ActivityResponse> getUserActivities(String userId) {
        List<Activity> activityList = activityRepository.findByUserId(userId);
        return activityList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void deleteActivity(String activityId, String userId) {
        Activity activity = ownedActivity(activityId, userId);
        activityRepository.delete(activity);
        // The AI service deletes the recommendation when it receives this event.
        publish(deleteTopicName, activity.getId(), activity, "delete");
    }


    public ActivityResponse updateActivity(String activityId, ActivityRequest request) {
        Activity existing = ownedActivity(activityId, request.getUserId());

        // Update mutable fields
        existing.setType(request.getType());
        existing.setDuration(request.getDuration());
        existing.setCaloriesBurned(request.getCaloriesBurned());
        existing.setStartTime(request.getStartTime());
        existing.setAdditionalMetrics(request.getAdditionalMetrics());

        Activity updated = activityRepository.save(existing);
        publish(topicName, updated.getUserId(), updated, "update");
        return mapToResponse(updated);
    }

    public ActivityResponse getActivityById(String activityId, String userId) {
        return mapToResponse(ownedActivity(activityId, userId));
    }

    /**
     * Another user's activity gets the same 404 as a missing one, so ids can't be probed.
     */
    private Activity ownedActivity(String activityId, String userId) {
        return activityRepository.findById(activityId)
                .filter(activity -> activity.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
    }

    /**
     * Kafka sends are asynchronous: the producer retries on its own until delivery.timeout.ms, and a
     * send that still fails is only reported here. The activity itself is already saved, so the
     * request succeeds; the recommendation for it is then missing (see README, "Consistency").
     */
    private void publish(String topic, String key, Activity activity, String kind) {
        try {
            kafkaTemplate.send(topic, key, activity).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Could not publish {} event for activity {}: {}", kind, activity.getId(), error.getMessage());
                }
            });
        } catch (RuntimeException e) {
            log.error("Could not publish {} event for activity {}: {}", kind, activity.getId(), e.getMessage());
        }
    }
}

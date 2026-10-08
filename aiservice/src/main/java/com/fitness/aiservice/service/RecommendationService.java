package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.model.RecommendationStatus;
import com.fitness.aiservice.respository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Reads recommendations for the caller only; callerId is the subject of the validated access token. */
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationRepository recommendationRepository;
    private final ActivityAIService activityAIService;

    /** Generates a summary with the model, so it is refused before any model call for another user. */
    public Recommendation getUserRecommendation(String userId, String callerId) {
        if (!userId.equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only read your own recommendations");
        }
        List<Recommendation> recs = recommendationRepository.findByUserId(userId).stream()
                .filter(rec -> rec.getStatus() != RecommendationStatus.FALLBACK)
                .toList();
        if (recs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No recommendations yet");
        }
        return activityAIService.generateUserCombinedRecommendation(userId, recs);
    }

    /** Another user's recommendation gets the same 404 as a missing one, so ids can't be probed. */
    public Recommendation getActivityRecommendation(String activityId, String callerId) {
        return recommendationRepository.findByActivityId(activityId)
                .filter(rec -> callerId.equals(rec.getUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No recommendation for this activity"));
    }
}

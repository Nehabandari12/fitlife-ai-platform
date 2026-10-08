package com.fitness.aiservice.controller;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The caller is always the subject of the validated access token. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<Recommendation> getUserCombinedRecommendation(@PathVariable String userId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(recommendationService.getUserRecommendation(userId, jwt.getSubject()));
    }

    @GetMapping("/activity/{activityId}")
    public ResponseEntity<Recommendation> getActivityRecommendation(@PathVariable String activityId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(recommendationService.getActivityRecommendation(activityId, jwt.getSubject()));
    }
}

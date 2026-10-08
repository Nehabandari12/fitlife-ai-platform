package com.fitness.activityservice.controller;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.service.ActivityService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The caller is always the subject of the validated access token. */
@RestController
@RequestMapping("/api/activities")
@AllArgsConstructor
public class ActivityController {

    private ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityResponse> trackActivity(@RequestBody ActivityRequest request, @AuthenticationPrincipal Jwt jwt) {
        request.setUserId(jwt.getSubject());
        return ResponseEntity.ok(activityService.trackActivity(request, jwt.getTokenValue()));
    }


    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getUserActivities(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(activityService.getUserActivities(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponse> getActivityById(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(activityService.getActivityById(id, jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        activityService.deleteActivity(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable String id,
            @RequestBody ActivityRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        request.setUserId(jwt.getSubject());
        return ResponseEntity.ok(activityService.updateActivity(id, request));
    }


}

package com.fitness.activityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {
    private final WebClient userServiceWebClient;

    /** Asks the user service whether this user exists, forwarding the caller's own token. */
    public boolean validateUser(String userId, String bearerToken) {
        log.debug("Checking user {} in the user service", userId);
        try {
            return Boolean.TRUE.equals(userServiceWebClient.get()
                    .uri("/api/users/{userId}/validate", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block());
        } catch (WebClientException e) {
            log.warn("User service check failed for {}: {}", userId, e.getMessage());
            return false;
        }
    }
}

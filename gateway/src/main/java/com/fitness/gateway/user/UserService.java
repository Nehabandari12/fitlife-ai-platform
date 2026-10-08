package com.fitness.gateway.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Calls the user service on behalf of the signed-in user. The user's own access token is forwarded,
 * because the user service authorizes every call against the token's subject.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final WebClient userServiceWebClient;

    public Mono<Boolean> validateUser(String userId, String bearerToken) {
        log.debug("Checking user {} in the user service", userId);
        return userServiceWebClient.get()
                .uri("/api/users/{userId}/validate", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND)
                        return Mono.error(new RuntimeException("User not found : " + userId));

                    else if (e.getStatusCode() == HttpStatus.BAD_REQUEST)
                        return Mono.error(new RuntimeException("Invalid : " + userId));

                    return Mono.error(new RuntimeException("Unexpected error : " + userId));
                });
    }

    public Mono<UserResponse> registerUser(RegisterRequest registerRequest, String bearerToken) {
        log.info("Registering user {} in the user service", registerRequest.getKeycloakId());
        return userServiceWebClient.post()
                .uri("/api/users/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .bodyValue(registerRequest)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.BAD_REQUEST)
                        return Mono.error(new RuntimeException("Bad request : " + e.getMessage()));

                    return Mono.error(new RuntimeException("Unexpected error : " + e.getMessage()));
                });
    }
}

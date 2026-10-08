package com.fitness.userservice.services;

import com.fitness.userservice.UserRepository;
import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.models.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;

    /**
     * Idempotent: registering an existing Keycloak user returns their profile. An email that already
     * belongs to a different user is a conflict, and nothing about that user is returned.
     */
    public UserResponse register(RegisterRequest request) {
        var existing = repository.findByKeycloakId(request.getKeycloakId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        if (request.getEmail() != null && repository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setKeycloakId(request.getKeycloakId());
        user.setLastName(request.getLastName());
        return toResponse(repository.save(user));
    }

    public UserResponse getUserProfile(String userId) {
        User user = repository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    public Boolean existByUserId(String userId) {
        log.debug("Checking user {}", userId);
        return repository.existsByKeycloakId(userId);
    }

    private static UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setKeycloakId(user.getKeycloakId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}

package com.fitness.userservice.controller;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.services.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** A caller can read, check and register only their own profile: the token's subject decides. */
@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable String userId, @AuthenticationPrincipal Jwt jwt) {
        requireSelf(userId, jwt);
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request, @AuthenticationPrincipal Jwt jwt) {
        request.setKeycloakId(jwt.getSubject());
        if (jwt.getClaimAsString("email") != null) {
            request.setEmail(jwt.getClaimAsString("email"));
        }
        return ResponseEntity.ok(userService.register(request));
    }

    @GetMapping("/{userId}/validate")
    public ResponseEntity<Boolean> validateUser(@PathVariable String userId, @AuthenticationPrincipal Jwt jwt) {
        requireSelf(userId, jwt);
        return ResponseEntity.ok(userService.existByUserId(userId));
    }

    private static void requireSelf(String userId, Jwt jwt) {
        if (!userId.equals(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own profile");
        }
    }
}

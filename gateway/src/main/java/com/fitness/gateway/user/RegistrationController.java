package com.fitness.gateway.user;


import com.fitness.gateway.KeycloakAdminClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final KeycloakAdminClient keycloakAdminClient;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<String>> register(@Valid @RequestBody RegisterRequest request) {

        return keycloakAdminClient.createUser(
                        request.getEmail(),
                        request.getPassword(),
                        request.getFirstName(),
                        request.getLastName()
                )
                .then(Mono.just(
                        ResponseEntity.status(HttpStatus.CREATED)
                                .body("User registered. Please log in.")
                ))
                .onErrorResume(UserAlreadyExistsException.class, ex ->
                        Mono.just(
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(ex.getMessage())
                        )
                );
    }
}

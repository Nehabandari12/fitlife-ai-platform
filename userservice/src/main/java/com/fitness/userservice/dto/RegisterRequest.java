package com.fitness.userservice.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * Profile data for a Keycloak user. There is no password: Keycloak owns credentials. The controller
 * takes keycloakId (and the email, when the token has one) from the caller's access token.
 */
@Data
public class RegisterRequest {
    @Email(message = "Invalid email format")
    private String email;
    private String keycloakId;
    private String firstName;
    private String lastName;
}

package com.fitness.userservice.controller;

import com.fitness.userservice.UserRepository;
import com.fitness.userservice.config.SecurityConfig;
import com.fitness.userservice.models.User;
import com.fitness.userservice.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A user can see, check and register only their own profile, and no response carries a password. */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, UserService.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/unused"})
class UserAccessTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserRepository repository;

    private static RequestPostProcessor as(String subject) {
        return jwt().jwt(token -> token.subject(subject).claim("email", subject + "@example.com"));
    }

    private static User user(String keycloakId) {
        User user = new User();
        user.setId("id-" + keycloakId);
        user.setKeycloakId(keycloakId);
        user.setEmail(keycloakId + "@example.com");
        user.setFirstName("First");
        return user;
    }

    @Test
    void profilesRequireAToken() throws Exception {
        mvc.perform(get("/api/users/user-b")).andExpect(status().isUnauthorized());
    }

    @Test
    void anotherUsersProfileIsForbidden() throws Exception {
        mvc.perform(get("/api/users/user-b").with(as("user-a")).header("X-User-ID", "user-b"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/users/user-b/validate").with(as("user-a")))
                .andExpect(status().isForbidden());
        verify(repository, never()).findByKeycloakId("user-b");
        verify(repository, never()).existsByKeycloakId("user-b");
    }

    @Test
    void ownProfileHasNoPasswordField() throws Exception {
        when(repository.findByKeycloakId("user-a")).thenReturn(Optional.of(user("user-a")));
        mvc.perform(get("/api/users/user-a").with(as("user-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakId").value("user-a"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registrationUsesTheTokenNotTheBody() throws Exception {
        when(repository.findByKeycloakId("user-a")).thenReturn(Optional.empty());
        when(repository.existsByEmail("user-a@example.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        mvc.perform(post("/api/users/register").with(as("user-a")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keycloakId": "user-b", "email": "user-b@example.com", "password": "ignored"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakId").value("user-a"))
                .andExpect(jsonPath("$.email").value("user-a@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void anEmailTakenByAnotherUserIsAConflictThatRevealsNothing() throws Exception {
        when(repository.findByKeycloakId("user-a")).thenReturn(Optional.empty());
        when(repository.existsByEmail("user-a@example.com")).thenReturn(true);

        mvc.perform(post("/api/users/register").with(as("user-a")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id-"))));
        verify(repository, never()).save(any(User.class));
    }
}

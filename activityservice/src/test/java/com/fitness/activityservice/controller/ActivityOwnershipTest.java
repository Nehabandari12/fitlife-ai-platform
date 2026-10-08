package com.fitness.activityservice.controller;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.config.SecurityConfig;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.model.ActivityType;
import com.fitness.activityservice.service.ActivityService;
import com.fitness.activityservice.service.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** User A must not read or change user B's activities, whatever headers or body fields A sends. */
@WebMvcTest(ActivityController.class)
@Import({SecurityConfig.class, ActivityService.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/unused",
        "kafka.topic.name=activity-events",
        "kafka.topic.delete-name=activity-delete-events"})
class ActivityOwnershipTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ActivityRepository activityRepository;

    @MockitoBean
    UserValidationService userValidationService;

    @MockitoBean
    KafkaTemplate<String, Activity> kafkaTemplate;

    private static final String BODY = """
            {"userId": "user-b", "type": "RUNNING", "duration": 30, "caloriesBurned": 300}
            """;

    private final Activity activityOfB = Activity.builder().id("act-b").userId("user-b").type(ActivityType.RUNNING).build();

    private static RequestPostProcessor as(String subject) {
        return jwt().jwt(token -> token.subject(subject));
    }

    @BeforeEach
    void setUp() {
        when(activityRepository.findById("act-b")).thenReturn(Optional.of(activityOfB));
        when(activityRepository.save(any(Activity.class))).thenAnswer(call -> call.getArgument(0));
        when(kafkaTemplate.send(anyString(), anyString(), any(Activity.class))).thenReturn(CompletableFuture.completedFuture(null));
        when(userValidationService.validateUser(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void requestsWithoutATokenAreRejected() throws Exception {
        mvc.perform(get("/api/activities/act-b").header("X-User-ID", "user-b")).andExpect(status().isUnauthorized());
    }

    @Test
    void anotherUsersActivityLooksMissingEvenWithAForgedHeader() throws Exception {
        mvc.perform(get("/api/activities/act-b").with(as("user-a")).header("X-User-ID", "user-b"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanReadTheirActivity() throws Exception {
        mvc.perform(get("/api/activities/act-b").with(as("user-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("act-b"));
    }

    @Test
    void anotherUserCannotUpdateOrDelete() throws Exception {
        mvc.perform(put("/api/activities/act-b").with(as("user-a")).header("X-User-ID", "user-b")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/activities/act-b").with(as("user-a")).header("X-User-ID", "user-b"))
                .andExpect(status().isNotFound());

        verify(activityRepository, never()).save(any(Activity.class));
        verify(activityRepository, never()).delete(any(Activity.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(Activity.class));
    }

    @Test
    void listingReturnsOnlyTheCallersActivities() throws Exception {
        when(activityRepository.findByUserId("user-a")).thenReturn(List.of());
        mvc.perform(get("/api/activities").with(as("user-a")).header("X-User-ID", "user-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(activityRepository).findByUserId("user-a");
        verify(activityRepository, never()).findByUserId("user-b");
    }

    @Test
    void newActivityBelongsToTheCallerNotToTheBodyUserId() throws Exception {
        mvc.perform(post("/api/activities").with(as("user-a"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-a"));
    }

    @Test
    void unknownUserCannotCreateActivities() throws Exception {
        when(userValidationService.validateUser(eq("user-x"), anyString())).thenReturn(false);
        mvc.perform(post("/api/activities").with(as("user-x"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    void aFailedEventPublishDoesNotFailTheSavedActivity() throws Exception {
        when(kafkaTemplate.send(anyString(), anyString(), any(Activity.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        mvc.perform(post("/api/activities").with(as("user-a"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
        verify(activityRepository).save(any(Activity.class));
    }
}

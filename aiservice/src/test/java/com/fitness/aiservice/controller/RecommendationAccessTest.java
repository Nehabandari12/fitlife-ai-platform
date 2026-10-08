package com.fitness.aiservice.controller;

import com.fitness.aiservice.config.SecurityConfig;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.model.RecommendationStatus;
import com.fitness.aiservice.respository.RecommendationRepository;
import com.fitness.aiservice.service.ActivityAIService;
import com.fitness.aiservice.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** User A must not read user B's recommendations or make the model summarize B's history. */
@WebMvcTest(RecommendationController.class)
@Import({SecurityConfig.class, RecommendationService.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/unused"})
class RecommendationAccessTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RecommendationRepository repository;

    @MockitoBean
    ActivityAIService activityAIService;

    private static RequestPostProcessor as(String subject) {
        return jwt().jwt(token -> token.subject(subject));
    }

    @BeforeEach
    void setUp() {
        Recommendation ofB = Recommendation.builder().activityId("act-b").userId("user-b").type("RUNNING")
                .recommendation("Overall: private to B").status(RecommendationStatus.GENERATED).build();
        when(repository.findByActivityId("act-b")).thenReturn(Optional.of(ofB));
        when(repository.findByUserId("user-b")).thenReturn(List.of(ofB));
    }

    @Test
    void recommendationsRequireAToken() throws Exception {
        mvc.perform(get("/api/recommendations/activity/act-b").header("X-User-ID", "user-b"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anotherUsersRecommendationLooksMissingEvenWithAForgedHeader() throws Exception {
        mvc.perform(get("/api/recommendations/activity/act-b").with(as("user-a")).header("X-User-ID", "user-b"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanReadTheirRecommendationAndItsStatus() throws Exception {
        mvc.perform(get("/api/recommendations/activity/act-b").with(as("user-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").value("Overall: private to B"))
                .andExpect(jsonPath("$.status").value("GENERATED"));
    }

    @Test
    void anotherUsersSummaryIsRefusedBeforeAnyModelCall() throws Exception {
        mvc.perform(get("/api/recommendations/user/user-b").with(as("user-a")))
                .andExpect(status().isForbidden());
        verify(activityAIService, never()).generateUserCombinedRecommendation(anyString(), anyList());
        verify(repository, never()).findByUserId(any());
    }

    @Test
    void ownSummaryIsGenerated() throws Exception {
        when(activityAIService.generateUserCombinedRecommendation(anyString(), anyList()))
                .thenReturn(Recommendation.builder().userId("user-b").type("USER_SUMMARY").status(RecommendationStatus.GENERATED).build());
        mvc.perform(get("/api/recommendations/user/user-b").with(as("user-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("USER_SUMMARY"));
    }
}

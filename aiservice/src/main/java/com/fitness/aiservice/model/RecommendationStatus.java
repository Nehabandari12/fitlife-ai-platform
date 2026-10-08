package com.fitness.aiservice.model;

/** Whether a recommendation came from the model or is the fixed fallback shown when the model failed. */
public enum RecommendationStatus {
    GENERATED,
    FALLBACK
}

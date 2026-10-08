package com.fitness.aiservice.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "recommendations")
@Data
@Builder
public class Recommendation {
    @Id
    private String id;
    @Indexed(unique = true)
    private String activityId;
    private String type;
    private String userId;
    private String recommendation;
    private List<String> improvements;
    private List<String> suggestions;
    private List<String> safety;
    /** GENERATED, or FALLBACK when the model failed; the UI says so instead of passing generic advice off as analysis. */
    private RecommendationStatus status;
    /** Why the fallback was used (see AiResponseException.Reason); null when generated. */
    private String fallbackReason;

    @CreatedDate
    private LocalDateTime createdAt;
}

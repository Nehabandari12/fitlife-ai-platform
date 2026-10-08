package com.fitness.aiservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Remembers that an activity was deleted. Delete events travel on their own topic, so an update for
 * the same activity can arrive after its delete; without this record that late update would bring
 * the deleted activity's recommendation back.
 */
@Document(collection = "deleted_activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeletedActivity {
    @Id
    private String activityId;
    private Instant deletedAt;
}

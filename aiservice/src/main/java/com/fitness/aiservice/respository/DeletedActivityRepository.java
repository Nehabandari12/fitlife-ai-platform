package com.fitness.aiservice.respository;

import com.fitness.aiservice.model.DeletedActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeletedActivityRepository extends MongoRepository<DeletedActivity, String> {
}

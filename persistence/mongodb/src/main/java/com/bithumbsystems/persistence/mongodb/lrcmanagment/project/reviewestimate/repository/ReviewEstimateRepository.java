package com.bithumbsystems.persistence.mongodb.lrcmanagment.project.reviewestimate.repository;

import com.bithumbsystems.persistence.mongodb.lrcmanagment.project.reviewestimate.model.entity.ReviewEstimate;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReviewEstimateRepository extends ReactiveMongoRepository<ReviewEstimate, String> {

    Flux<ReviewEstimate> findByProjectId(String projectId);
    Mono<ReviewEstimate> findByFileKey(String fileKey);
}

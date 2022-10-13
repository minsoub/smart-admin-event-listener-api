package com.bithumbsystems.persistence.mongodb.document.repository;

import com.bithumbsystems.persistence.mongodb.document.model.entity.DocFile;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DocFileRepository extends ReactiveMongoRepository<DocFile, String> {
}

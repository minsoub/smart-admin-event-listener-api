package com.bithumbsystems.persistence.mongodb.document.service;

import com.bithumbsystems.persistence.mongodb.document.model.entity.DocFile;
import com.bithumbsystems.persistence.mongodb.document.repository.DocFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DocumentDomainService {
    private final DocFileRepository docFileRepository;

    public Mono<DocFile> save(DocFile chatFile) {
        return docFileRepository.save(chatFile);
    }
    public Mono<DocFile> findByDocFileId(String id) {
        return docFileRepository.findById(id);
    }
}

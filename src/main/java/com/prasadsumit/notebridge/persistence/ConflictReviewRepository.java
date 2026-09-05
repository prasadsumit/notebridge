package com.prasadsumit.notebridge.persistence;

import com.prasadsumit.notebridge.model.ConflictStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConflictReviewRepository extends JpaRepository<ConflictReview, Long> {
    List<ConflictReview> findByStatusOrderByCreatedAtDesc(ConflictStatus status);

    void deleteByChunkDocument(IndexedDocument document);
}

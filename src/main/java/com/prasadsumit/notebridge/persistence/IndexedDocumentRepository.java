package com.prasadsumit.notebridge.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface IndexedDocumentRepository extends JpaRepository<IndexedDocument, Long> { Optional<IndexedDocument> findByRelativePath(String relativePath); }

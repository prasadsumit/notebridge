package com.prasadsumit.notebridge.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NoteChunkRepository extends JpaRepository<NoteChunk, Long> { List<NoteChunk> findByExcludedFalse(); List<NoteChunk> findByDocument(IndexedDocument document); void deleteByDocument(IndexedDocument document); }

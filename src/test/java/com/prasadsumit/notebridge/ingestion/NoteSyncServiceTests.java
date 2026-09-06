package com.prasadsumit.notebridge.ingestion;

import com.prasadsumit.notebridge.persistence.*;
import com.prasadsumit.notebridge.review.ConflictDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NoteSyncServiceTests {
    @Test
    void indexesEveryChunkOfALongSourceWithoutRepeatingTheFirstChunk() throws Exception {
        FileContentExtractor extractor = mock(FileContentExtractor.class);
        IndexedDocumentRepository documents = mock(IndexedDocumentRepository.class);
        NoteChunkRepository chunks = mock(NoteChunkRepository.class);
        ConflictReviewRepository conflicts = mock(ConflictReviewRepository.class);
        ConflictDetectionService conflictDetection = mock(ConflictDetectionService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        MultipartFile file = mock(MultipartFile.class);
        String content = "a".repeat(3_000);

        when(file.getOriginalFilename()).thenReturn("notes.md");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
        when(extractor.supports("notes.md")).thenReturn(true);
        when(extractor.extract(any(), eq("notes.md"))).thenReturn(content);
        when(documents.findByRelativePath("notes.md")).thenReturn(Optional.empty());
        when(documents.findAll()).thenReturn(List.of());
        when(documents.save(any(IndexedDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AtomicLong ids = new AtomicLong();
        when(chunks.save(any(NoteChunk.class))).thenAnswer(invocation -> {
            NoteChunk chunk = invocation.getArgument(0);
            chunk.setId(ids.incrementAndGet());
            return chunk;
        });

        NoteSyncService service = new NoteSyncService(extractor, documents, chunks, conflicts, conflictDetection, vectorStore);
        SyncResult result = service.sync(List.of(new NoteSyncService.SelectedFile("notes.md", Instant.now(), file)));

        assertEquals(1, result.added());
        verify(chunks, times(3)).save(any(NoteChunk.class));
        verify(vectorStore, times(3)).add(any());
    }
}

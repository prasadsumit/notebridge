package com.prasadsumit.notebridge.ingestion;

import com.prasadsumit.notebridge.persistence.*;
import com.prasadsumit.notebridge.review.ConflictDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
        when(file.getSize()).thenReturn(3_000L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
        when(extractor.supports("notes.md")).thenReturn(true);
        when(extractor.extract(any(), eq("notes.md"))).thenReturn(content);
        when(documents.findByRelativePath("notes.md")).thenReturn(Optional.empty());
        IndexedDocument existingSource = new IndexedDocument();
        existingSource.setRelativePath("already-indexed.md");
        when(documents.findAll()).thenReturn(List.of(existingSource));
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
        assertEquals(0, result.removed());
        verify(documents).save(argThat(document -> document.getSourceFileSize() == 3_000L));
        verify(chunks, times(3)).save(any(NoteChunk.class));
        verify(vectorStore, times(3)).add(any());
        verify(documents, never()).delete(any(IndexedDocument.class));
    }

    @Test
    void removesSourceChunksConflictsAndVectorEmbeddings() {
        FileContentExtractor extractor = mock(FileContentExtractor.class);
        IndexedDocumentRepository documents = mock(IndexedDocumentRepository.class);
        NoteChunkRepository chunks = mock(NoteChunkRepository.class);
        ConflictReviewRepository conflicts = mock(ConflictReviewRepository.class);
        ConflictDetectionService conflictDetection = mock(ConflictDetectionService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        IndexedDocument document = new IndexedDocument();
        document.setId(7L);
        document.setRelativePath("books/guide.epub");
        NoteChunk first = new NoteChunk();
        first.setId(41L);
        first.setDocument(document);
        NoteChunk second = new NoteChunk();
        second.setId(42L);
        second.setDocument(document);
        when(documents.findById(7L)).thenReturn(Optional.of(document));
        when(chunks.findByDocument(document)).thenReturn(List.of(first, second));

        NoteSyncService service = new NoteSyncService(extractor, documents, chunks, conflicts, conflictDetection, vectorStore);
        String removedPath = service.remove(7L);

        assertEquals("books/guide.epub", removedPath);
        verify(conflicts).deleteByChunkDocument(document);
        verify(vectorStore).delete(List.of(vectorId(41L), vectorId(42L)));
        verify(chunks).deleteByDocument(document);
        verify(documents).delete(document);
    }

    private static String vectorId(long chunkId) {
        return UUID.nameUUIDFromBytes(("notebridge-chunk-" + chunkId).getBytes(StandardCharsets.UTF_8)).toString();
    }
}

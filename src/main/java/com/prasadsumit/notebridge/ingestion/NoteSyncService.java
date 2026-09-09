package com.prasadsumit.notebridge.ingestion;

import com.prasadsumit.notebridge.persistence.*;
import com.prasadsumit.notebridge.review.ConflictDetectionService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

@Service
public class NoteSyncService {
    private static final int CHUNK_SIZE = 1_400;
    private static final Logger logger = LoggerFactory.getLogger(NoteSyncService.class);
    private final FileContentExtractor extractor;
    private final IndexedDocumentRepository documents;
    private final NoteChunkRepository chunks;
    private final ConflictReviewRepository conflicts;
    private final ConflictDetectionService conflictDetection;
    private final VectorStore vectorStore;

    public NoteSyncService(FileContentExtractor extractor, IndexedDocumentRepository documents, NoteChunkRepository chunks,
                           ConflictReviewRepository conflicts, ConflictDetectionService conflictDetection, VectorStore vectorStore) {
        this.extractor = extractor;
        this.documents = documents;
        this.chunks = chunks;
        this.conflicts = conflicts;
        this.conflictDetection = conflictDetection;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public SyncResult sync(List<SelectedFile> selectedFiles) throws IOException {
        long syncStartedAt = System.nanoTime();
        int[] counts = new int[4];
        logger.info("source-sync stage=start files={}", selectedFiles.size());
        try {
            for (SelectedFile selectedFile : selectedFiles) {
                if (!extractor.supports(selectedFile.file().getOriginalFilename())) continue;
                String relativePath = selectedFile.relativePath();
                long fileStartedAt = System.nanoTime();
                try (var input = selectedFile.file().getInputStream()) {
                    long extractionStartedAt = System.nanoTime();
                    String content = extractor.extract(input, relativePath);
                    long extractionMs = elapsedMillis(extractionStartedAt);
                    if (content.isBlank()) {
                        counts[3]++;
                        logger.info("source-sync stage=file file={} outcome=blank extraction_ms={}", relativePath, extractionMs);
                        continue;
                    }
                    long lookupStartedAt = System.nanoTime();
                    String hash = sha256(content);
                    Optional<IndexedDocument> existing = documents.findByRelativePath(relativePath);
                    long lookupMs = elapsedMillis(lookupStartedAt);
                    if (existing.isPresent() && existing.get().getContentHash().equals(hash)) {
                        counts[3]++;
                        logger.info("source-sync stage=file file={} outcome=unchanged chars={} extraction_ms={} lookup_ms={} total_ms={}",
                                relativePath, content.length(), extractionMs, lookupMs, elapsedMillis(fileStartedAt));
                        continue;
                    }
                    IndexedDocument document = existing.orElseGet(IndexedDocument::new);
                    long replacementMs = 0;
                    if (existing.isPresent()) {
                        long replacementStartedAt = System.nanoTime();
                        conflicts.deleteByChunkDocument(document);
                        deleteVectors(document);
                        chunks.deleteByDocument(document);
                        replacementMs = elapsedMillis(replacementStartedAt);
                        counts[1]++;
                    } else {
                        document.setRelativePath(relativePath);
                        counts[0]++;
                    }
                    document.setFormat(extension(relativePath));
                    document.setContentHash(hash);
                    document.setSourceModifiedAt(selectedFile.modifiedAt());
                    document.setIndexedAt(Instant.now());
                    document.setContent(content);
                    // Spring Data may return a managed copy when saving a new
                    // entity. Chunks must reference that persisted document, not
                    // the transient instance created above.
                    long documentSaveStartedAt = System.nanoTime();
                    document = documents.save(document);
                    long documentSaveMs = elapsedMillis(documentSaveStartedAt);
                    IndexingMetrics indexing = indexChunks(document, content);
                    logger.info("source-sync stage=file file={} outcome={} chars={} chunks={} extraction_ms={} lookup_ms={} replacement_ms={} document_save_ms={} chunk_save_ms={} embedding_ms={} indexing_ms={} total_ms={}",
                            relativePath, existing.isPresent() ? "updated" : "added", content.length(), indexing.chunkCount(), extractionMs,
                            lookupMs, replacementMs, documentSaveMs, indexing.chunkSaveMs(), indexing.embeddingMs(), indexing.totalMs(),
                            elapsedMillis(fileStartedAt));
                }
            }
            long conflictDetectionStartedAt = System.nanoTime();
            int flagged = (counts[0] + counts[1]) == 0 ? 0 : conflictDetection.analyseEligibleChunks();
            logger.info("source-sync stage=conflict-review invoked={} flagged={} duration_ms={}", counts[0] + counts[1] > 0, flagged,
                    elapsedMillis(conflictDetectionStartedAt));
            return new SyncResult(counts[0], counts[1], counts[2], counts[3], flagged);
        } finally {
            logger.info("source-sync stage=complete added={} updated={} unchanged={} duration_ms={}", counts[0], counts[1], counts[3],
                    elapsedMillis(syncStartedAt));
        }
    }

    public List<IndexedDocument> documents() { return documents.findAll().stream().sorted(Comparator.comparing(IndexedDocument::getRelativePath)).toList(); }

    public record SelectedFile(String relativePath, Instant modifiedAt, MultipartFile file) {}

    private IndexingMetrics indexChunks(IndexedDocument document, String content) {
        long indexingStartedAt = System.nanoTime();
        long chunkSaveMs = 0;
        long embeddingMs = 0;
        int sequence = 0;
        for (int from = 0; from < content.length();) {
            int to = Math.min(content.length(), from + CHUNK_SIZE);
            if (to < content.length()) { int boundary = content.lastIndexOf('\n', to); if (boundary > from + CHUNK_SIZE / 2) to = boundary; }
            NoteChunk chunk = new NoteChunk();
            chunk.setDocument(document); chunk.setSequenceNumber(sequence++); chunk.setContent(content.substring(from, to).trim()); chunk.setExcluded(false);
            long chunkSaveStartedAt = System.nanoTime();
            chunk = chunks.save(chunk);
            chunkSaveMs += elapsedMillis(chunkSaveStartedAt);
            long embeddingStartedAt = System.nanoTime();
            vectorStore.add(List.of(new Document(vectorId(chunk.getId()), chunk.getContent(), Map.of("chunkId", chunk.getId(), "path", document.getRelativePath()))));
            embeddingMs += elapsedMillis(embeddingStartedAt);
            // This loop controls its cursor explicitly so a newline boundary
            // cannot skip or repeat any source content.
            from = to;
        }
        return new IndexingMetrics(sequence, chunkSaveMs, embeddingMs, elapsedMillis(indexingStartedAt));
    }

    private void deleteVectors(IndexedDocument document) {
        List<String> ids = chunks.findByDocument(document).stream().map(chunk -> vectorId(chunk.getId())).toList();
        if (!ids.isEmpty()) vectorStore.delete(ids);
    }

    private static String extension(String path) { return path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT); }
    private static String vectorId(Long chunkId) {
        return UUID.nameUUIDFromBytes(("notebridge-chunk-" + chunkId).getBytes(StandardCharsets.UTF_8)).toString();
    }
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private record IndexingMetrics(int chunkCount, long chunkSaveMs, long embeddingMs, long totalMs) { }
}

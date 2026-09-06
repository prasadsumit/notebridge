package com.prasadsumit.notebridge.ingestion;

import com.prasadsumit.notebridge.persistence.*;
import com.prasadsumit.notebridge.review.ConflictDetectionService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Service
public class NoteSyncService {
    private static final int CHUNK_SIZE = 1_400;
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
        Set<String> discovered = new HashSet<>();
        int[] counts = new int[4];
        for (SelectedFile selectedFile : selectedFiles) {
            if (!extractor.supports(selectedFile.file().getOriginalFilename())) continue;
            String relativePath = selectedFile.relativePath();
            discovered.add(relativePath);
            try (var input = selectedFile.file().getInputStream()) {
                String content = extractor.extract(input, relativePath);
                if (content.isBlank()) { counts[3]++; continue; }
                String hash = sha256(content);
                Optional<IndexedDocument> existing = documents.findByRelativePath(relativePath);
                if (existing.isPresent() && existing.get().getContentHash().equals(hash)) { counts[3]++; continue; }
                IndexedDocument document = existing.orElseGet(IndexedDocument::new);
                if (existing.isPresent()) { conflicts.deleteByChunkDocument(document); deleteVectors(document); chunks.deleteByDocument(document); counts[1]++; }
                else { document.setRelativePath(relativePath); counts[0]++; }
                document.setFormat(extension(relativePath));
                document.setContentHash(hash);
                document.setSourceModifiedAt(selectedFile.modifiedAt());
                document.setIndexedAt(Instant.now());
                document.setContent(content);
                // Spring Data may return a managed copy when saving a new
                // entity. Chunks must reference that persisted document, not
                // the transient instance created above.
                document = documents.save(document);
                indexChunks(document, content);
            }
        }
        for (IndexedDocument document : documents.findAll()) {
            if (!discovered.contains(document.getRelativePath())) { conflicts.deleteByChunkDocument(document); deleteVectors(document); documents.delete(document); counts[2]++; }
        }
        int flagged = (counts[0] + counts[1]) == 0 ? 0 : conflictDetection.analyseEligibleChunks();
        return new SyncResult(counts[0], counts[1], counts[2], counts[3], flagged);
    }

    public List<IndexedDocument> documents() { return documents.findAll().stream().sorted(Comparator.comparing(IndexedDocument::getRelativePath)).toList(); }

    public record SelectedFile(String relativePath, Instant modifiedAt, MultipartFile file) {}

    private void indexChunks(IndexedDocument document, String content) {
        int sequence = 0;
        for (int from = 0; from < content.length();) {
            int to = Math.min(content.length(), from + CHUNK_SIZE);
            if (to < content.length()) { int boundary = content.lastIndexOf('\n', to); if (boundary > from + CHUNK_SIZE / 2) to = boundary; }
            NoteChunk chunk = new NoteChunk();
            chunk.setDocument(document); chunk.setSequenceNumber(sequence++); chunk.setContent(content.substring(from, to).trim()); chunk.setExcluded(false);
            chunk = chunks.save(chunk);
            vectorStore.add(List.of(new Document(vectorId(chunk.getId()), chunk.getContent(), Map.of("chunkId", chunk.getId(), "path", document.getRelativePath()))));
            // This loop controls its cursor explicitly so a newline boundary
            // cannot skip or repeat any source content.
            from = to;
        }
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
}

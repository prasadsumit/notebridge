package com.prasadsumit.notebridge.review;

import com.prasadsumit.notebridge.ai.AiProvider;
import com.prasadsumit.notebridge.ai.ConflictAssessment;
import com.prasadsumit.notebridge.model.ConflictStatus;
import com.prasadsumit.notebridge.persistence.ConflictReview;
import com.prasadsumit.notebridge.persistence.ConflictReviewRepository;
import com.prasadsumit.notebridge.persistence.NoteChunk;
import com.prasadsumit.notebridge.persistence.NoteChunkRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ConflictDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(ConflictDetectionService.class);
    private final NoteChunkRepository chunks;
    private final ConflictReviewRepository reviews;
    private final AiProvider provider;

    public ConflictDetectionService(NoteChunkRepository chunks, ConflictReviewRepository reviews, AiProvider provider) {
        this.chunks = chunks;
        this.reviews = reviews;
        this.provider = provider;
    }

    @Transactional
    public int analyseEligibleChunks() {
        long analysisStartedAt = System.nanoTime();
        long candidateQueryStartedAt = System.nanoTime();
        List<NoteChunk> candidates = chunks.findByExcludedFalse().stream().limit(80).toList();
        long candidateQueryMs = elapsedMillis(candidateQueryStartedAt);
        long modelMs = 0;
        long persistenceMs = 0;
        int flagged = 0;
        for (NoteChunk candidate : candidates.stream().limit(15).toList()) {
            long modelStartedAt = System.nanoTime();
            ConflictAssessment assessment = provider.assessClaim(excerpt(candidate));
            modelMs += elapsedMillis(modelStartedAt);
            if (assessment.incorrect() && assessment.confidence() >= 60) {
                long persistenceStartedAt = System.nanoTime();
                flag(candidate, assessment);
                persistenceMs += elapsedMillis(persistenceStartedAt);
                flagged++;
            }
        }
        logger.info("conflict-review stage=complete candidates={} assessed={} flagged={} candidate_query_ms={} model_ms={} persistence_ms={} total_ms={}",
                candidates.size(), Math.min(candidates.size(), 15), flagged, candidateQueryMs, modelMs, persistenceMs,
                elapsedMillis(analysisStartedAt));
        return flagged;
    }

    private void flag(NoteChunk chunk, ConflictAssessment assessment) {
        chunk.setExcluded(true);
        chunks.save(chunk);
        ConflictReview review = new ConflictReview();
        review.setChunk(chunk);
        review.setStatus(ConflictStatus.OPEN);
        review.setConflictType(assessment.conflictType());
        review.setConfidence(assessment.confidence());
        review.setRationale(assessment.rationale());
        review.setCreatedAt(Instant.now());
        reviews.save(review);
    }

    private AiProvider.SourceExcerpt excerpt(NoteChunk chunk) {
        return new AiProvider.SourceExcerpt(chunk.getId(), chunk.getDocument().getRelativePath() + " (section " + (chunk.getSequenceNumber() + 1) + ")", chunk.getContent());
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

}

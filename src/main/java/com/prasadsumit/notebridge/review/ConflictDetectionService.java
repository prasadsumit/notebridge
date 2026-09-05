package com.prasadsumit.notebridge.review;

import com.prasadsumit.notebridge.ai.AiProvider;
import com.prasadsumit.notebridge.ai.ConflictAssessment;
import com.prasadsumit.notebridge.model.ConflictStatus;
import com.prasadsumit.notebridge.persistence.ConflictReview;
import com.prasadsumit.notebridge.persistence.ConflictReviewRepository;
import com.prasadsumit.notebridge.persistence.NoteChunk;
import com.prasadsumit.notebridge.persistence.NoteChunkRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ConflictDetectionService {
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
        List<NoteChunk> candidates = chunks.findByExcludedFalse().stream().limit(80).toList();
        int flagged = 0;
        for (NoteChunk candidate : candidates.stream().limit(15).toList()) {
            ConflictAssessment assessment = provider.assessClaim(excerpt(candidate));
            if (assessment.incorrect() && assessment.confidence() >= 60) {
                flag(candidate, assessment);
                flagged++;
            }
        }
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

}

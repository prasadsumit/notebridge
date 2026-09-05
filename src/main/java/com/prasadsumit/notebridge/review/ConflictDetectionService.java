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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        int calls = 0;
        for (int left = 0; left < candidates.size() && calls < 15; left++)
            for (int right = left + 1; right < candidates.size() && calls < 15; right++) {
                NoteChunk first = candidates.get(left), second = candidates.get(right);
                if (overlap(first.getContent(), second.getContent()) < 5) continue;
                calls++;
                ConflictAssessment assessment = provider.assessConflict(excerpt(first), excerpt(second));
                if (assessment.conflict() && assessment.confidence() >= 60) {
                    flag(first, assessment);
                    flag(second, assessment);
                    flagged += 2;
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

    private int overlap(String first, String second) {
        Set<String> words = words(first);
        words.retainAll(words(second));
        return words.size();
    }

    private Set<String> words(String content) {
        Set<String> result = new HashSet<>();
        for (String word : content.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
            if (word.length() > 4) result.add(word);
        return result;
    }
}

package com.prasadsumit.notebridge.review;

import com.prasadsumit.notebridge.model.ConflictStatus;
import com.prasadsumit.notebridge.persistence.ConflictReview;
import com.prasadsumit.notebridge.persistence.ConflictReviewRepository;
import com.prasadsumit.notebridge.persistence.NoteChunkRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConflictReviewService {
    private final ConflictReviewRepository reviews;
    private final NoteChunkRepository chunks;
    public ConflictReviewService(ConflictReviewRepository reviews, NoteChunkRepository chunks) { this.reviews = reviews; this.chunks = chunks; }
    public List<ConflictReview> openReviews() { return reviews.findByStatusOrderByCreatedAtDesc(ConflictStatus.OPEN); }
    @Transactional public void dismiss(long id) { update(id, ConflictStatus.DISMISSED, false); }
    @Transactional public void resolve(long id) { update(id, ConflictStatus.RESOLVED, false); }
    private void update(long id, ConflictStatus status, boolean excluded) {
        ConflictReview review = reviews.findById(id).orElseThrow();
        review.setStatus(status); review.getChunk().setExcluded(excluded); chunks.save(review.getChunk());
    }
}

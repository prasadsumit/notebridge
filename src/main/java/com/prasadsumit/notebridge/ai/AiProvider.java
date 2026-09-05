package com.prasadsumit.notebridge.ai;

import com.prasadsumit.notebridge.quiz.QuizRequest;

import java.util.List;

/**
 * Provider boundary: domain services never depend on a vendor-specific SDK.
 */
public interface AiProvider {
    GeneratedQuiz generateQuiz(QuizRequest request, List<SourceExcerpt> excerpts);

    ConflictAssessment assessClaim(SourceExcerpt excerpt);

    record SourceExcerpt(Long chunkId, String citation, String content) {
    }
}

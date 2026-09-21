package com.prasadsumit.notebridge.quiz;

import com.prasadsumit.notebridge.ai.GeneratedQuiz;
import com.prasadsumit.notebridge.ai.AiProvider;
import com.prasadsumit.notebridge.model.Difficulty;
import com.prasadsumit.notebridge.model.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuizValidationServiceTests {
    private final QuizValidationService validator = new QuizValidationService();
    private final QuizRequest request = new QuizRequest(Difficulty.EASY, QuestionType.MULTIPLE_CHOICE, 1, "", null, List.of(1L));
    private final List<AiProvider.SourceExcerpt> excerpts = List.of(new AiProvider.SourceExcerpt(7L, "note.md (section 1)", "Relevant text"));

    @Test void acceptsGroundedSingleAnswerQuestion() {
        assertDoesNotThrow(() -> validate(question(List.of(0), List.of(citation(7L, "relevant concept")))));
    }

    @Test void rejectsQuestionWithoutCitation() {
        assertThrows(IllegalArgumentException.class, () -> validate(question(List.of(0), List.of())));
    }

    @Test void rejectsCitationToUnprovidedChunk() {
        assertThrows(IllegalArgumentException.class, () -> validate(question(List.of(0), List.of(citation(99L, "made up location")))));
    }

    @Test void rejectsCitationWithoutSearchHint() {
        assertThrows(IllegalArgumentException.class, () -> validate(question(List.of(0), List.of(citation(7L, " ")))));
    }

    @Test void rejectsMultipleCorrectAnswersForMcq() {
        assertThrows(IllegalArgumentException.class, () -> validate(question(List.of(0, 1), List.of(citation(7L, "relevant concept")))));
    }

    private void validate(GeneratedQuiz.GeneratedQuestion question) {
        validator.validate(new GeneratedQuiz("Quiz", List.of(question)), request, excerpts);
    }

    private GeneratedQuiz.GeneratedCitation citation(Long id, String hint) {
        return new GeneratedQuiz.GeneratedCitation(id, hint);
    }

    private GeneratedQuiz.GeneratedQuestion question(List<Integer> answers, List<GeneratedQuiz.GeneratedCitation> citations) {
        return new GeneratedQuiz.GeneratedQuestion("Question?", List.of("One", "Two"), answers, "Explanation", null, citations);
    }
}

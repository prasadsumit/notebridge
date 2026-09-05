package com.prasadsumit.notebridge.quiz;

import com.prasadsumit.notebridge.ai.GeneratedQuiz;
import com.prasadsumit.notebridge.model.Difficulty;
import com.prasadsumit.notebridge.model.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuizValidationServiceTests {
    private final QuizValidationService validator = new QuizValidationService();
    private final QuizRequest request = new QuizRequest(Difficulty.EASY, QuestionType.MULTIPLE_CHOICE, 1, "", null);
    @Test void acceptsGroundedSingleAnswerQuestion() { assertDoesNotThrow(() -> validator.validate(new GeneratedQuiz("Quiz", List.of(question(List.of(0), List.of("note.md")))), request)); }
    @Test void rejectsQuestionWithoutCitation() { assertThrows(IllegalArgumentException.class, () -> validator.validate(new GeneratedQuiz("Quiz", List.of(question(List.of(0), List.of()))), request)); }
    @Test void rejectsMultipleCorrectAnswersForMcq() { assertThrows(IllegalArgumentException.class, () -> validator.validate(new GeneratedQuiz("Quiz", List.of(question(List.of(0, 1), List.of("note.md")))), request)); }
    private GeneratedQuiz.GeneratedQuestion question(List<Integer> answers, List<String> citations) { return new GeneratedQuiz.GeneratedQuestion("Question?", List.of("One", "Two"), answers, "Explanation", null, citations); }
}

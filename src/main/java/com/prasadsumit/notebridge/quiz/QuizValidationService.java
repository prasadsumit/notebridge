package com.prasadsumit.notebridge.quiz;

import com.prasadsumit.notebridge.ai.GeneratedQuiz;
import com.prasadsumit.notebridge.model.QuestionType;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class QuizValidationService {
    public void validate(GeneratedQuiz quiz, QuizRequest request) {
        if (quiz == null || quiz.questions() == null || quiz.questions().size() != request.questionCount())
            throw new IllegalArgumentException("The provider did not return the requested number of questions.");
        Set<String> prompts = new HashSet<>();
        for (GeneratedQuiz.GeneratedQuestion question : quiz.questions()) {
            if (question.prompt() == null || question.prompt().isBlank() || !prompts.add(question.prompt().trim().toLowerCase()))
                throw new IllegalArgumentException("The provider returned an invalid or duplicate question.");
            if (question.options() == null || question.options().size() < 2 || question.correctOptionIndexes() == null || question.correctOptionIndexes().isEmpty())
                throw new IllegalArgumentException("Each question must contain options and an answer.");
            if (request.questionType() == QuestionType.MULTIPLE_CHOICE && question.correctOptionIndexes().size() != 1)
                throw new IllegalArgumentException("Multiple-choice questions require exactly one answer.");
            if (question.correctOptionIndexes().stream().anyMatch(i -> i == null || i < 0 || i >= question.options().size()))
                throw new IllegalArgumentException("A question has an invalid answer index.");
            if (question.citations() == null || question.citations().isEmpty())
                throw new IllegalArgumentException("Every question needs a note citation.");
        }
    }
}

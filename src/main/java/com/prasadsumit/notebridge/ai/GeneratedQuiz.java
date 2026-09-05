package com.prasadsumit.notebridge.ai;

import java.util.List;

public record GeneratedQuiz(String title, List<GeneratedQuestion> questions) {
    public record GeneratedQuestion(String prompt, List<String> options, List<Integer> correctOptionIndexes,
                                    String explanation, String additionalLearningContext, List<String> citations) { }
}

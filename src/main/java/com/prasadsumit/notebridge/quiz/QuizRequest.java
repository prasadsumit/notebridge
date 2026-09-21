package com.prasadsumit.notebridge.quiz;

import com.prasadsumit.notebridge.model.Difficulty;
import com.prasadsumit.notebridge.model.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuizRequest(@NotNull Difficulty difficulty, @NotNull QuestionType questionType,
                          @Min(1) @Max(50) int questionCount, String topic,
                          @Min(1) @Max(360) Integer timeLimitMinutes, List<Long> sourceIds) {
    public String topicOrDefault() {
        return topic == null || topic.isBlank() ? "the supplied notes" : topic.trim();
    }
}

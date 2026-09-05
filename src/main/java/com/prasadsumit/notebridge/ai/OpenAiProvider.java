package com.prasadsumit.notebridge.ai;

import com.prasadsumit.notebridge.quiz.QuizRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class OpenAiProvider implements AiProvider {
    private final ChatClient chatClient;
    OpenAiProvider(ChatClient.Builder builder) { this.chatClient = builder.build(); }

    @Override
    public GeneratedQuiz generateQuiz(QuizRequest request, List<SourceExcerpt> excerpts) {
        String evidence = excerpts.stream().map(excerpt -> "[" + excerpt.citation() + "]\n" + excerpt.content())
                .reduce("", (left, right) -> left + "\n\n" + right);
        String prompt = """
                Create an evidence-grounded practice quiz. Use only the supplied note excerpts as the basis for correct answers.
                A question must cite one or more supplied file citations. Do not invent citations. Additional learning context is optional,
                must be clearly separate, and cannot be needed to answer correctly. %s question format is requested.
                Difficulty: %s. Question count: %d. Topic: %s.

                EVIDENCE:
                %s
                """.formatted(request.questionType(), request.difficulty(), request.questionCount(), request.topicOrDefault(), evidence);
        return chatClient.prompt().user(prompt).call().entity(GeneratedQuiz.class,
                spec -> spec.useProviderStructuredOutput().validateSchema());
    }

    @Override
    public ConflictAssessment assessConflict(SourceExcerpt first, SourceExcerpt second) {
        String prompt = """
                Assess whether these two note excerpts make incompatible factual claims. Be conservative: similarity, nuance,
                omissions, and different scopes are not conflicts. This is only a suspected-conflict review for the note owner,
                not external verification. If they conflict, give a brief neutral rationale and confidence 0-100.
                FIRST (%s): %s
                SECOND (%s): %s
                """.formatted(first.citation(), first.content(), second.citation(), second.content());
        return chatClient.prompt().user(prompt).call().entity(ConflictAssessment.class,
                spec -> spec.useProviderStructuredOutput().validateSchema());
    }
}

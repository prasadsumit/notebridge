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

                Make every question and every answer option descriptive at every difficulty. Include the
                relevant subject, condition, or scenario in the question rather than relying on vague prompts such as
                "What is this?" or "Which is correct?". Write options as complete, specific alternatives. Increase difficulty through reasoning,
                comparison, application, and subtle but evidence-supported distinctions—not by making the wording shorter,
                more ambiguous, or less informative.
                Do not systematically place correct answers in the first option. Vary their positions across the quiz.

                EVIDENCE:
                %s
                """.formatted(request.questionType(), request.difficulty(), request.questionCount(), request.topicOrDefault(), evidence);
        return chatClient.prompt().user(prompt).call().entity(GeneratedQuiz.class,
                spec -> spec.useProviderStructuredOutput().validateSchema());
    }

    @Override
    public ConflictAssessment assessClaim(SourceExcerpt excerpt) {
        String prompt = """
                Fact-check the factual claims in this note excerpt using your general knowledge. Identify a claim as incorrect only
                when it is materially false or expresses a clear misconception. Do not flag subjective statements, incomplete but
                not misleading explanations, opinions, stylistic choices, or claims you cannot verify confidently. For example,
                saying Java is the programming language used with HTML and CSS for browser interactivity is a misconception: that
                role is generally JavaScript. This is a review suggestion for the note owner, not an authoritative correction.
                If a material error exists, state the specific correction briefly and give confidence 0-100.
                EXCERPT (%s): %s
                """.formatted(excerpt.citation(), excerpt.content());
        return chatClient.prompt().user(prompt).call().entity(ConflictAssessment.class,
                spec -> spec.useProviderStructuredOutput().validateSchema());
    }
}

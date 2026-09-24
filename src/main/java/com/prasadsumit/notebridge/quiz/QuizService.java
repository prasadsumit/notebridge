package com.prasadsumit.notebridge.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasadsumit.notebridge.ai.AiProvider;
import com.prasadsumit.notebridge.ai.GeneratedQuiz;
import com.prasadsumit.notebridge.model.Difficulty;
import com.prasadsumit.notebridge.persistence.*;
import com.prasadsumit.notebridge.session.ActivityTracker;
import com.prasadsumit.notebridge.session.ActivityType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QuizService {
    public record CompletedQuiz(Quiz quiz, QuizAttempt attempt) {
    }

    private record ShuffledOptions(List<String> options, Set<Integer> correctIndexes) {
    }

    private final NoteChunkRepository chunks;
    private final IndexedDocumentRepository documents;
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final AiProvider provider;
    private final QuizValidationService validator;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;
    private final ActivityTracker activityTracker;
    private final TokenCountEstimator tokenEstimator = new JTokkitTokenCountEstimator();

    public QuizService(NoteChunkRepository chunks, IndexedDocumentRepository documents, QuizRepository quizzes, QuizAttemptRepository attempts, AiProvider provider, QuizValidationService validator, ObjectMapper objectMapper, VectorStore vectorStore, ActivityTracker activityTracker) {
        this.chunks = chunks;
        this.documents = documents;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.provider = provider;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
        this.activityTracker = activityTracker;
    }

    @Transactional
    public Quiz generate(QuizRequest request) {
        Set<Long> selectedSourceIds = request.sourceIds() == null ? Set.of() : new HashSet<>(request.sourceIds());
        if (selectedSourceIds.isEmpty() || selectedSourceIds.contains(null))
            throw new IllegalArgumentException("Select at least one source for the quiz.");
        List<IndexedDocument> selectedSources = documents.findAllById(selectedSourceIds);
        if (selectedSources.size() != selectedSourceIds.size())
            throw new IllegalArgumentException("A selected source is no longer available. Choose your sources again.");

        int limit = switch (request.difficulty()) {
            case EASY -> 8;
            case MEDIUM -> 16;
            case HARD -> 30;
        };
        List<NoteChunk> eligible = chunks.findByExcludedFalse().stream()
                .filter(chunk -> selectedSourceIds.contains(chunk.getDocument().getId()))
                .toList();
        if (eligible.isEmpty())
            throw new IllegalStateException("No eligible note excerpts are available in the selected sources.");

        SessionActivity activity = activityTracker.start(ActivityType.QUIZ_CREATED,
                "Creating %d-question %s quiz".formatted(request.questionCount(), request.difficulty().name().toLowerCase(Locale.ROOT)));

        try {

        var sourcePaths = selectedSources.stream().map(IndexedDocument::getRelativePath).map(path -> (Object) path).toList();
        var sourceFilter = new FilterExpressionBuilder().in("path", sourcePaths).build();
        activityTracker.addTokens(tokenEstimator.estimate(request.topicOrDefault()), 0);
        Set<Long> retrievedIds = vectorStore.similaritySearch(SearchRequest.builder().query(request.topicOrDefault())
                        .topK(limit).similarityThreshold(0.0).filterExpression(sourceFilter).build()).stream()
                .map(Document::getMetadata)
                .map(metadata -> metadata.get("chunkId"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .collect(java.util.stream.Collectors.toSet());

        List<AiProvider.SourceExcerpt> excerpts = eligible.stream()
                .filter(chunk -> retrievedIds.isEmpty() || retrievedIds.contains(chunk.getId()))
                .limit(limit)
                .map(chunk -> new AiProvider.SourceExcerpt(chunk.getId(), CitationLocator.describe(chunk), chunk.getContent()))
                .toList();

        if (excerpts.isEmpty())
            throw new IllegalStateException("No eligible note excerpts are available. Sync your notes or resolve the review queue first.");

        GeneratedQuiz generated = provider.generateQuiz(request, excerpts);

        validator.validate(generated, request, excerpts);
        Map<Long, AiProvider.SourceExcerpt> excerptsById = new HashMap<>();
        excerpts.forEach(excerpt -> excerptsById.put(excerpt.chunkId(), excerpt));

        Quiz quiz = new Quiz();
        quiz.setTitle(generated.title() == null || generated.title().isBlank() ? "Practice quiz" : generated.title());
        quiz.setDifficulty(request.difficulty());
        quiz.setQuestionType(request.questionType());
        quiz.setTimeLimitMinutes(request.timeLimitMinutes());
        quiz.setCreatedAt(Instant.now());

        for (int i = 0; i < generated.questions().size(); i++) {
            GeneratedQuiz.GeneratedQuestion source = generated.questions().get(i);
            QuizQuestion target = new QuizQuestion();
            target.setQuiz(quiz);
            target.setPosition(i + 1);
            target.setPrompt(source.prompt());
            ShuffledOptions shuffled = shuffleOptions(source);
            target.setOptions(shuffled.options());
            target.setCorrectOptionIndexes(shuffled.correctIndexes());
            target.setExplanation(source.explanation());
            target.setAdditionalLearningContext(source.additionalLearningContext());
            target.setCitations(source.citations().stream()
                    .map(citation -> excerptsById.get(citation.chunkId()).citation() + " — " + citation.searchHint().trim())
                    .toList());
            quiz.getQuestions().add(target);
        }
        Quiz saved = quizzes.save(quiz);
        activityTracker.complete(activity, "%s · %d questions · %d sources"
                .formatted(saved.getTitle(), saved.getQuestions().size(), selectedSources.size()));
        return saved;
        } catch (RuntimeException exception) {
            activityTracker.fail(activity, exception);
            throw exception;
        }
    }

    private ShuffledOptions shuffleOptions(GeneratedQuiz.GeneratedQuestion question) {
        List<Integer> originalIndexes = new ArrayList<>();
        for (int index = 0; index < question.options().size(); index++) originalIndexes.add(index);
        Collections.shuffle(originalIndexes, ThreadLocalRandom.current());

        List<String> shuffledOptions = new ArrayList<>();
        Set<Integer> correctIndexes = new HashSet<>();
        for (int newIndex = 0; newIndex < originalIndexes.size(); newIndex++) {
            int originalIndex = originalIndexes.get(newIndex);
            shuffledOptions.add(question.options().get(originalIndex));
            if (question.correctOptionIndexes().contains(originalIndex)) correctIndexes.add(newIndex);
        }
        return new ShuffledOptions(shuffledOptions, correctIndexes);
    }

    public Quiz get(long id) {
        return quizzes.findById(id).orElseThrow();
    }

    public List<CompletedQuiz> history() {
        return attempts.findAllByOrderBySubmittedAtDesc().stream()
                .map(attempt -> new CompletedQuiz(get(attempt.getQuiz().getId()), attempt))
                .toList();
    }

    public QuizAttempt getAttempt(long id) {
        return attempts.findById(id).orElseThrow();
    }

    public Optional<QuizAttempt> completedAttempt(long quizId) {
        return attempts.findFirstByQuizIdOrderBySubmittedAtDesc(quizId);
    }

    public Map<Long, Set<Integer>> selectedAnswers(QuizAttempt attempt) {
        try {
            Map<String, List<Integer>> persisted = objectMapper.readValue(attempt.getSelectedAnswersJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {
                    });
            Map<Long, Set<Integer>> selected = new HashMap<>();
            persisted.forEach((questionId, answers) -> selected.put(Long.valueOf(questionId), new HashSet<>(answers)));
            return selected;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read saved answers", exception);
        }
    }

    @Transactional
    public QuizAttempt submit(long quizId, Map<Long, Set<Integer>> answers) {
        Quiz quiz = get(quizId);
        if (completedAttempt(quizId).isPresent())
            throw new IllegalStateException("This quiz has already been completed.");
        int score = 0;
        for (QuizQuestion question : quiz.getQuestions())
            if (question.getCorrectOptionIndexes().equals(answers.getOrDefault(question.getId(), Set.of()))) score++;
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setSubmittedAt(Instant.now());
        attempt.setScore(score);
        attempt.setPossibleScore(quiz.getQuestions().size());
        try {
            attempt.setSelectedAnswersJson(objectMapper.writeValueAsString(answers));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not save answers", exception);
        }
        return attempts.save(attempt);
    }
}

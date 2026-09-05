package com.prasadsumit.notebridge.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasadsumit.notebridge.ai.AiProvider;
import com.prasadsumit.notebridge.ai.GeneratedQuiz;
import com.prasadsumit.notebridge.model.Difficulty;
import com.prasadsumit.notebridge.persistence.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.Instant;
import java.util.*;

@Service
public class QuizService {
    private final NoteChunkRepository chunks;
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final AiProvider provider;
    private final QuizValidationService validator;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;

    public QuizService(NoteChunkRepository chunks, QuizRepository quizzes, QuizAttemptRepository attempts, AiProvider provider, QuizValidationService validator, ObjectMapper objectMapper, VectorStore vectorStore) {
        this.chunks = chunks;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.provider = provider;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public Quiz generate(QuizRequest request) {
        int limit = switch (request.difficulty()) {
            case EASY -> 8;
            case MEDIUM -> 16;
            case HARD -> 30;
        };
        Set<Long> retrievedIds = vectorStore.similaritySearch(SearchRequest.builder().query(request.topicOrDefault()).topK(limit).similarityThreshold(0.0).build()).stream()
                .map(Document::getMetadata).map(metadata -> metadata.get("chunkId")).filter(Number.class::isInstance).map(Number.class::cast).map(Number::longValue).collect(java.util.stream.Collectors.toSet());
        List<NoteChunk> eligible = chunks.findByExcludedFalse();
        List<AiProvider.SourceExcerpt> excerpts = eligible.stream().filter(chunk -> retrievedIds.isEmpty() || retrievedIds.contains(chunk.getId())).limit(limit)
                .map(chunk -> new AiProvider.SourceExcerpt(chunk.getId(), chunk.getDocument().getRelativePath() + " (section " + (chunk.getSequenceNumber() + 1) + ")", chunk.getContent())).toList();
        if (excerpts.isEmpty())
            throw new IllegalStateException("No eligible note excerpts are available. Sync your notes or resolve the review queue first.");
        GeneratedQuiz generated = provider.generateQuiz(request, excerpts);
        validator.validate(generated, request);
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
            target.setOptions(new ArrayList<>(source.options()));
            target.setCorrectOptionIndexes(new HashSet<>(source.correctOptionIndexes()));
            target.setExplanation(source.explanation());
            target.setAdditionalLearningContext(source.additionalLearningContext());
            target.setCitations(new ArrayList<>(source.citations()));
            quiz.getQuestions().add(target);
        }
        return quizzes.save(quiz);
    }

    public Quiz get(long id) {
        return quizzes.findById(id).orElseThrow();
    }

    public List<Quiz> history() {
        return quizzes.findAll().stream().sorted(Comparator.comparing(Quiz::getCreatedAt).reversed()).toList();
    }

    public QuizAttempt getAttempt(long id) {
        return attempts.findById(id).orElseThrow();
    }

    @Transactional
    public QuizAttempt submit(long quizId, Map<Long, Set<Integer>> answers) {
        Quiz quiz = get(quizId);
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

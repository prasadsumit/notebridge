package com.prasadsumit.notebridge.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasadsumit.notebridge.ai.AiProvider;
import com.prasadsumit.notebridge.ai.GeneratedQuiz;
import com.prasadsumit.notebridge.model.Difficulty;
import com.prasadsumit.notebridge.model.QuestionType;
import com.prasadsumit.notebridge.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizServiceSourceSelectionTests {
    private final NoteChunkRepository chunks = mock(NoteChunkRepository.class);
    private final IndexedDocumentRepository documents = mock(IndexedDocumentRepository.class);
    private final QuizRepository quizzes = mock(QuizRepository.class);
    private final QuizAttemptRepository attempts = mock(QuizAttemptRepository.class);
    private final AiProvider provider = mock(AiProvider.class);
    private final VectorStore vectorStore = mock(VectorStore.class);
    private final QuizService service = new QuizService(chunks, documents, quizzes, attempts, provider,
            new QuizValidationService(), new ObjectMapper(), vectorStore);

    @Test
    void usesOnlySelectedSourceForQuizEvidence() {
        IndexedDocument selected = document(1L, "selected.md");
        IndexedDocument other = document(2L, "other.md");
        when(documents.findAllById(any())).thenReturn(List.of(selected));
        when(chunks.findByExcludedFalse()).thenReturn(List.of(chunk(11L, selected), chunk(22L, other)));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("selected-vector", "Selected content", Map.of("chunkId", 11L)),
                new Document("other-vector", "Other content", Map.of("chunkId", 22L))));
        when(provider.generateQuiz(any(), any())).thenAnswer(invocation -> {
            List<AiProvider.SourceExcerpt> excerpts = invocation.getArgument(1);
            assertEquals(1, excerpts.size());
            assertEquals(11L, excerpts.getFirst().chunkId());
            return new GeneratedQuiz("Quiz", List.of(new GeneratedQuiz.GeneratedQuestion(
                    "Question?", List.of("One", "Two"), List.of(0), "Explanation", null,
                    List.of(excerpts.getFirst().citation()))));
        });
        when(quizzes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate(request(List.of(1L)));

        var search = org.mockito.ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(search.capture());
        assertEquals(new FilterExpressionBuilder().in("path", "selected.md").build(),
                search.getValue().getFilterExpression());
    }

    @Test
    void rejectsUnknownAndEmptySourceSelections() {
        assertThrows(IllegalArgumentException.class, () -> service.generate(request(List.of())));
        assertThrows(IllegalArgumentException.class, () -> service.generate(request(List.of(99L))));
        verifyNoInteractions(vectorStore, provider);
    }

    private QuizRequest request(List<Long> sourceIds) {
        return new QuizRequest(Difficulty.EASY, QuestionType.MULTIPLE_CHOICE, 1, "", null, sourceIds);
    }

    private IndexedDocument document(long id, String path) {
        IndexedDocument document = new IndexedDocument();
        document.setId(id);
        document.setRelativePath(path);
        return document;
    }

    private NoteChunk chunk(long id, IndexedDocument document) {
        NoteChunk chunk = new NoteChunk();
        chunk.setId(id);
        chunk.setDocument(document);
        chunk.setContent("Content from " + document.getRelativePath());
        return chunk;
    }
}

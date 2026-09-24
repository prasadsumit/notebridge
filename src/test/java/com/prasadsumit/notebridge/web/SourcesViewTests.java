package com.prasadsumit.notebridge.web;

import com.prasadsumit.notebridge.ingestion.NoteSyncService;
import com.prasadsumit.notebridge.persistence.IndexedDocument;
import com.prasadsumit.notebridge.quiz.QuizService;
import com.prasadsumit.notebridge.review.ConflictReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyController.class)
class SourcesViewTests {
    @Autowired MockMvc mockMvc;
    @MockitoBean NoteSyncService syncService;
    @MockitoBean ConflictReviewService reviews;
    @MockitoBean QuizService quizzes;

    @Test
    void showsFileSizeBesideFileType() throws Exception {
        IndexedDocument document = new IndexedDocument();
        document.setId(1L);
        document.setRelativePath("chapter-one.md");
        document.setFormat("md");
        document.setSourceFileSize(1_536L);
        document.setIndexedAt(Instant.parse("2026-09-24T00:00:00Z"));
        when(syncService.documents()).thenReturn(List.of(document));

        mockMvc.perform(get("/sources"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("md · 1.5 KB · updated Sep 24, 2026")));
    }
}

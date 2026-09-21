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

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyController.class)
class QuizSetupViewTests {
    @Autowired MockMvc mockMvc;
    @MockitoBean NoteSyncService syncService;
    @MockitoBean ConflictReviewService reviews;
    @MockitoBean QuizService quizzes;

    @Test
    void showsUploadedSourceNamesInQuizPicker() throws Exception {
        IndexedDocument first = new IndexedDocument();
        first.setId(1L);
        first.setRelativePath("chapter-one.md");
        IndexedDocument second = new IndexedDocument();
        second.setId(2L);
        second.setRelativePath("chapter-two.md");
        when(syncService.documents()).thenReturn(List.of(first, second));

        mockMvc.perform(get("/quizzes/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"form-dropdown single-select-dropdown\"")))
                .andExpect(content().string(containsString("name=\"difficulty\"")))
                .andExpect(content().string(containsString("name=\"questionType\"")))
                .andExpect(content().string(containsString("chapter-one.md +1")))
                .andExpect(content().string(containsString("chapter-two.md")));
    }
}

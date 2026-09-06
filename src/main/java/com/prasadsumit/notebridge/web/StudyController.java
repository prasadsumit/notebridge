package com.prasadsumit.notebridge.web;

import com.prasadsumit.notebridge.ingestion.NoteSyncService;
import com.prasadsumit.notebridge.ingestion.SyncResult;
import com.prasadsumit.notebridge.model.Difficulty;
import com.prasadsumit.notebridge.model.QuestionType;
import com.prasadsumit.notebridge.quiz.QuizRequest;
import com.prasadsumit.notebridge.quiz.QuizService;
import com.prasadsumit.notebridge.review.ConflictReviewService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
public class StudyController {

    private final NoteSyncService syncService;
    private final ConflictReviewService reviews;
    private final QuizService quizzes;

    public StudyController(NoteSyncService syncService, ConflictReviewService reviews, QuizService quizzes) {
        this.syncService = syncService;
        this.reviews = reviews;
        this.quizzes = quizzes;
    }

    @GetMapping("/sources")
    String sources(Model model) {
        model.addAttribute("documents", syncServiceDocuments());
        return "sources";
    }

    @PostMapping(path = "/sources/sync", consumes = "multipart/form-data")
    String sync(@RequestParam("files") java.util.List<MultipartFile> files,
                @RequestParam("relativePaths") java.util.List<String> relativePaths,
                @RequestParam("modifiedAts") java.util.List<Long> modifiedAts,
                RedirectAttributes flash) {
        try {
            if (files.size() != relativePaths.size() || files.size() != modifiedAts.size()) {
                throw new IllegalArgumentException("The selected files could not be read. Please choose the folder again.");
            }
            var selectedFiles = new ArrayList<NoteSyncService.SelectedFile>();
            for (int index = 0; index < files.size(); index++) {
                selectedFiles.add(new NoteSyncService.SelectedFile(safeRelativePath(relativePaths.get(index)), Instant.ofEpochMilli(modifiedAts.get(index)), files.get(index)));
            }
            SyncResult result = syncService.sync(selectedFiles);
            flash.addFlashAttribute("message", "Sync complete: %d added, %d updated, %d removed, %d unchanged.".formatted(result.added(), result.updated(), result.removed(), result.skipped()));
        } catch (IOException | IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/sources";
    }

    @GetMapping("/reviews")
    String reviews(Model model) {
        model.addAttribute("reviews", reviews.openReviews());
        return "reviews";
    }

    @PostMapping("/reviews/{id}/dismiss")
    String dismiss(@PathVariable long id) {
        reviews.dismiss(id);
        return "redirect:/reviews";
    }

    @PostMapping(value = "/reviews/{id}/dismiss", params = "partial")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void dismissWithoutRefresh(@PathVariable long id) {
        reviews.dismiss(id);
    }

    @GetMapping("/quizzes/new")
    String newQuiz(Model model) {
        model.addAttribute("request", new QuizRequest(Difficulty.MEDIUM, QuestionType.MULTIPLE_CHOICE, 10, "", null));
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("types", QuestionType.values());
        return "quiz-setup";
    }

    @PostMapping("/quizzes")
    String create(@Valid @ModelAttribute("request") QuizRequest request, RedirectAttributes flash) {
        try {
            return "redirect:/quizzes/" + quizzes.generate(request).getId();
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
            return "redirect:/quizzes/new";
        }
    }

    @GetMapping("/quizzes/{id}")
    String quiz(@PathVariable long id, Model model) {
        var attempt = quizzes.completedAttempt(id);
        if (attempt.isPresent())
            return "redirect:/quizzes/" + id + "/attempts/" + attempt.get().getId();
        model.addAttribute("quiz", quizzes.get(id));
        return "quiz";
    }

    @PostMapping("/quizzes/{id}/submit")
    String submit(@PathVariable long id, @RequestParam MultiValueMap<String, String> form) {
        var completedAttempt = quizzes.completedAttempt(id);
        if (completedAttempt.isPresent())
            return "redirect:/quizzes/" + id + "/attempts/" + completedAttempt.get().getId();
        Map<Long, Set<Integer>> answers = new HashMap<>();
        form.forEach((key, values) -> {
            if (key.startsWith("answer_")) {
                long questionId = Long.parseLong(key.substring(7));
                Set<Integer> picked = new HashSet<>();
                for (String value : values) picked.add(Integer.parseInt(value));
                answers.put(questionId, picked);
            }
        });
        return "redirect:/quizzes/" + id + "/attempts/" + quizzes.submit(id, answers).getId();
    }

    @GetMapping("/quizzes/{quizId}/attempts/{attemptId}")
    String result(@PathVariable long quizId, @PathVariable long attemptId, Model model) {
        var quiz = quizzes.get(quizId);
        var attempt = quizzes.getAttempt(attemptId);
        if (!attempt.getQuiz().getId().equals(quiz.getId())) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("quiz", quiz);
        model.addAttribute("attempt", attempt);
        model.addAttribute("selectedAnswers", quizzes.selectedAnswers(attempt));
        return "result";
    }

    @GetMapping("/history")
    String history(Model model) {
        model.addAttribute("completedQuizzes", quizzes.history());
        return "history";
    }

    private java.util.List<com.prasadsumit.notebridge.persistence.IndexedDocument> syncServiceDocuments() {
        return syncService.documents();
    }

    private static String safeRelativePath(String path) {
        String normalized = path.replace('\\', '/');
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.contains("../") || normalized.equals("..")) {
            throw new IllegalArgumentException("A selected file had an invalid folder path.");
        }
        return normalized;
    }
}

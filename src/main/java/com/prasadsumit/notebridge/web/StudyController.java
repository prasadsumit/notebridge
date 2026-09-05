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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
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

    @PostMapping("/sources/sync")
    String sync(RedirectAttributes flash) {
        try {
            SyncResult result = syncService.sync();
            flash.addFlashAttribute("message", "Sync complete: %d added, %d updated, %d removed, %d unchanged.".formatted(result.added(), result.updated(), result.removed(), result.skipped()));
        } catch (IOException | IllegalStateException exception) {
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
        model.addAttribute("quiz", quizzes.get(id));
        return "quiz";
    }

    @PostMapping("/quizzes/{id}/submit")
    String submit(@PathVariable long id, @RequestParam MultiValueMap<String, String> form) {
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
        model.addAttribute("quiz", quizzes.get(quizId));
        model.addAttribute("attempt", quizzes.getAttempt(attemptId));
        return "result";
    }

    @GetMapping("/history")
    String history(Model model) {
        model.addAttribute("quizzes", quizzes.history());
        return "history";
    }

    private java.util.List<com.prasadsumit.notebridge.persistence.IndexedDocument> syncServiceDocuments() {
        return syncService.documents();
    }
}

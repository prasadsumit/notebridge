package com.prasadsumit.notebridge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByQuizIdOrderBySubmittedAtDesc(Long quizId);
}

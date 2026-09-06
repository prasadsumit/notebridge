package com.prasadsumit.notebridge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByQuizIdOrderBySubmittedAtDesc(Long quizId);
    Optional<QuizAttempt> findFirstByQuizIdOrderBySubmittedAtDesc(Long quizId);
    List<QuizAttempt> findAllByOrderBySubmittedAtDesc();
}

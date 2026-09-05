package com.prasadsumit.notebridge.persistence;

import com.prasadsumit.notebridge.model.Difficulty;
import com.prasadsumit.notebridge.model.QuestionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
public class Quiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Difficulty difficulty;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private QuestionType questionType;
    private Integer timeLimitMinutes;
    @Column(nullable = false) private Instant createdAt;
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position")
    private List<QuizQuestion> questions = new ArrayList<>();
}

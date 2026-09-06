package com.prasadsumit.notebridge.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) private Quiz quiz;
    @Column(nullable = false) private Instant submittedAt;
    @Column(nullable = false) private int score;
    @Column(nullable = false) private int possibleScore;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(nullable = false) private String selectedAnswersJson;
}

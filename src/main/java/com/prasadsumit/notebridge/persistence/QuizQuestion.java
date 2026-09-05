package com.prasadsumit.notebridge.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter @Setter @NoArgsConstructor
public class QuizQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Quiz quiz;
    @Column(nullable = false) private int position;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(nullable = false) private String prompt;
    @ElementCollection(fetch = FetchType.EAGER) private List<String> options = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER) private Set<Integer> correctOptionIndexes = new HashSet<>();
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(nullable = false) private String explanation;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) private String additionalLearningContext;
    @ElementCollection(fetch = FetchType.EAGER) private List<String> citations = new ArrayList<>();
}

package com.prasadsumit.notebridge.persistence;

import com.prasadsumit.notebridge.model.ConflictStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ConflictReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private NoteChunk chunk;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConflictStatus status = ConflictStatus.OPEN;
    @Column(nullable = false)
    private String conflictType;
    @Column(nullable = false)
    private Integer confidence;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    private String rationale;
    @Column(nullable = false)
    private Instant createdAt;
}

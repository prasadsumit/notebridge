package com.prasadsumit.notebridge.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter @Setter @NoArgsConstructor
public class NoteChunk {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private IndexedDocument document;
    @Column(nullable = false)
    private int sequenceNumber;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    private boolean excluded;
}

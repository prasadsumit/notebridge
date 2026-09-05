package com.prasadsumit.notebridge.persistence;

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
public class IndexedDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 1024)
    private String relativePath;
    @Column(nullable = false)
    private String format;
    @Column(nullable = false, length = 64)
    private String contentHash;
    @Column(nullable = false)
    private Instant sourceModifiedAt;
    @Column(nullable = false)
    private Instant indexedAt;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    private String content;
}

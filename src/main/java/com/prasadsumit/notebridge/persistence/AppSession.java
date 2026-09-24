package com.prasadsumit.notebridge.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_session")
@Getter
@Setter
@NoArgsConstructor
public class AppSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant endedAt;

    @Column(nullable = false)
    private long inputTokens;

    @Column(nullable = false)
    private long outputTokens;

    @Column(nullable = false)
    private long totalTokens;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startedAt DESC")
    private List<SessionActivity> activities = new ArrayList<>();
}

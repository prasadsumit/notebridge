package com.prasadsumit.notebridge.persistence;

import com.prasadsumit.notebridge.session.ActivityStatus;
import com.prasadsumit.notebridge.session.ActivityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "session_activity")
@Getter
@Setter
@NoArgsConstructor
public class SessionActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AppSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus status;

    @Column(nullable = false, length = 1024)
    private String description;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @Column(nullable = false)
    private long inputTokens;

    @Column(nullable = false)
    private long outputTokens;

    @Column(nullable = false)
    private long totalTokens;
}

package com.prasadsumit.notebridge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;

public interface AppSessionRepository extends JpaRepository<AppSession, Long> {
    List<AppSession> findByEndedAtIsNull();

    @EntityGraph(attributePaths = "activities")
    List<AppSession> findByStartedAtGreaterThanEqualOrderByStartedAtDesc(Instant startedAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from AppSession session where session.id = :id")
    java.util.Optional<AppSession> findForUpdate(Long id);
}

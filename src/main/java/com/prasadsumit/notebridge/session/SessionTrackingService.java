package com.prasadsumit.notebridge.session;

import com.prasadsumit.notebridge.persistence.AppSession;
import com.prasadsumit.notebridge.persistence.AppSessionRepository;
import com.prasadsumit.notebridge.persistence.SessionActivity;
import com.prasadsumit.notebridge.persistence.SessionActivityRepository;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class SessionTrackingService implements ActivityTracker {
    private final AppSessionRepository sessions;
    private final SessionActivityRepository activities;
    private final ZoneId sessionTimeZone;
    private final ThreadLocal<Long> activeActivityId = new ThreadLocal<>();
    private volatile Long currentSessionId;

    public SessionTrackingService(AppSessionRepository sessions, SessionActivityRepository activities, ZoneId sessionTimeZone) {
        this.sessions = sessions;
        this.activities = activities;
        this.sessionTimeZone = sessionTimeZone;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public synchronized void startApplicationSession() {
        Instant now = Instant.now();
        for (AppSession previous : sessions.findByEndedAtIsNull()) {
            Instant lastKnownActivity = previous.getActivities().stream()
                    .map(activity -> activity.getCompletedAt() == null ? activity.getStartedAt() : activity.getCompletedAt())
                    .max(Instant::compareTo).orElse(previous.getStartedAt());
            previous.setEndedAt(lastKnownActivity);
            sessions.save(previous);
        }
        AppSession session = new AppSession();
        session.setStartedAt(now);
        currentSessionId = sessions.save(session).getId();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public SessionActivity start(ActivityType type, String description) {
        AppSession session = currentSession();
        SessionActivity activity = new SessionActivity();
        activity.setSession(session);
        activity.setActivityType(type);
        activity.setStatus(ActivityStatus.IN_PROGRESS);
        activity.setDescription(description);
        activity.setStartedAt(Instant.now());
        activity = activities.save(activity);
        activeActivityId.set(activity.getId());
        return activity;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void addTokens(int inputTokens, int outputTokens) {
        Long activityId = activeActivityId.get();
        if (activityId == null) return;
        SessionActivity activity = activities.findById(activityId).orElse(null);
        if (activity == null) return;
        activity.setInputTokens(activity.getInputTokens() + Math.max(0, inputTokens));
        activity.setOutputTokens(activity.getOutputTokens() + Math.max(0, outputTokens));
        activity.setTotalTokens(activity.getInputTokens() + activity.getOutputTokens());
        AppSession session = sessions.findForUpdate(activity.getSession().getId())
                .orElseThrow(() -> new IllegalStateException("The current app session could not be loaded."));
        session.setInputTokens(session.getInputTokens() + Math.max(0, inputTokens));
        session.setOutputTokens(session.getOutputTokens() + Math.max(0, outputTokens));
        session.setTotalTokens(session.getInputTokens() + session.getOutputTokens());
        activities.save(activity);
        sessions.save(session);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void complete(SessionActivity activity, String description) {
        SessionActivity persisted = activities.findById(activity.getId()).orElse(activity);
        persisted.setDescription(description);
        persisted.setStatus(ActivityStatus.COMPLETED);
        persisted.setCompletedAt(Instant.now());
        activities.save(persisted);
        copyCompletionState(persisted, activity);
        activeActivityId.remove();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void fail(SessionActivity activity, Exception exception) {
        SessionActivity persisted = activities.findById(activity.getId()).orElse(activity);
        persisted.setStatus(ActivityStatus.FAILED);
        persisted.setCompletedAt(Instant.now());
        String message = exception.getMessage();
        if (message != null && !message.isBlank()) persisted.setDescription(persisted.getDescription() + " — " + message);
        activities.save(persisted);
        copyCompletionState(persisted, activity);
        activeActivityId.remove();
    }

    @Transactional
    public List<AppSession> sessionsThisMonth() {
        Instant monthStart = java.time.ZonedDateTime.now(sessionTimeZone)
                .with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay(sessionTimeZone).toInstant();
        return sessions.findByStartedAtGreaterThanEqualOrderByStartedAtDesc(monthStart);
    }

    public Long currentSessionId() {
        return currentSessionId;
    }

    @PreDestroy
    @Transactional
    public void endApplicationSession() {
        if (currentSessionId == null) return;
        sessions.findById(currentSessionId).ifPresent(session -> {
            session.setEndedAt(Instant.now());
            sessions.save(session);
        });
    }

    private AppSession currentSession() {
        if (currentSessionId == null) startApplicationSession();
        return sessions.findById(currentSessionId)
                .orElseThrow(() -> new IllegalStateException("The current app session could not be loaded."));
    }

    private void copyCompletionState(SessionActivity source, SessionActivity target) {
        target.setDescription(source.getDescription());
        target.setStatus(source.getStatus());
        target.setCompletedAt(source.getCompletedAt());
        target.setInputTokens(source.getInputTokens());
        target.setOutputTokens(source.getOutputTokens());
        target.setTotalTokens(source.getTotalTokens());
    }
}

package com.prasadsumit.notebridge.session;

import com.prasadsumit.notebridge.persistence.AppSession;
import com.prasadsumit.notebridge.persistence.AppSessionRepository;
import com.prasadsumit.notebridge.persistence.SessionActivity;
import com.prasadsumit.notebridge.persistence.SessionActivityRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionTrackingServiceTests {
    @Test
    void startsANewSessionAndRollsTokenUsageIntoItsActivity() {
        AppSessionRepository sessions = mock(AppSessionRepository.class);
        SessionActivityRepository activities = mock(SessionActivityRepository.class);
        AtomicReference<AppSession> savedSession = new AtomicReference<>();
        AtomicReference<SessionActivity> savedActivity = new AtomicReference<>();
        when(sessions.findByEndedAtIsNull()).thenReturn(List.of());
        when(sessions.save(any())).thenAnswer(invocation -> {
            AppSession session = invocation.getArgument(0);
            if (session.getId() == null) session.setId(7L);
            savedSession.set(session);
            return session;
        });
        when(sessions.findById(7L)).thenAnswer(invocation -> Optional.of(savedSession.get()));
        when(sessions.findForUpdate(7L)).thenAnswer(invocation -> Optional.of(savedSession.get()));
        when(activities.save(any())).thenAnswer(invocation -> {
            SessionActivity activity = invocation.getArgument(0);
            if (activity.getId() == null) activity.setId(11L);
            savedActivity.set(activity);
            return activity;
        });
        when(activities.findById(11L)).thenAnswer(invocation -> Optional.of(savedActivity.get()));

        SessionTrackingService tracking = new SessionTrackingService(sessions, activities, ZoneId.of("Asia/Kolkata"));
        tracking.startApplicationSession();
        SessionActivity activity = tracking.start(ActivityType.QUIZ_CREATED, "Creating quiz");
        tracking.addTokens(120, 30);
        tracking.complete(activity, "Biology · 10 questions · 2 sources");

        assertEquals(150, activity.getTotalTokens());
        assertEquals(150, activity.getSession().getTotalTokens());
        assertEquals(ActivityStatus.COMPLETED, activity.getStatus());
        assertEquals("Biology · 10 questions · 2 sources", activity.getDescription());
    }
}

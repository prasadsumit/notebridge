package com.prasadsumit.notebridge.web;

import com.prasadsumit.notebridge.persistence.AppSession;
import com.prasadsumit.notebridge.persistence.SessionActivity;
import com.prasadsumit.notebridge.session.ActivityStatus;
import com.prasadsumit.notebridge.session.ActivityType;
import com.prasadsumit.notebridge.session.SessionTrackingService;
import com.prasadsumit.notebridge.config.SessionTimeConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WebMvcTest(SessionController.class)
@Import(SessionTimeConfiguration.class)
class SessionsViewTests {
    @Autowired MockMvc mockMvc;
    @MockitoBean SessionTrackingService sessionTracking;

    @Test
    void showsCurrentActivityAndMonthlyTokenTotals() throws Exception {
        AppSession session = new AppSession();
        session.setId(5L);
        session.setStartedAt(Instant.parse("2026-09-24T12:00:00Z"));
        session.setInputTokens(1_200);
        session.setOutputTokens(345);
        session.setTotalTokens(1_545);
        SessionActivity activity = new SessionActivity();
        activity.setSession(session);
        activity.setActivityType(ActivityType.QUIZ_CREATED);
        activity.setStatus(ActivityStatus.COMPLETED);
        activity.setDescription("Biology · 10 questions · 2 sources");
        activity.setStartedAt(Instant.parse("2026-09-24T12:05:00Z"));
        activity.setTotalTokens(1_545);
        session.setActivities(List.of(activity));
        AppSession previous = new AppSession();
        previous.setId(4L);
        previous.setStartedAt(Instant.parse("2026-09-23T10:00:00Z"));
        previous.setEndedAt(Instant.parse("2026-09-23T11:00:00Z"));
        previous.setTotalTokens(200);
        AppSession sameDayPrevious = new AppSession();
        sameDayPrevious.setId(3L);
        sameDayPrevious.setStartedAt(Instant.parse("2026-09-23T08:00:00Z"));
        sameDayPrevious.setEndedAt(Instant.parse("2026-09-23T09:00:00Z"));
        sameDayPrevious.setTotalTokens(100);
        when(sessionTracking.currentSessionId()).thenReturn(5L);
        when(sessionTracking.sessionsThisMonth()).thenReturn(List.of(session, previous, sameDayPrevious));

        mockMvc.perform(get("/sessions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Current session")))
                .andExpect(content().string(containsString("Biology · 10 questions · 2 sources")))
                .andExpect(content().string(containsString("5:35 PM")))
                .andExpect(content().string(containsString("No activity recorded.")))
                .andExpect(content().string(containsString("200")))
                .andExpect(content().string(containsString("1,545")))
                .andExpect(result -> assertEquals(1, occurrences(result.getResponse().getContentAsString(),
                        "class=\"content-card session-day-group\"")))
                .andExpect(result -> assertEquals(2, occurrences(result.getResponse().getContentAsString(),
                        "class=\"previous-session\"")));
    }

    private static int occurrences(String value, String match) {
        return value.split(java.util.regex.Pattern.quote(match), -1).length - 1;
    }
}

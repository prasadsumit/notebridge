package com.prasadsumit.notebridge.web;

import com.prasadsumit.notebridge.persistence.AppSession;
import com.prasadsumit.notebridge.session.SessionTrackingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.LinkedHashMap;

@Controller
public class SessionController {
    public record SessionDayGroup(LocalDate date, List<AppSession> sessions) {
    }

    private final SessionTrackingService sessionTracking;
    private final ZoneId sessionTimeZone;

    public SessionController(SessionTrackingService sessionTracking, ZoneId sessionTimeZone) {
        this.sessionTracking = sessionTracking;
        this.sessionTimeZone = sessionTimeZone;
    }

    @GetMapping("/sessions")
    String sessions(Model model) {
        List<AppSession> monthSessions = sessionTracking.sessionsThisMonth();
        Long currentId = sessionTracking.currentSessionId();
        AppSession current = monthSessions.stream()
                .filter(session -> session.getId().equals(currentId))
                .findFirst().orElse(null);
        model.addAttribute("currentSession", current);
        List<AppSession> previousSessions = monthSessions.stream()
                .filter(session -> !session.getId().equals(currentId)).toList();
        var sessionsByDate = new LinkedHashMap<LocalDate, List<AppSession>>();
        previousSessions.forEach(session -> sessionsByDate.computeIfAbsent(
                session.getStartedAt().atZone(sessionTimeZone).toLocalDate(), ignored -> new java.util.ArrayList<>()).add(session));
        model.addAttribute("previousSessionCount", previousSessions.size());
        model.addAttribute("previousSessionDays", sessionsByDate.entrySet().stream()
                .map(entry -> new SessionDayGroup(entry.getKey(), List.copyOf(entry.getValue()))).toList());
        model.addAttribute("sessionTimeZone", sessionTimeZone);
        return "sessions";
    }
}

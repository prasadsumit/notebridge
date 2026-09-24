package com.prasadsumit.notebridge.session;

import com.prasadsumit.notebridge.persistence.SessionActivity;

public interface ActivityTracker {
    SessionActivity start(ActivityType type, String description);

    void addTokens(int inputTokens, int outputTokens);

    void complete(SessionActivity activity, String description);

    void fail(SessionActivity activity, Exception exception);
}

package com.prasadsumit.notebridge.session;

public enum ActivityType {
    FILE_UPLOAD("Files uploaded"),
    QUIZ_CREATED("Quiz created");

    private final String label;

    ActivityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

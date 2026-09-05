package com.prasadsumit.notebridge.ai;

/** A suspected conflict, never a claim of verified fact. */
public record ConflictAssessment(boolean conflict, int confidence, String conflictType, String rationale) { }

package com.prasadsumit.notebridge.ai;

/** A possible factual error found in a note excerpt, subject to the owner's review. */
public record ConflictAssessment(boolean incorrect, int confidence, String conflictType, String rationale) { }

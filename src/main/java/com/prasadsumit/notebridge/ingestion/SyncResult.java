package com.prasadsumit.notebridge.ingestion;

public record SyncResult(int added, int updated, int removed, int skipped, int conflictsFlagged) { }

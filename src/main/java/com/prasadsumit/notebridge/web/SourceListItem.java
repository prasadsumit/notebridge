package com.prasadsumit.notebridge.web;

import java.time.Instant;
import java.util.Locale;

public record SourceListItem(Long id,
                             String relativePath,
                             String format,
                             String fileSize,
                             Instant indexedAt) {
    public static SourceListItem from(com.prasadsumit.notebridge.persistence.IndexedDocument document) {
        return new SourceListItem(document.getId(), document.getRelativePath(), document.getFormat(),
                formatFileSize(document.getSourceFileSize()), document.getIndexedAt());
    }

    private static String formatFileSize(Long bytes) {
        if (bytes == null) return "size unavailable";
        if (bytes < 1_024) return bytes + " B";

        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unitIndex = -1;
        do {
            value /= 1_024;
            unitIndex++;
        } while (value >= 1_024 && unitIndex < units.length - 1);

        String pattern = value < 10 ? "%.1f %s" : "%.0f %s";
        return String.format(Locale.ROOT, pattern, value, units[unitIndex]);
    }
}
package com.prasadsumit.notebridge.quiz;

import com.prasadsumit.notebridge.persistence.NoteChunk;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds a source location from indexed text without guessing page numbers. */
final class CitationLocator {
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("(?m)^[ \\t]{0,3}#{1,6}[ \\t]+(.+?)[ \\t]*#*[ \\t]*$");
    private static final Pattern CHAPTER_HEADING = Pattern.compile("(?im)^[ \\t]*((?:chapter|section)[ \\t]+[0-9ivxlc]+(?:\\.[0-9]+)*(?:[ \\t]*[:.\\-–][ \\t]*|[ \\t]+)?[^\\r\\n]{0,80})[ \\t]*$");

    private record LocatedHeading(int position, String title) { }

    private CitationLocator() { }

    static String describe(NoteChunk chunk) {
        String path = chunk.getDocument().getRelativePath();
        String heading = nearestHeading(chunk);
        String location = heading == null ? path : path + " — " + heading;
        return location + " (excerpt " + (chunk.getSequenceNumber() + 1) + ")";
    }

    private static String nearestHeading(NoteChunk chunk) {
        String content = chunk.getDocument().getContent();
        if (content == null || chunk.getContent() == null) return null;
        int position = content.indexOf(chunk.getContent());
        if (position < 0 || content.indexOf(chunk.getContent(), position + 1) >= 0) return null;
        int firstLineEnd = content.indexOf('\n', position);
        int end = firstLineEnd < 0 ? content.length() : firstLineEnd;
        String beforeAndFirstLine = content.substring(0, end);
        LocatedHeading heading = lastMatch(MARKDOWN_HEADING.matcher(beforeAndFirstLine));
        LocatedHeading chapter = lastMatch(CHAPTER_HEADING.matcher(beforeAndFirstLine));
        if (heading == null) return chapter == null ? null : chapter.title();
        if (chapter == null) return heading.title();
        return heading.position() >= chapter.position() ? heading.title() : chapter.title();
    }

    private static LocatedHeading lastMatch(Matcher matcher) {
        LocatedHeading value = null;
        while (matcher.find()) value = new LocatedHeading(matcher.start(), matcher.group(1).trim());
        return value;
    }
}

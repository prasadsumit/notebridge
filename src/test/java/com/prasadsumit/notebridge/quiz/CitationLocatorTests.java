package com.prasadsumit.notebridge.quiz;

import com.prasadsumit.notebridge.persistence.IndexedDocument;
import com.prasadsumit.notebridge.persistence.NoteChunk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitationLocatorTests {
    @Test
    void includesNearestMarkdownHeadingAndSection() {
        String content = "# Collections\nOverview\n## Maps\nA map stores key-value pairs.\n";
        assertEquals("notes.md — Maps (excerpt 2)",
                CitationLocator.describe(chunk("notes.md", content, "A map stores key-value pairs.", 1)));
    }

    @Test
    void fallsBackToFileAndSectionWhenNoHeadingIsAvailable() {
        assertEquals("chapter.pdf (excerpt 1)",
                CitationLocator.describe(chunk("chapter.pdf", "PDF body text", "PDF body text", 0)));
    }

    @Test
    void usesChapterLineWhenPresent() {
        String content = "Chapter 3: Sorting\nA sorted collection keeps an ordering.";
        assertEquals("book.txt — Chapter 3: Sorting (excerpt 1)",
                CitationLocator.describe(chunk("book.txt", content, "A sorted collection keeps an ordering.", 0)));
    }

    private NoteChunk chunk(String path, String documentContent, String chunkContent, int sequence) {
        IndexedDocument document = new IndexedDocument();
        document.setRelativePath(path);
        document.setContent(documentContent);
        NoteChunk chunk = new NoteChunk();
        chunk.setDocument(document);
        chunk.setContent(chunkContent);
        chunk.setSequenceNumber(sequence);
        return chunk;
    }
}

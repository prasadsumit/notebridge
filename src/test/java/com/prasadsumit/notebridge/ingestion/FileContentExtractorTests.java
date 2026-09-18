package com.prasadsumit.notebridge.ingestion;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileContentExtractorTests {
    private final FileContentExtractor extractor = new FileContentExtractor();

    @Test
    void supportsEpubFilesCaseInsensitively() {
        assertTrue(extractor.supports("book.epub"));
        assertTrue(extractor.supports("BOOK.EPUB"));
        assertFalse(extractor.supports("book.epub.zip"));
    }

    @Test
    void extractsTextFromEpubFiles() throws Exception {
        String text = extractor.extract(new ByteArrayInputStream(epub()), "book.epub");

        assertTrue(text.contains("Chapter One"));
        assertTrue(text.contains("EPUB source content"));
    }

    private static byte[] epub() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            add(zip, "mimetype", "application/epub+zip");
            add(zip, "META-INF/container.xml", """
                    <?xml version="1.0"?>
                    <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                      <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
                    </container>
                    """);
            add(zip, "OEBPS/content.opf", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <package version="3.0" xmlns="http://www.idpf.org/2007/opf" unique-identifier="book-id">
                      <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                        <dc:identifier id="book-id">notebridge-test</dc:identifier>
                        <dc:title>Test book</dc:title>
                        <dc:language>en</dc:language>
                      </metadata>
                      <manifest><item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml"/></manifest>
                      <spine><itemref idref="chapter"/></spine>
                    </package>
                    """);
            add(zip, "OEBPS/chapter.xhtml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml"><head><title>Chapter One</title></head>
                    <body><h1>Chapter One</h1><p>EPUB source content</p></body></html>
                    """);
        }
        return output.toByteArray();
    }

    private static void add(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}

package com.prasadsumit.notebridge.ingestion;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

@Component
public class FileContentExtractor {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("md", "txt", "pdf", "docx");
    private final Tika tika = new Tika();

    public boolean supports(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && SUPPORTED_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public String extract(Path path) throws IOException {
        try {
            return tika.parseToString(path).trim();
        } catch (TikaException exception) {
            throw new IOException("Could not parse " + path.getFileName(), exception);
        }
    }
}

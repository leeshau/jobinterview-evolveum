package org.lesek.usermanagement.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes a list of items to a single JSON file. Kept generic so
 * both users and policies can reuse the same persistence mechanism.
 * <p>
 * {@code filePath} is resolved relative to the process's working directory.
 * To avoid silently starting with an empty, confusing-looking data set when
 * that external file cannot be found, {@link #load()} falls back to a same-named
 * seed file bundled on the classpath under {@code /data/<filename>}.
 */
public class JsonFileStore<T> {

    private static final Logger log = LoggerFactory.getLogger(JsonFileStore.class);

    private final ObjectMapper objectMapper;
    private final Path filePath;
    private final TypeReference<List<T>> typeReference;

    public JsonFileStore(Path filePath, TypeReference<List<T>> typeReference) {
        this.filePath = filePath;
        this.typeReference = typeReference;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * This method is called upon application start. It loads JSON data into memory.
     */
    public List<T> load() {
        Path absolutePath = filePath.toAbsolutePath();
        if (Files.exists(filePath)) {
            log.info("Loading {} from {}", typeReference, absolutePath);
            try {
                return new ArrayList<>(objectMapper.readValue(filePath.toFile(), typeReference));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load data from " + absolutePath, e);
            }
        }
        log.warn("{} not found, falling back to bundled seed data for {}", absolutePath, typeReference);
        return loadSeedFromClasspath();
    }

    /**
     * Tries to look for and load JSON source file upon not finding one in the correct place.
     */
    private List<T> loadSeedFromClasspath() {
        String resource = "/data/" + filePath.getFileName();
        try (InputStream in = JsonFileStore.class.getResourceAsStream(resource)) {
            if (in == null) {
                log.warn("No bundled seed data found at classpath resource {}", resource);
                return new ArrayList<>();
            }
            return new ArrayList<>(objectMapper.readValue(in, typeReference));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load seed data from classpath resource " + resource, e);
        }
    }

    public void save(List<T> items) {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            objectMapper.writeValue(filePath.toFile(), items);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save data to " + filePath, e);
        }
    }
}

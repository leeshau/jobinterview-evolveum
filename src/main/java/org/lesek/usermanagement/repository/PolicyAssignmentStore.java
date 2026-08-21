package org.lesek.usermanagement.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the user-id -> policy-ids assignment snapshot to a
 * single JSON file, e.g. {@code {"jdoe": ["underaged", "internal-user"]}}.
 * Mirrors {@link JsonFileStore}'s persistence approach (including the
 * classpath seed fallback), but for a map instead of a list, since that is
 * the natural shape of this data.
 * @implNote Can be generalized just like {@link JsonFileStore} if needed.
 */
public class PolicyAssignmentStore {

    private static final Logger log = LoggerFactory.getLogger(PolicyAssignmentStore.class);
    private static final TypeReference<Map<String, List<String>>> TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Path filePath;

    public PolicyAssignmentStore(Path filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Map<String, List<String>> load() {
        Path absolutePath = filePath.toAbsolutePath();
        if (Files.exists(filePath)) {
            log.info("Loading policy assignments from {}", absolutePath);
            try {
                return new LinkedHashMap<>(objectMapper.readValue(filePath.toFile(), TYPE));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load data from " + absolutePath, e);
            }
        }
        log.warn("{} not found, falling back to bundled seed data for policy assignments", absolutePath);
        return loadSeedFromClasspath();
    }

    private Map<String, List<String>> loadSeedFromClasspath() {
        String resource = "/data/" + filePath.getFileName();
        try (InputStream in = PolicyAssignmentStore.class.getResourceAsStream(resource)) {
            if (in == null) {
                log.warn("No bundled seed data found at classpath resource {}", resource);
                return new LinkedHashMap<>();
            }
            return new LinkedHashMap<>(objectMapper.readValue(in, TYPE));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load seed data from classpath resource " + resource, e);
        }
    }

    public void save(Map<String, List<String>> assignments) {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            objectMapper.writeValue(filePath.toFile(), assignments);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save data to " + filePath, e);
        }
    }
}

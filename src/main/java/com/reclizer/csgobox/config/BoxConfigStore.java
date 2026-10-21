package com.reclizer.csgobox.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Parse and validate before publishing; never replace a good file/snapshot with a partial one. */
public final class BoxConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path file;
    private final Predicate<String> validItem;
    private volatile Map<String, BoxDefinition> snapshot = Map.of();

    public BoxConfigStore(Path file, Predicate<String> validItem) {
        this.file = file;
        this.validItem = validItem;
    }

    public Map<String, BoxDefinition> snapshot() { return snapshot; }

    public synchronized int reload() throws IOException {
        Map<String, BoxDefinition> next = read();
        snapshot = next;
        return next.size();
    }

    private Map<String, BoxDefinition> read() throws IOException {
        if (Files.size(file) > 2 * 1024 * 1024) throw new IOException("box.json exceeds 2 MiB");
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<BoxDefinition> definitions = GSON.fromJson(reader, new TypeToken<List<BoxDefinition>>() {}.getType());
            if (definitions == null) throw new IllegalArgumentException("Expected a JSON array");
            if (definitions.size() > 512) throw new IllegalArgumentException("At most 512 box definitions are supported");
            LinkedHashMap<String, BoxDefinition> next = new LinkedHashMap<>();
            for (BoxDefinition box : definitions) {
                Objects.requireNonNull(box, "Null box definition").validate(validItem);
                if (next.putIfAbsent(box.name(), box) != null)
                    throw new IllegalArgumentException("Duplicate box name: " + box.name());
            }
            return Collections.unmodifiableMap(next);
        } catch (JsonParseException | IllegalArgumentException | NullPointerException ex) {
            throw new IOException("Invalid box.json: " + ex.getMessage(), ex);
        }
    }

    public synchronized void saveRewards(String name, List<RewardEntry> rewards) throws IOException {
        // Include external changes, and refuse to overwrite an invalid file.
        Map<String, BoxDefinition> next = new LinkedHashMap<>(read());
        BoxDefinition previous = next.get(name);
        BoxDefinition replacement = previous == null ? BoxDefinition.create(name, rewards) : previous.withRewards(rewards);
        replacement.validate(validItem);
        next.put(name, replacement);
        if (next.size() > 512) throw new IllegalArgumentException("At most 512 box definitions are supported");
        String json = GSON.toJson(next.values());
        if (json.getBytes(StandardCharsets.UTF_8).length > 2 * 1024 * 1024)
            throw new IOException("box.json exceeds 2 MiB");
        Path temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "box-", ".tmp");
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            snapshot = Collections.unmodifiableMap(next);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

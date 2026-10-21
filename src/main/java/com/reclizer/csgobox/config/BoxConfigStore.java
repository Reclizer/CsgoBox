package com.reclizer.csgobox.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Predicate;

/** Parse and validate before publishing; never replace a good file/snapshot with a partial one. */
public final class BoxConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path file;
    private final Predicate<String> validItem;
    private final java.util.function.Consumer<BoxDefinition> validateRuntime;
    private volatile Map<String, BoxDefinition> snapshot = Map.of();

    public BoxConfigStore(Path file, Predicate<String> validItem) {
        this(file, validItem, ignored -> {});
    }
    public BoxConfigStore(Path file, Predicate<String> validItem, java.util.function.Consumer<BoxDefinition> validateRuntime) {
        this.file = file;
        this.validItem = validItem;
        this.validateRuntime = validateRuntime;
    }

    public Map<String, BoxDefinition> snapshot() { return snapshot; }
    public synchronized BoxDefinition readForEditing(String id) throws IOException { return read().get(id); }

    /** Optimistic version of one pool; unrelated pool changes do not block a save. */
    public static String revision(BoxDefinition box) {
        if (box == null) return "";
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(GSON.toJson(box).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
    }

    public synchronized void saveDefinition(BoxDefinition box, String expectedRevision) throws IOException {
        Map<String, BoxDefinition> next = new LinkedHashMap<>(read());
        if (!revision(next.get(box.id())).equals(expectedRevision))
            throw new IllegalArgumentException("editor.csgobox.conflict");
        box.validate(validItem);
        validateRuntime.accept(box);
        next.put(box.id(), box);
        publish(next);
    }

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
                validateRuntime.accept(box);
                if (next.putIfAbsent(box.id(), box) != null)
                    throw new IllegalArgumentException("Duplicate pool id: " + box.id());
            }
            return Collections.unmodifiableMap(next);
        } catch (JsonParseException | IllegalArgumentException | NullPointerException ex) {
            throw new IOException("Invalid box.json: " + ex.getMessage(), ex);
        }
    }

    private void publish(Map<String, BoxDefinition> next) throws IOException {
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

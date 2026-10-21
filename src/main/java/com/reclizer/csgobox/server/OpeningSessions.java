package com.reclizer.csgobox.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Single-use previews, confined to the server thread. */
public final class OpeningSessions<T> {
    public record Session<T>(UUID token, int slot, T snapshot, long expiresAt) {}
    private final Map<UUID, Session<T>> sessions = new HashMap<>();

    public Session<T> begin(UUID player, int slot, T snapshot, long now) {
        Session<T> session = new Session<>(UUID.randomUUID(), slot, snapshot, now + 1200);
        sessions.put(player, session);
        return session;
    }

    public Session<T> consume(UUID player, UUID token, long now) {
        Session<T> session = sessions.get(player);
        if (session == null || !session.token().equals(token)) return null;
        sessions.remove(player);
        return now >= session.expiresAt() ? null : session;
    }

    public void remove(UUID player) { sessions.remove(player); }
    public void clear() { sessions.clear(); }
}

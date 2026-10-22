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

    public Session<T> peek(UUID player, UUID token, long now) {
        var session = sessions.get(player);
        return session != null && session.token().equals(token) && now < session.expiresAt() ? session : null;
    }

    public boolean renew(UUID player, UUID token, long now) {
        var session = peek(player, token, now);
        if (session == null) return false;
        sessions.put(player, new Session<>(token, session.slot(), session.snapshot(), now + 1200));
        return true;
    }

    public void remove(UUID player) { sessions.remove(player); }
    public void clear() { sessions.clear(); }
}

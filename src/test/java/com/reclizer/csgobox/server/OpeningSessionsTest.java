package com.reclizer.csgobox.server;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class OpeningSessionsTest {
    @Test void tokenIsBoundToPlayerAndConsumedOnce() {
        var sessions = new OpeningSessions<String>();
        UUID player = UUID.randomUUID();
        var session = sessions.begin(player, 4, "box", 10);
        assertNull(sessions.consume(UUID.randomUUID(), session.token(), 11));
        assertNull(sessions.consume(player, UUID.randomUUID(), 11));
        assertEquals(session, sessions.consume(player, session.token(), 11));
        assertNull(sessions.consume(player, session.token(), 11));
    }

    @Test void expiredSupersededAndLoggedOutSessionsAreRejected() {
        var sessions = new OpeningSessions<String>();
        UUID player = UUID.randomUUID();
        var first = sessions.begin(player, 0, "first", 0);
        var second = sessions.begin(player, 1, "second", 1);
        assertNull(sessions.consume(player, first.token(), 2));
        assertNull(sessions.consume(player, second.token(), 1201));
        var third = sessions.begin(player, 0, "third", 1202);
        sessions.remove(player);
        assertNull(sessions.consume(player, third.token(), 1203));
        var fourth = sessions.begin(player, 0, "fourth", 1204);
        sessions.clear();
        assertNull(sessions.consume(player, fourth.token(), 1205));
    }
}

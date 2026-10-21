package com.reclizer.csgobox.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BoxConfigStoreTest {
    @TempDir Path directory;
    private static final String VALID = """
            [{"name":"测试箱","key":"csgobox:csgo_key0","drop":0.3,
              "random":[2,5,6,20,625],"entity":["minecraft:zombie"],
              "grade1":["minecraft:stone"]}]
            """;

    private BoxConfigStore store(String content) throws IOException {
        Path path = directory.resolve("box.json");
        Files.writeString(path, content);
        return new BoxConfigStore(path, item -> !item.equals("missing:item"));
    }

    @Test void loadsLegacySchemaWithOmittedEmptyGrades() throws IOException {
        var store = store(VALID);
        assertEquals(1, store.reload());
        assertEquals(List.of(), store.snapshot().get("测试箱").grade5());
        assertThrows(UnsupportedOperationException.class, () -> store.snapshot().clear());
        assertThrows(UnsupportedOperationException.class, () -> store.snapshot().get("测试箱").grade1().clear());
    }

    @Test void badReloadKeepsLastSnapshotAndFile() throws IOException {
        var store = store(VALID);
        store.reload();
        var before = store.snapshot();
        for (String invalid : List.of("{", "null", "[null]", VALID.replace("625", "-1"),
                VALID.replace("minecraft:stone", "missing:item"), VALID.replace("minecraft:stone", ""),
                VALID.replace("0.3", "2"), VALID.replace("[2,5,6,20,625]", "[0,0,0,0,0]"))) {
            Files.writeString(directory.resolve("box.json"), invalid);
            assertThrows(IOException.class, store::reload, invalid);
            assertSame(before, store.snapshot());
            assertEquals(invalid, Files.readString(directory.resolve("box.json")));
        }
    }

    @Test void savePairsRewardsWithoutParallelListsAndPreservesMetadata() throws IOException {
        var store = store(VALID);
        store.reload();
        store.saveRewards("测试箱", List.of(new RewardEntry("minecraft:diamond", 5), new RewardEntry("minecraft:stick", 1)));
        var saved = store.snapshot().get("测试箱");
        assertEquals(List.of("minecraft:diamond"), saved.grade5());
        assertEquals(List.of("minecraft:stick"), saved.grade1());
        assertEquals(0.3F, saved.drop());
        assertEquals(List.of(2,5,6,20,625), saved.random());
        assertEquals(List.of("minecraft:zombie"), saved.entity());
        assertEquals(1, store.reload());
        assertEquals(saved, store.snapshot().get("测试箱"));
    }

    @Test void invalidSaveDoesNotOverwriteAndExternalEditsAreRetained() throws IOException {
        var store = store(VALID);
        store.reload();
        assertThrows(IllegalArgumentException.class, () -> store.saveRewards("测试箱", List.of()));
        assertEquals(VALID, Files.readString(directory.resolve("box.json")));
        Files.writeString(directory.resolve("box.json"), VALID.replace("0.3", "0.7"));
        store.saveRewards("测试箱", List.of(new RewardEntry("minecraft:diamond", 1)));
        assertEquals(0.7F, store.snapshot().get("测试箱").drop());
        Files.writeString(directory.resolve("box.json"), "invalid");
        assertThrows(IOException.class, () -> store.saveRewards("new", List.of(new RewardEntry("minecraft:stone", 1))));
        assertEquals("invalid", Files.readString(directory.resolve("box.json")));
    }

    @Test void rejectsDuplicateNamesAndInvalidGrades() throws IOException {
        String object = VALID.trim().substring(1, VALID.trim().length() - 1);
        assertThrows(IOException.class, store("[" + object + "," + object + "]")::reload);
        assertThrows(IllegalArgumentException.class, () -> new RewardEntry("minecraft:stone", 6));
    }

    @Test void newBoxIsAvailableImmediatelyAndSurvivesReload() throws IOException {
        var store = store("[]");
        store.reload();
        store.saveRewards("新箱子", List.of(new RewardEntry("minecraft:stone", 3)));
        assertEquals(List.of("minecraft:stone"), store.snapshot().get("新箱子").grade3());
        assertEquals(1, store.reload());
    }
}

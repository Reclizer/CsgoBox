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
        assertTrue(store.snapshot().get("测试箱").luckEnabled());
        assertThrows(UnsupportedOperationException.class, () -> store.snapshot().clear());
        assertThrows(UnsupportedOperationException.class, () -> store.snapshot().get("测试箱").grade1().clear());
    }

    @Test void luckToggleSurvivesEditorCodecSaveReloadAndRewardEdits() throws IOException {
        var store = store(VALID);
        store.reload();
        var original = store.snapshot().get("测试箱");
        var edited = original.withLuckEnabled(false);
        assertEquals(0F, edited.effectiveLuck(100));
        assertEquals(0F, edited.effectiveLuck(-10));
        assertEquals(10F, original.effectiveLuck(10));
        assertEquals(edited, BoxEditorCodec.decode(BoxEditorCodec.encode(edited)));
        assertNotEquals(BoxConfigStore.revision(original), BoxConfigStore.revision(edited));
        store.saveDefinition(edited, BoxConfigStore.revision(original));
        store.reload();
        assertFalse(store.snapshot().get("测试箱").luckEnabled());
        store.saveRewards("测试箱", List.of(new RewardEntry("minecraft:diamond", 1)));
        assertFalse(store.readForEditing("测试箱").luckEnabled());
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

    @Test void editorSavesAllRulesAndRejectsStaleOrAccidentalOverwrite() throws IOException {
        var store = store(VALID);
        store.reload();
        var original = store.snapshot().get("测试箱");
        String version = BoxConfigStore.revision(original);
        var edited = new BoxDefinition("测试箱", "minecraft:tripwire_hook", 0.8F, List.of(10,0,0,0,0),
                List.of("minecraft:skeleton"), List.of(), List.of(), List.of(), List.of(), List.of("minecraft:diamond"));
        store.saveDefinition(edited, version);
        assertEquals(edited, store.readForEditing("测试箱"));
        assertEquals(edited, store.snapshot().get("测试箱"));
        assertThrows(IllegalArgumentException.class, () -> store.saveDefinition(original, version));
        assertThrows(IllegalArgumentException.class, () -> store.saveDefinition(original, ""));
        assertEquals(edited, store.readForEditing("测试箱"));
    }

    @Test void editorDetectsExternalChangesAndCanReadFreshVersionWithoutPublishing() throws IOException {
        var store = store(VALID);
        store.reload();
        var original = store.snapshot().get("测试箱");
        Files.writeString(directory.resolve("box.json"), VALID.replace("0.3", "0.9"));
        assertThrows(IllegalArgumentException.class, () -> store.saveDefinition(original, BoxConfigStore.revision(original)));
        var latest = store.readForEditing("测试箱");
        assertEquals(0.9F, latest.drop());
        assertEquals(original, store.snapshot().get("测试箱"));
        store.saveDefinition(latest, BoxConfigStore.revision(latest));
        assertEquals(latest, store.snapshot().get("测试箱"));
    }

    @Test void editorNewPoolAndUnrelatedEditsPreserveOtherPools() throws IOException {
        var store = store(VALID);
        store.reload();
        var original = store.snapshot().get("测试箱");
        var added = BoxDefinition.create("copy", List.of(new RewardEntry("minecraft:diamond", 5)));
        store.saveDefinition(added, "");
        store.saveDefinition(original, BoxConfigStore.revision(original));
        assertEquals(2, store.reload());
        assertEquals(added, store.snapshot().get("copy"));
        var invalid = BoxDefinition.create("invalid", List.of());
        assertThrows(IllegalArgumentException.class, () -> store.saveDefinition(invalid, ""));
        assertEquals(2, store.reload());
    }
}

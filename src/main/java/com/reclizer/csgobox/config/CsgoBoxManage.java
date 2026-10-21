package com.reclizer.csgobox.config;

import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/** Logical-server-only configuration facade. Clients receive explicit display packets. */
public final class CsgoBoxManage {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("csbox/box.json");
    private static final BoxConfigStore STORE = new BoxConfigStore(FILE, data -> {
        try { ItemNBT.parseStrict(data); return true; }
        catch (IllegalArgumentException ex) { return false; }
    });

    private CsgoBoxManage() {}

    public static int loadConfigBox() throws IOException {
        Files.createDirectories(FILE.getParent());
        if (!Files.exists(FILE)) {
            try (InputStream defaults = CsgoBoxManage.class.getResourceAsStream("/default-boxes.json")) {
                if (defaults == null) throw new IOException("Missing default-boxes.json");
                Files.copy(defaults, FILE);
            }
        }
        return STORE.reload();
    }

    public static Collection<BoxDefinition> boxes() { return STORE.snapshot().values(); }
    public static BoxDefinition find(String id) { return STORE.snapshot().get(id); }
    public static BoxDefinition readForEditing(String id) throws IOException { return STORE.readForEditing(id); }
    public static void saveDefinition(BoxDefinition box, String revision) throws IOException {
        for (String entity : box.entity()) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(entity);
            if (id == null || !net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(id))
                throw new IllegalArgumentException("Unknown entity: " + entity);
        }
        if (box.drop() > 0 && box.entity().isEmpty()) throw new IllegalArgumentException("Drop chance requires at least one entity");
        STORE.saveDefinition(box, revision);
    }
}

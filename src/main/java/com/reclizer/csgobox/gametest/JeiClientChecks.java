package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.compat.jei.*;
import com.reclizer.csgobox.config.AcquisitionSummary;
import com.reclizer.csgobox.gui.client.ClientAcquisitions;
import com.reclizer.csgobox.gui.client.ClientPackets;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.packet.PacketBoxCatalog;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import java.util.*;

/** Real JEI list/recipe checks; optional lifecycle checks edit only temporary client caches. */
@JeiPlugin
public final class JeiClientChecks implements IModPlugin {
    private IJeiRuntime runtime;
    private int ticks;
    private boolean checked, listening;
    @Override public ResourceLocation getPluginUid() { return new ResourceLocation("csgobox","zz_client_checks"); }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!Boolean.getBoolean("csgobox.jeiSmokeTest") && !Boolean.getBoolean("csgobox.jeiVisibilityChecks")) return;
        this.runtime=runtime; ticks=0; checked=false;
        if (!listening) { MinecraftForge.EVENT_BUS.addListener(this::tick); listening=true; }
    }
    @Override public void onRuntimeUnavailable() { runtime=null; }
    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END || runtime==null || checked || ++ticks<40) return;
        var pools=ClientAcquisitions.entries();
        if (pools.isEmpty() || pools.size()!=BoxCatalog.entries().size()) return;
        checked=true;
        try {
            if (Boolean.getBoolean("csgobox.entityPickerChecks")) EntityPickerClientChecks.run();
            var ids=runtime.getIngredientManager().getAllItemStacks().stream().filter(s->s.getItem() instanceof ItemCsgoBox)
                    .map(ItemCsgoBox::boxId).collect(java.util.stream.Collectors.toSet());
            var expected=pools.stream().map(p->p.id()).collect(java.util.stream.Collectors.toSet());
            if (!ids.equals(expected)) throw new AssertionError("Configured JEI item identities differ: "+ids+" vs "+expected);
            var filter=runtime.getIngredientFilter(); var oldFilter=filter.getFilterText();
            try {
                assertVisible(expected,""); assertVisible(expected,"@csgobox");
                for (var pool:pools) {
                    filter.setFilterText(BoxCatalog.displayName(pool.id()));
                    if (filter.getFilteredItemStacks().stream().noneMatch(s->ItemCsgoBox.boxId(s).equals(pool.id())))
                        throw new AssertionError("Configured box is not searchable by name: "+pool.id());
                }
                if (Boolean.getBoolean("csgobox.jeiVisibilityChecks")) checkLifecycle();
            } finally { filter.setFilterText(oldFilter); }
            var manager=runtime.getRecipeManager(); var entries=manager.createRecipeLookup(CsgoBoxJeiPlugin.TYPE).get().toList();
            if (entries.size()!=AcquisitionRecipe.build(pools).size()) throw new AssertionError("Missing or duplicate acquisition entries");
            var factory=runtime.getJeiHelpers().getFocusFactory();
            for (var pool:pools) {
                var focus=factory.createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,AcquisitionRecipe.box(pool.id()));
                var matches=manager.createRecipeLookup(CsgoBoxJeiPlugin.TYPE).limitFocus(List.of(focus)).get().toList();
                long expectedMatches=AcquisitionRecipe.build(List.of(pool)).stream().filter(r->ItemCsgoBox.boxId(r.output()).equals(pool.id())).count();
                if (matches.size()!=expectedMatches || matches.stream().anyMatch(r->!ItemCsgoBox.boxId(r.output()).equals(pool.id())))
                    throw new AssertionError("JEI output focus merged different pool IDs: "+pool.id());
            }
            var category=manager.getRecipeCategory(CsgoBoxJeiPlugin.TYPE);
            for (var recipe:entries) if (manager.createRecipeLayoutDrawable(category,recipe,factory.getEmptyFocusGroup()).isEmpty())
                throw new AssertionError("Cannot create acquisition layout: "+recipe.fingerprint());
            CsgoBox.LOGGER.info("JEI CLIENT CHECKS PASSED: {} pools, {} acquisition entries; visible grid, name/mod search, unique IDs, output focus and layouts",pools.size(),entries.size());
        } catch (Throwable error) { CsgoBox.LOGGER.error("JEI CLIENT CHECKS FAILED",error); }
        finally { if (Boolean.getBoolean("csgobox.jeiChecksExit")) net.minecraft.client.Minecraft.getInstance().stop(); }
    }
    private void assertVisible(Set<String> expected,String query) {
        var filter=runtime.getIngredientFilter(); filter.setFilterText(query);
        var boxes=filter.getFilteredItemStacks().stream().filter(s->s.getItem() instanceof ItemCsgoBox).toList();
        var ids=boxes.stream().map(ItemCsgoBox::boxId).collect(java.util.stream.Collectors.toSet());
        if (!ids.equals(expected) || boxes.size()!=expected.size())
            throw new AssertionError("JEI visible box list differs for '"+query+"': "+ids+" ("+boxes.size()+") vs "+expected);
    }
    private void replace(List<BoxCatalog.Entry> catalog,List<AcquisitionSummary> pools) {
        ClientPackets.catalog(new PacketBoxCatalog(catalog)); pools.forEach(ClientAcquisitions::accept);
    }
    private void checkLifecycle() {
        var originalCatalog=List.copyOf(BoxCatalog.entries()); var originalPools=List.copyOf(ClientAcquisitions.entries());
        var expected=originalCatalog.stream().map(BoxCatalog.Entry::id).collect(java.util.stream.Collectors.toSet());
        try {
            replace(originalCatalog,originalPools); assertVisible(expected,"");
            var samplePool=originalPools.get(0);
            var dropRule=new com.reclizer.csgobox.config.AcquisitionDefinition(null,
                    new com.reclizer.csgobox.config.AcquisitionDefinition.Drop(true,.25F,List.of("minecraft:zombie"),1),null);
            var enabled=originalPools.stream().map(p->p.id().equals(samplePool.id())
                    ?new AcquisitionSummary(p.id(),p.key(),dropRule,p.keyRules()):p).toList();
            replace(originalCatalog,enabled);
            if (runtime.getRecipeManager().createRecipeLookup(CsgoBoxJeiPlugin.TYPE).get().count()!=AcquisitionRecipe.build(enabled).size())
                throw new AssertionError("Enabling acquisition did not add the real recipe");
            replace(originalCatalog,originalPools);
            if (runtime.getRecipeManager().createRecipeLookup(CsgoBoxJeiPlugin.TYPE).get().count()!=AcquisitionRecipe.build(originalPools).size())
                throw new AssertionError("Disabling acquisition left a placeholder or stale recipe");
            var first=originalCatalog.get(0); String renamed="JEI visibility renamed box";
            var renamedCatalog=originalCatalog.stream().map(e->e.id().equals(first.id())?new BoxCatalog.Entry(e.id(),renamed,e.appearance()):e).toList();
            replace(renamedCatalog,originalPools); assertVisible(expected,""); assertVisible(Set.of(first.id()),renamed);
            var smaller=renamedCatalog.stream().filter(e->!e.id().equals(first.id())).toList();
            var smallerPools=originalPools.stream().filter(p->!p.id().equals(first.id())).toList();
            replace(smaller,smallerPools);
            var remaining=new HashSet<>(expected); remaining.remove(first.id()); assertVisible(remaining,"");
            String addedId="csgobox:jei_visibility_check";
            var addedCatalog=new ArrayList<>(smaller); addedCatalog.add(new BoxCatalog.Entry(addedId,"JEI visibility new box"));
            var addedPools=new ArrayList<>(smallerPools); var sample=originalPools.get(0);
            addedPools.add(new AcquisitionSummary(addedId,sample.key(),sample.boxRules(),sample.keyRules()));
            replace(addedCatalog,addedPools); remaining.add(addedId); assertVisible(remaining,"@csgobox");
            replace(List.of(),List.of()); assertVisible(Set.of(),"");
            replace(originalCatalog,originalPools); assertVisible(expected,"@csgobox");
            CsgoBox.LOGGER.info("JEI VISIBILITY LIFECYCLE CHECKS PASSED: acquisition enable/disable, refresh, rename, delete, add, clear and reconnect");
        } finally { replace(originalCatalog,originalPools); }
    }
}

package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.compat.jei.*;
import com.reclizer.csgobox.gui.client.ClientAcquisitions;
import com.reclizer.csgobox.item.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import java.util.*;

/** Read-only checks against a real JEI runtime. Development fixture excluded from the release JAR. */
@JeiPlugin
public final class JeiClientChecks implements IModPlugin {
    private IJeiRuntime runtime;
    private int ticks;
    private boolean checked, listening;
    @Override public ResourceLocation getPluginUid() { return new ResourceLocation("csgobox","zz_client_checks"); }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!Boolean.getBoolean("csgobox.jeiSmokeTest")) return;
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
            var ids=runtime.getIngredientManager().getAllItemStacks().stream().filter(s->s.getItem() instanceof ItemCsgoBox)
                    .map(ItemCsgoBox::boxId).collect(java.util.stream.Collectors.toSet());
            var expected=pools.stream().map(p->p.id()).collect(java.util.stream.Collectors.toSet());
            if (!ids.equals(expected)) throw new AssertionError("Configured JEI item identities differ: "+ids+" vs "+expected);
            var manager=runtime.getRecipeManager(); var entries=manager.createRecipeLookup(CsgoBoxJeiPlugin.TYPE).get().toList();
            if (entries.size()!=AcquisitionRecipe.build(pools).size()) throw new AssertionError("Missing or duplicate acquisition entries");
            var factory=runtime.getJeiHelpers().getFocusFactory();
            for (var pool:pools) {
                var focus=factory.createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,AcquisitionRecipe.box(pool.id()));
                var matches=manager.createRecipeLookup(CsgoBoxJeiPlugin.TYPE).limitFocus(List.of(focus)).get().toList();
                if (matches.isEmpty() || matches.stream().anyMatch(r->!ItemCsgoBox.boxId(r.output()).equals(pool.id())))
                    throw new AssertionError("JEI output focus merged different pool IDs: "+pool.id());
            }
            var category=manager.getRecipeCategory(CsgoBoxJeiPlugin.TYPE);
            for (var recipe:entries) if (manager.createRecipeLayoutDrawable(category,recipe,factory.getEmptyFocusGroup()).isEmpty())
                throw new AssertionError("Cannot create acquisition layout: "+recipe.fingerprint());
            CsgoBox.LOGGER.info("JEI CLIENT CHECKS PASSED: {} pools, {} acquisition entries; unique IDs, output focus and layouts",pools.size(),entries.size());
        } catch (Throwable error) { CsgoBox.LOGGER.error("JEI CLIENT CHECKS FAILED",error); }
    }
}

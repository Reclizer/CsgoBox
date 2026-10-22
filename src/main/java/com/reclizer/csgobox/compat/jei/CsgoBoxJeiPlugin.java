package com.reclizer.csgobox.compat.jei;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.gui.client.ClientAcquisitions;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.utils.ItemNBT;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** JEI discovers this class only when installed. Common and server code never reference it. */
@JeiPlugin
public final class CsgoBoxJeiPlugin implements IModPlugin {
    public static final RecipeType<AcquisitionRecipe> TYPE = RecipeType.create(CsgoBox.MODID,"acquisition",AcquisitionRecipe.class);
    private IJeiRuntime runtime;
    private final Map<String, AcquisitionRecipe> known = new HashMap<>();
    private final Map<String, ItemStack> addedKeys = new HashMap<>();
    private List<AcquisitionRecipe> shown = List.of();
    @Override public ResourceLocation getPluginUid() { return new ResourceLocation(CsgoBox.MODID,"jei"); }
    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.ITEM_CSGOBOX.get(), (stack, context) -> ItemCsgoBox.boxId(stack));
        registration.registerSubtypeInterpreter(ModItems.ITEM_EMPTY_BOX.get(), (stack, context) -> EmptyBoxItem.appearance(stack).id());
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AcquisitionCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime; known.clear(); shown = List.of(); addedKeys.clear();
        ClientAcquisitions.setListener(this::refresh); refresh();
    }
    @Override public void onRuntimeUnavailable() {
        runtime = null; known.clear(); shown = List.of(); addedKeys.clear();
        ClientAcquisitions.setListener(() -> {});
    }
    private void refresh() {
        if (runtime == null) return;
        var ingredients = runtime.getIngredientManager();
        var manager = runtime.getRecipeManager();
        manager.hideRecipes(TYPE,shown);
        var pools = ClientAcquisitions.entries();
        // Include creative-populated variants too, so deleted IDs and renamed search entries do not linger.
        var oldBoxes = ingredients.getAllItemStacks().stream().filter(s -> s.getItem() instanceof ItemCsgoBox).toList();
        if (!oldBoxes.isEmpty()) ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,oldBoxes);
        var boxes = pools.stream().map(p -> AcquisitionRecipe.box(p.id())).toList();
        if (!boxes.isEmpty()) ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK,boxes);
        var helper = ingredients.getIngredientHelper(VanillaTypes.ITEM_STACK);
        var keys = new LinkedHashMap<String,ItemStack>();
        pools.stream().filter(p -> !p.key().isEmpty()).map(p -> ItemNBT.parseStrict(p.key()))
                .forEach(s -> keys.put(helper.getUniqueId(s,UidContext.Ingredient),s));
        var removed = addedKeys.entrySet().stream().filter(e -> !keys.containsKey(e.getKey())).map(Map.Entry::getValue).toList();
        if (!removed.isEmpty()) ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,removed);
        addedKeys.keySet().retainAll(keys.keySet());
        keys.forEach((uid,key) -> {
            if (ingredients.getTypedIngredientByUid(VanillaTypes.ITEM_STACK,uid).isEmpty()) {
                ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK,List.of(key)); addedKeys.put(uid,key);
            }
        });
        var added = new ArrayList<AcquisitionRecipe>();
        shown = AcquisitionRecipe.build(pools).stream().map(recipe -> known.computeIfAbsent(recipe.fingerprint(), id -> {
            added.add(recipe); return recipe;
        })).toList();
        if (!added.isEmpty()) manager.addRecipes(TYPE,added);
        if (!shown.isEmpty()) manager.unhideRecipes(TYPE,shown);
        CsgoBox.LOGGER.info("JEI synchronized {} configured boxes and {} acquisition entries",boxes.size(),shown.size());
    }
}

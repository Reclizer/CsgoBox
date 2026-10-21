package com.reclizer.csgobox.gui;

import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.item.BoxCatalog;
import com.reclizer.csgobox.packet.*;
import com.reclizer.csgobox.utils.ItemNBT;
import com.reclizer.csgobox.utils.WeightedSelection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.function.Consumer;

/** An operator-only configuration editor. All displayed inventory slots are read-only samples. */
public class CsgoBoxCraftScreen extends AbstractContainerScreen<CsgoBoxCraftMenu> {
    private static final int[] COLORS = {0xFF6097FF, 0xFF9D7BFF, 0xFFDC73E8, 0xFFF0747A, 0xFFEFC66B};
    private static final int TEXT = 0xFFE6ECF5, MUTED = 0xFF93A2B8;
    private static final int PAGE_SIZE = 27;
    private final List<List<ItemStack>> rewards = new ArrayList<>();
    private final List<EditBox> fields = new ArrayList<>();
    private final List<UiButton> catalogButtons = new ArrayList<>();
    private final String[] weights = {"625", "20", "6", "5", "2"};
    private String name = "", key = "csgobox:csgo_key0", drop = "0", entities = "minecraft:zombie";
    private String originalName = "", revision = "", search = "", itemId = "", luck = "0";
    private String status = "", issue = "", amountError = "";
    private boolean dirty, needsValidation = true, statusError;
    private int page, grade = 1, rewardPage, catalogPage, selected = -1;
    private int requestId, pending, pendingTicks;
    private float uiScale = 1;
    private EditBox amountField;
    private UiButton saveButton;
    private Runnable discardAction;
    private ItemStack tooltip = ItemStack.EMPTY;
    private String tooltipChance = "";

    public CsgoBoxCraftScreen(CsgoBoxCraftMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 620;
        imageHeight = 430;
        for (int i = 0; i < 5; i++) rewards.add(new ArrayList<>());
    }

    private static Component tr(String key, Object... args) { return Component.translatable("editor.csgobox." + key, args); }
    private static String text(String key, Object... args) { return tr(key, args).getString(); }

    @Override protected void init() {
        super.init();
        uiScale = Math.min(1F, Math.min((width - 8F) / imageWidth, (height - 8F) / imageHeight));
        leftPos = Math.round((width / uiScale - imageWidth) / 2);
        topPos = Math.round((height / uiScale - imageHeight) / 2);
        rebuild();
    }

    private EditBox field(int x, int y, int w, int max, String value, Consumer<String> setter) {
        EditBox input = new EditBox(font, leftPos + x, topPos + y, w, 18, Component.empty());
        input.setMaxLength(max);
        input.setValue(value);
        input.setResponder(setter);
        input.setEditable(pending == 0);
        fields.add(input);
        addRenderableWidget(input);
        return input;
    }

    private UiButton button(int x, int y, int w, Component label, Runnable action, int color) {
        UiButton button = new UiButton(leftPos + x, topPos + y, w, label, action, color);
        button.active = pending == 0;
        return addRenderableWidget(button);
    }

    private void rebuild() {
        clearWidgets();
        fields.clear();
        catalogButtons.clear();
        amountField = null;
        if (discardAction != null) {
            button(170, 238, 132, tr("discard"), () -> {
                Runnable action = discardAction; discardAction = null; dirty = false; action.run();
            }, 0xFFF0747A);
            button(318, 238, 132, tr("cancel"), () -> { discardAction = null; rebuild(); }, MUTED);
            return;
        }
        field(16, 46, 116, 128, search, value -> { search = value; catalogPage = 0; });
        for (int i = 0; i < 12; i++) {
            int index = i;
            catalogButtons.add(button(16, 74 + i * 22, 116, Component.empty(), () -> {
                List<String> names = filteredNames();
                int offset = catalogPage * 12 + index;
                if (offset < names.size()) guard(() -> load(names.get(offset)));
            }, MUTED));
        }
        button(16, 344, 28, Component.literal("<"), () -> catalogPage = Math.max(0, catalogPage - 1), MUTED);
        button(104, 344, 28, Component.literal(">"), () -> {
            if ((catalogPage + 1) * 12 < filteredNames().size()) catalogPage++;
        }, MUTED);
        button(16, 374, 116, tr("new"), () -> guard(this::newDraft), COLORS[4]);
        field(152, 46, 294, 128, name, value -> { name = value; changed(); });
        button(454, 46, 150, tr("duplicate"), () -> {
            name = uniqueCopyName(); originalName = ""; revision = ""; changed(); rebuild();
        }, MUTED);
        for (int i = 0; i < 3; i++) {
            int tab = i;
            button(152 + i * 152, 76, 144, tr(new String[]{"rewards", "rules", "probabilities"}[i]),
                    () -> { page = tab; rebuild(); }, page == i ? COLORS[4] : MUTED);
        }
        if (page == 0) buildRewards();
        if (page == 1) buildRules();
        if (page == 2) {
            field(152, 126, 90, 12, luck, value -> luck = value);
            button(254, 126, 120, tr("my_luck"), () -> {
                luck = Float.toString(minecraft.player.getLuck()); rebuild();
            }, MUTED);
        }
        saveButton = button(456, 402, 94, tr("save"), this::save, COLORS[4]);
        button(558, 402, 46, tr("close"), this::onClose, MUTED);
    }

    private void buildRewards() {
        for (int i = 0; i < 5; i++) {
            int nextGrade = i + 1;
            button(152 + i * 91, 108, 84, tr("grade" + nextGrade), () -> {
                grade = nextGrade; selected = -1; rewardPage = 0; amountError = ""; rebuild();
            }, grade == nextGrade ? COLORS[i] : MUTED);
        }
        button(152, 238, 30, Component.literal("<"), () -> { rewardPage = Math.max(0, rewardPage - 1); }, MUTED);
        button(392, 238, 30, Component.literal(">"), () -> {
            if ((rewardPage + 1) * PAGE_SIZE < current().size()) rewardPage++;
        }, MUTED);
        ItemStack chosen = selectedStack();
        amountField = field(438, 186, 72, 3, chosen.isEmpty() ? "" : Integer.toString(chosen.getCount()), value -> {
            try {
                int count = Integer.parseInt(value);
                if (selectedStack().isEmpty() || count < 1 || count > selectedStack().getMaxStackSize()) throw new NumberFormatException();
                selectedStack().setCount(count); amountError = ""; changed();
            } catch (NumberFormatException ex) { amountError = text("bad_count"); needsValidation = true; }
        });
        amountField.setEditable(pending == 0 && !chosen.isEmpty());
        UiButton remove = button(438, 214, 166, tr("remove"), this::removeSelected, 0xFFF0747A);
        remove.active = pending == 0 && !chosen.isEmpty();
        field(152, 270, 350, 256, itemId, value -> itemId = value);
        button(512, 270, 92, tr("add_item"), () -> {
            try { addSample(ItemNBT.parseStrict(itemId.trim())); }
            catch (RuntimeException ex) { fail(text("bad_item")); }
        }, MUTED);
    }

    private void buildRules() {
        field(152, 126, 308, 256, key, value -> { key = value; changed(); });
        button(470, 126, 134, tr("use_hand"), () -> {
            ItemStack hand = minecraft.player.getMainHandItem();
            if (!hand.isEmpty()) { key = ForgeRegistries.ITEMS.getKey(hand.getItem()).toString(); changed(); rebuild(); }
        }, MUTED);
        for (int i = 0; i < 5; i++) {
            int index = i;
            field(152 + i * 91, 186, 84, 10, weights[i], value -> { weights[index] = value; changed(); });
        }
        field(152, 264, 100, 16, drop, value -> { drop = value; changed(); });
        field(152, 322, 452, 8192, entities, value -> { entities = value; changed(); });
    }

    private List<String> filteredNames() {
        String query = search.toLowerCase(Locale.ROOT);
        return BoxCatalog.names().stream().filter(n -> n.toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private String uniqueCopyName() {
        String base = (name.isBlank() ? text("untitled") : name);
        if (base.length() > 108) base = base.substring(0, 108);
        String copy = base + text("copy_suffix");
        for (int i = 2; BoxCatalog.names().contains(copy); i++) copy = base + text("copy_suffix") + " " + i;
        return copy;
    }

    private void guard(Runnable action) {
        if (pending != 0) return;
        if (dirty) { discardAction = action; rebuild(); }
        else action.run();
    }

    private void newDraft() {
        for (var group : rewards) group.clear();
        name = ""; key = "csgobox:csgo_key0"; drop = "0"; entities = "minecraft:zombie";
        String[] defaults = {"625", "20", "6", "5", "2"};
        System.arraycopy(defaults, 0, weights, 0, 5);
        originalName = ""; revision = ""; selected = -1; rewardPage = 0; grade = 1; page = 0;
        dirty = false; amountError = ""; status = ""; needsValidation = true; rebuild();
    }

    private void load(String name) {
        pending = 1; pendingTicks = 0; requestId++;
        status = text("loading"); statusError = false; rebuild();
        Networking.INSTANCE.sendToServer(new PacketLoadBox(menu.containerId, requestId, name));
    }

    private void save() {
        validateDraft();
        if (!issue.isEmpty() || pending != 0) { fail(issue); return; }
        BoxDefinition definition = definition();
        String expected = definition.name().equals(originalName) ? revision : "";
        pending = 2; pendingTicks = 0; requestId++;
        status = text("saving"); statusError = false; rebuild();
        Networking.INSTANCE.sendToServer(new PacketSaveBox(menu.containerId, requestId,
                BoxEditorCodec.encode(definition), expected));
    }

    public void receive(PacketEditorState packet) {
        if (packet.containerId() != menu.containerId || packet.requestId() != requestId || pending == 0) return;
        pending = 0;
        if (!packet.error().isEmpty()) {
            fail(Component.translatable(packet.error()).getString()); rebuild(); return;
        }
        BoxDefinition box = BoxEditorCodec.decode(packet.json());
        name = box.name(); originalName = box.name(); revision = packet.revision(); key = box.key();
        drop = new java.math.BigDecimal(Float.toString(box.drop())).movePointRight(2).stripTrailingZeros().toPlainString();
        entities = String.join(", ", box.entity());
        for (int i = 0; i < 5; i++) {
            weights[i] = box.random().get(4 - i).toString();
            rewards.get(i).clear();
            for (String item : box.items(i + 1)) rewards.get(i).add(ItemNBT.parseStrict(item));
        }
        dirty = false; selected = -1; amountError = ""; rewardPage = 0; needsValidation = true;
        status = text(packet.saved() ? "saved" : "loaded"); statusError = false;
        rebuild();
    }

    private void changed() { dirty = true; needsValidation = true; status = ""; statusError = false; }
    private void fail(String message) { status = message; statusError = true; }
    private List<ItemStack> current() { return rewards.get(grade - 1); }
    private ItemStack selectedStack() { return selected >= 0 && selected < current().size() ? current().get(selected) : ItemStack.EMPTY; }
    private int rewardCount() { return rewards.stream().mapToInt(List::size).sum(); }

    private void addSample(ItemStack sample) {
        if (sample.isEmpty()) return;
        if (rewardCount() >= 256) { fail(text("reward_limit")); return; }
        current().add(sample.copy());
        selected = current().size() - 1; rewardPage = selected / PAGE_SIZE;
        amountError = ""; changed(); rebuild();
    }

    private void removeSelected() {
        if (selectedStack().isEmpty()) return;
        current().remove(selected); selected = -1; amountError = "";
        rewardPage = Math.min(rewardPage, Math.max(0, (current().size() - 1) / PAGE_SIZE));
        changed(); rebuild();
    }

    private List<Integer> parsedWeights() {
        List<Integer> result = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            int value = Integer.parseInt(weights[i]);
            if (value < 0) throw new NumberFormatException();
            result.add(value);
        }
        return result;
    }

    private BoxDefinition definition() {
        List<RewardEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++)
            for (ItemStack item : rewards.get(i)) entries.add(new RewardEntry(ItemNBT.getStacksData(item), i + 1));
        return new BoxDefinition(name.trim(), key.trim(), Float.parseFloat(drop) / 100F, parsedWeights(),
                Arrays.stream(entities.split("[,;\\s]+")).filter(s -> !s.isBlank()).distinct().toList(),
                null, null, null, null, null).withRewards(entries);
    }

    private void validateDraft() {
        if (!needsValidation) return;
        needsValidation = false; issue = "";
        if (name.isBlank()) { issue = text("name_required"); return; }
        if (!name.trim().equals(originalName) && BoxCatalog.names().contains(name.trim())) { issue = text("name_exists"); return; }
        if (!amountError.isEmpty()) { issue = amountError; return; }
        ResourceLocation keyId = ResourceLocation.tryParse(key.trim());
        if (keyId == null || !ForgeRegistries.ITEMS.containsKey(keyId) || new ItemStack(ForgeRegistries.ITEMS.getValue(keyId)).isEmpty()) {
            issue = text("bad_key"); return;
        }
        try { parsedWeights(); } catch (RuntimeException ex) { issue = text("bad_weights"); return; }
        try {
            float rate = Float.parseFloat(drop);
            if (!Float.isFinite(rate) || rate < 0 || rate > 100) throw new NumberFormatException();
        } catch (RuntimeException ex) { issue = text("bad_drop"); return; }
        BoxDefinition box = definition();
        for (String entity : box.entity()) {
            ResourceLocation id = ResourceLocation.tryParse(entity);
            if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)) { issue = text("bad_entity", entity); return; }
        }
        if (box.drop() > 0 && box.entity().isEmpty()) { issue = text("entity_required"); return; }
        try { WeightedSelection.effectiveWeights(box.random(), g -> !box.items(g).isEmpty(), 0); }
        catch (RuntimeException ex) { issue = text("empty_pool"); return; }
        try { box.validate(value -> !ItemNBT.getStacks(value).isEmpty()); }
        catch (RuntimeException ex) { issue = ex.getMessage(); }
    }

    private double[] chances(float previewLuck) {
        try {
            long[] effective = WeightedSelection.effectiveWeights(parsedWeights(), g -> !rewards.get(g - 1).isEmpty(), previewLuck);
            double total = Arrays.stream(effective).sum();
            double[] result = new double[5];
            for (int i = 0; i < 5; i++) result[i] = effective[4 - i] / total;
            return result;
        } catch (RuntimeException ex) { return new double[5]; }
    }

    private String percent(double value) { return String.format(Locale.ROOT, "%.3f%%", value * 100); }
    private void label(GuiGraphics g, String value, int x, int y, int color) { g.drawString(font, value, leftPos + x, topPos + y, color, false); }
    private void clipped(GuiGraphics g, String value, int x, int y, int w, int color) {
        label(g, font.width(value) <= w ? value : font.plainSubstrByWidth(value, w - font.width("...")) + "...", x, y, color);
    }
    private void panel(GuiGraphics g, int x, int y, int w, int h, int color) { g.fill(leftPos+x, topPos+y, leftPos+x+w, topPos+y+h, color); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int mx = (int)(mouseX / uiScale), my = (int)(mouseY / uiScale);
        tooltip = ItemStack.EMPTY; tooltipChance = "";
        validateDraft();
        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1);
        panel(g, 0, 0, imageWidth, imageHeight, 0xFC101722);
        panel(g, 0, 0, imageWidth, 3, COLORS[4]);
        panel(g, 8, 34, 132, 360, 0xFF182231);
        panel(g, 146, 102, 466, 292, 0xFF182231);
        label(g, text("title"), 16, 15, TEXT);
        label(g, text("catalog"), 16, 34, MUTED);
        label(g, text("name") + (dirty ? " *" : ""), 152, 34, MUTED);
        clipped(g, originalName.isEmpty() ? text("new_mode") : text("editing", originalName), 310, 15, 294, MUTED);
        List<String> names = filteredNames();
        catalogPage = Math.min(catalogPage, Math.max(0, (names.size() - 1) / 12));
        if (discardAction == null) {
            for (int i = 0; i < catalogButtons.size(); i++) {
                int offset = catalogPage * 12 + i;
                var b = catalogButtons.get(i);
                b.visible = offset < names.size();
                if (b.visible) b.setMessage(Component.literal(names.get(offset)));
            }
            label(g, (catalogPage+1) + "/" + Math.max(1, (names.size()+11)/12), 54, 350, MUTED);
            if (names.isEmpty()) label(g, text("no_pools"), 22, 82, MUTED);
            if (page == 0) renderRewards(g, mx, my);
            if (page == 1) renderRules(g);
            if (page == 2) renderProbabilities(g);
            String footer = !status.isEmpty() ? status : issue.isEmpty() ? text("ready", rewardCount()) : issue;
            int footerColor = statusError || (status.isEmpty() && !issue.isEmpty()) ? 0xFFF19A9A : MUTED;
            clipped(g, footer, 16, 407, 430, footerColor);
            saveButton.active = pending == 0 && issue.isEmpty() && dirty;
            saveButton.setMessage(tr(pending == 2 ? "saving_short" : !originalName.isEmpty() && !name.trim().equals(originalName) ? "save_as" : "save"));
        } else {
            panel(g, 0, 0, imageWidth, imageHeight, 0xD0101722);
            label(g, text("unsaved"), 170, 185, TEXT);
            label(g, text("discard_hint"), 170, 207, MUTED);
        }
        for (Renderable widget : renderables) widget.render(g, mx, my, partialTick);
        g.pose().popPose();
        if (!tooltip.isEmpty() && discardAction == null) {
            List<Component> lines = new ArrayList<>(getTooltipFromItem(minecraft, tooltip));
            if (!tooltipChance.isEmpty()) lines.add(Component.literal(tooltipChance));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void renderRewards(GuiGraphics g, int mx, int my) {
        double chance = chances(0)[grade-1];
        for (int i = 0; i < PAGE_SIZE; i++) {
            int x = 152 + (i%9)*30, y = 140 + (i/9)*30, index = rewardPage*PAGE_SIZE+i;
            panel(g, x, y, 26, 26, index == selected ? 0xFF40516A : 0xFF0E1621);
            if (index >= current().size()) continue;
            panel(g, x, y+24, 26, 2, COLORS[grade-1]);
            ItemStack stack = current().get(index);
            g.renderItem(stack, leftPos+x+5, topPos+y+4);
            g.renderItemDecorations(font, stack, leftPos+x+5, topPos+y+4);
            if (inside(mx, my, x, y, 26, 26)) {
                tooltip = stack;
                tooltipChance = text("entry_chance", percent(chance/current().size()));
            }
        }
        label(g, text("page_count", rewardPage+1, Math.max(1, (current().size()+PAGE_SIZE-1)/PAGE_SIZE), current().size()), 190, 244, MUTED);
        ItemStack selectedItem = selectedStack();
        clipped(g, selectedItem.isEmpty() ? text("select_reward") : selectedItem.getHoverName().getString(), 438, 143, 164, TEXT);
        label(g, selectedItem.hasTag() ? text("nbt_preserved") : text("no_nbt"), 438, 161, MUTED);
        label(g, text("quantity"), 438, 174, MUTED);
        if (!selectedItem.isEmpty()) label(g, "1-" + selectedItem.getMaxStackSize(), 520, 192, MUTED);
        label(g, text("item_id"), 152, 258, MUTED);
        label(g, text("inventory_samples"), 152, 296, MUTED);
        for (var slot : menu.slots) {
            panel(g, slot.x-1, slot.y-1, 18, 18, 0xFF0E1621);
            ItemStack stack = slot.getItem();
            g.renderItem(stack, leftPos+slot.x, topPos+slot.y);
            g.renderItemDecorations(font, stack, leftPos+slot.x, topPos+slot.y);
            if (!stack.isEmpty() && inside(mx, my, slot.x-1, slot.y-1, 18, 18)) {
                tooltip = stack; tooltipChance = text("sample_hint");
            }
        }
        label(g, text("sample_hint"), 350, 307, TEXT);
        label(g, text("sample_preserves"), 350, 324, MUTED);
        label(g, text("right_remove"), 350, 346, MUTED);
        label(g, text("reward_total", rewardCount()), 350, 370, COLORS[4]);
    }

    private void renderRules(GuiGraphics g) {
        label(g, text("key"), 152, 112, TEXT);
        label(g, text("weights"), 152, 158, TEXT);
        double[] probabilities = chances(0);
        for (int i = 0; i < 5; i++) {
            label(g, text("grade"+(i+1)), 152+i*91, 174, COLORS[i]);
            label(g, percent(probabilities[i]), 152+i*91, 214, MUTED);
        }
        label(g, text("weights_hint"), 152, 232, MUTED);
        label(g, text("drop"), 152, 250, TEXT);
        label(g, text("drop_hint"), 264, 270, MUTED);
        label(g, text("entities"), 152, 306, TEXT);
        label(g, text("entities_hint"), 152, 350, MUTED);
        label(g, text("rule_save_hint"), 152, 376, COLORS[4]);
    }

    private void renderProbabilities(GuiGraphics g) {
        label(g, text("luck"), 152, 112, TEXT);
        float previewLuck;
        try {
            previewLuck = Float.parseFloat(luck);
            if (!Float.isFinite(previewLuck)) throw new NumberFormatException();
        } catch (RuntimeException ex) {
            label(g, text("bad_luck"), 152, 164, 0xFFF19A9A); return;
        }
        double[] values = chances(previewLuck);
        for (int i = 0; i < 5; i++) {
            int y = 169 + i*36;
            label(g, text("grade"+(i+1)), 152, y, COLORS[i]);
            panel(g, 224, y, 192, 10, 0xFF0E1621);
            panel(g, 224, y, (int)Math.round(192*values[i]), 10, COLORS[i]);
            label(g, percent(values[i]), 428, y, TEXT);
            int count = rewards.get(i).size();
            label(g, text("entries", count), 518, y, MUTED);
            label(g, text("entry_chance", percent(count == 0 ? 0 : values[i]/count)), 224, y+14, MUTED);
        }
        label(g, text("probability_hint"), 152, 359, MUTED);
        label(g, text("duplicate_hint"), 152, 377, MUTED);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= leftPos+x && mx < leftPos+x+w && my >= topPos+y && my < topPos+y+h;
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        double mx = x/uiScale, my = y/uiScale;
        if (pending != 0) return true;
        if (discardAction == null && page == 0) {
            for (int i = 0; i < PAGE_SIZE; i++) {
                int index = rewardPage*PAGE_SIZE+i;
                if (index < current().size() && inside(mx, my, 152+i%9*30, 140+i/9*30, 26, 26)) {
                    selected = index; amountError = ""; needsValidation = true;
                    if (button == 1) removeSelected(); else rebuild();
                    return true;
                }
            }
            for (var slot : menu.slots) {
                if (inside(mx, my, slot.x-1, slot.y-1, 18, 18)) { addSample(slot.getItem()); return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased(x/uiScale, y/uiScale, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return super.mouseDragged(x/uiScale, y/uiScale, button, dx/uiScale, dy/uiScale); }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (pending != 0 || discardAction != null) return true;
        if (inside(x/uiScale, y/uiScale, 8, 34, 132, 360)) {
            catalogPage = Math.max(0, Math.min(Math.max(0, (filteredNames().size()-1)/12), catalogPage + (delta < 0 ? 1 : -1)));
            return true;
        }
        if (page == 0 && inside(x/uiScale, y/uiScale, 152, 140, 270, 90)) {
            rewardPage = Math.max(0, Math.min(Math.max(0, (current().size()-1)/PAGE_SIZE), rewardPage + (delta < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(x/uiScale, y/uiScale, delta);
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256) { onClose(); return true; }
        if (pending != 0) return true;
        if (hasControlDown() && key == 83 && discardAction == null) { save(); return true; }
        if (getFocused() instanceof EditBox input && input.isFocused() && key != 258) return input.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }
    @Override protected void containerTick() {
        super.containerTick();
        for (var field : fields) field.tick();
        if (pending != 0 && ++pendingTicks > 300) {
            pending = 0; requestId++; fail(text("timeout")); rebuild();
        }
    }
    @Override public void onClose() {
        if (pending == 2) return;
        if (discardAction != null) { discardAction = null; rebuild(); return; }
        if (pending == 1) { pending = 0; requestId++; }
        guard(() -> super.onClose());
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {}

    private class UiButton extends Button {
        private final int accent;
        UiButton(int x, int y, int width, Component label, Runnable action, int accent) {
            super(x, y, width, 20, label, ignored -> action.run(), DEFAULT_NARRATION);
            this.accent = accent;
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
            boolean hover = isHoveredOrFocused();
            g.fill(getX(), getY(), getX()+width, getY()+height, !active ? 0xFF202A37 : hover ? 0xFF36465B : 0xFF28364A);
            g.fill(getX(), getY()+height-1, getX()+width, getY()+height, active ? accent : 0xFF485466);
            String label = getMessage().getString();
            if (font.width(label) > width-8) label = font.plainSubstrByWidth(label, width-16) + "...";
            g.drawCenteredString(font, label, getX()+width/2, getY()+6, active ? TEXT : MUTED);
        }
    }
}

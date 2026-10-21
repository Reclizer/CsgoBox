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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
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
    private String name = "", key = "";
    private final List<Integer> displayOrder = new ArrayList<>();
    private AcquisitionDraft boxAcquisition = new AcquisitionDraft(AcquisitionDefinition.disabled());
    private AcquisitionDraft keyAcquisition = new AcquisitionDraft(AcquisitionDefinition.disabled());
    private boolean acquireKey;
    private int acquisitionPage, sampleTarget;
    private ItemStack pressedSample = ItemStack.EMPTY;
    private double samplePressX, samplePressY;
    private boolean sampleDragging;
    private final Map<String, ItemStack> sampleCache = new HashMap<>();
    private String poolId = BoxDefinition.newId(), originalId = "", revision = "", search = "", itemId = "", luck = "0";
    private String status = "", issue = "", amountError = "";
    private boolean dirty, needsValidation = true, statusError, luckEnabled = true;
    private int page, grade = 1, rewardPage, catalogPage, selected = -1;
    private int requestId, pending, pendingTicks;
    private float uiScale = 1;
    private EditBox amountField;
    private UiButton saveButton, testButton;
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
        pressedSample = ItemStack.EMPTY;
        sampleDragging = false;
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
                List<BoxCatalog.Entry> pools = filteredPools();
                int offset = catalogPage * 12 + index;
                if (offset < pools.size()) guard(() -> load(pools.get(offset).id()));
            }, MUTED));
        }
        button(16, 344, 28, Component.literal("<"), () -> catalogPage = Math.max(0, catalogPage - 1), MUTED);
        button(104, 344, 28, Component.literal(">"), () -> {
            if ((catalogPage + 1) * 12 < filteredPools().size()) catalogPage++;
        }, MUTED);
        button(16, 374, 116, tr("new"), () -> guard(this::newDraft), COLORS[4]);
        field(152, 46, 294, 128, name, value -> { name = value; changed(); });
        button(454, 46, 150, tr("duplicate"), () -> {
            name = uniqueCopyName(); poolId = BoxDefinition.newId(); originalId = ""; revision = ""; changed(); rebuild();
        }, MUTED);
        for (int i = 0; i < 3; i++) {
            int tab = i;
            button(152 + i * 152, 76, 144, tr(new String[]{"rewards", "rules", "probabilities"}[i]),
                    () -> { page = tab; rebuild(); }, page == i ? COLORS[4] : MUTED);
        }
        if (page == 0) buildRewards();
        if (page == 1) buildAcquisition();
        if (page == 2) {
            for (int i = 0; i < 5; i++) {
                int index = i;
                field(152 + i * 91, 126, 84, 10, weights[i], value -> { weights[index] = value; changed(); });
            }
            field(152, 181, 90, 12, luck, value -> luck = value).setEditable(pending == 0 && luckEnabled);
            UiButton myLuck = button(254, 181, 120, tr("my_luck"), () -> {
                luck = Float.toString(minecraft.player.getLuck()); rebuild();
            }, MUTED);
            myLuck.active = pending == 0 && luckEnabled;
            luckButton(390, 181, 214);
        }
        testButton = button(350, 402, 98, tr("test_box"), this::getTestBox, COLORS[0]);
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
        UiButton remove = button(438, 208, 166, tr("remove"), this::removeSelected, 0xFFF0747A);
        remove.active = pending == 0 && !chosen.isEmpty();
        field(152, 270, 350, 256, itemId, value -> itemId = value);
        button(512, 270, 92, tr("add_item"), () -> {
            try { addSample(ItemNBT.parseStrict(itemId.trim())); }
            catch (RuntimeException ex) {
                fail("editor.csgobox.tacz_sample_required".equals(ex.getMessage())
                        ? text("tacz_sample_required") : text("bad_item"));
            }
        }, MUTED);
        field(472, 250, 105, 8192, key, value -> { key = value; changed(); });
        button(582, 250, 22, Component.literal("×"), () -> {
            key = ""; keyAcquisition = new AcquisitionDraft(AcquisitionDefinition.disabled()); changed(); rebuild();
        }, MUTED);
    }

    private AcquisitionDraft acquisition() { return acquireKey ? keyAcquisition : boxAcquisition; }

    private void acquisitionField(int x, int y, int width, int max, String value, Consumer<String> setter) {
        field(x, y, width, max, value, next -> { setter.accept(next); changed(); });
    }

    private void buildAcquisition() {
        button(152, 108, 120, tr("acquire_box"), () -> { acquireKey = false; sampleTarget = 0; rebuild(); }, !acquireKey ? COLORS[4] : MUTED);
        button(282, 108, 120, tr("acquire_key"), () -> { acquireKey = true; sampleTarget = 0; rebuild(); }, acquireKey ? COLORS[4] : MUTED);
        if (acquireKey && key.isBlank()) return;
        var a = acquisition();
        for (int i = 0; i < 3; i++) {
            int tab = i;
            boolean enabled = i == 0 ? a.crafting : i == 1 ? a.dropping : a.trading;
            button(152 + i * 152, 138, 144, tr(new String[]{"crafting", "dropping", "trading"}[i]).copy().append(enabled ? " ✓" : ""),
                    () -> { acquisitionPage = tab; sampleTarget = 0; rebuild(); }, acquisitionPage == i ? COLORS[4] : MUTED);
        }
        boolean enabled = acquisitionPage == 0 ? a.crafting : acquisitionPage == 1 ? a.dropping : a.trading;
        button(484, 108, 120, tr(enabled ? "method_on" : "method_off"), () -> {
            if (acquisitionPage == 0) a.crafting = !a.crafting;
            else if (acquisitionPage == 1) a.dropping = !a.dropping;
            else a.trading = !a.trading;
            changed(); rebuild();
        }, enabled ? COLORS[0] : MUTED);
        if (acquisitionPage == 0) {
            acquisitionField(353, 211, 60, 2, a.craftCount, value -> a.craftCount = value);
        } else if (acquisitionPage == 1) {
            acquisitionField(152, 190, 100, 16, a.dropChance, value -> a.dropChance = value);
            acquisitionField(280, 190, 70, 2, a.dropCount, value -> a.dropCount = value);
            acquisitionField(152, 238, 452, 8192, a.entities, value -> a.entities = value);
        } else {
            acquisitionField(152, 181, 220, 128, a.profession, value -> a.profession = value);
            acquisitionField(390, 181, 54, 1, a.level, value -> a.level = value);
            acquisitionField(461, 181, 64, 4, a.uses, value -> a.uses = value);
            acquisitionField(542, 181, 62, 3, a.xp, value -> a.xp = value);
            acquisitionField(183, 228, 46, 2, a.costCount, value -> a.costCount = value);
            acquisitionField(270, 228, 46, 2, a.secondCount, value -> a.secondCount = value);
            acquisitionField(363, 228, 50, 2, a.tradeCount, value -> a.tradeCount = value);
        }
        if (acquisitionPage != 1) {
            field(152, 270, 350, 8192, itemId, value -> itemId = value);
            button(512, 270, 92, tr("set_sample"), () -> {
                try { setAcquisitionSample(ItemNBT.parseStrict(itemId.trim()), sampleTarget); }
                catch (RuntimeException ex) { fail(text("bad_item")); }
            }, MUTED);
        }
    }

    private void luckButton(int x, int y, int width) {
        button(x, y, width, tr(luckEnabled ? "luck_on" : "luck_off"), () -> {
            luckEnabled = !luckEnabled; changed(); rebuild();
        }, luckEnabled ? COLORS[4] : MUTED);
    }

    private List<BoxCatalog.Entry> filteredPools() {
        String query = search.toLowerCase(Locale.ROOT);
        return BoxCatalog.entries().stream().filter(p -> p.name().toLowerCase(Locale.ROOT).contains(query)
                || p.id().contains(query)).toList();
    }

    private String uniqueCopyName() {
        String base = (name.isBlank() ? text("untitled") : name);
        if (base.length() > 108) base = base.substring(0, 108);
        String copy = base + text("copy_suffix");
        var names = BoxCatalog.entries().stream().map(BoxCatalog.Entry::name).toList();
        for (int i = 2; names.contains(copy); i++) copy = base + text("copy_suffix") + " " + i;
        return copy;
    }

    private void guard(Runnable action) {
        if (pending != 0) return;
        if (dirty) { discardAction = action; rebuild(); }
        else action.run();
    }

    private void newDraft() {
        for (var group : rewards) group.clear();
        name = ""; key = "";
        displayOrder.clear(); sampleCache.clear(); acquireKey = false;
        boxAcquisition = new AcquisitionDraft(AcquisitionDefinition.disabled());
        keyAcquisition = new AcquisitionDraft(AcquisitionDefinition.disabled());
        luckEnabled = true;
        String[] defaults = {"625", "20", "6", "5", "2"};
        System.arraycopy(defaults, 0, weights, 0, 5);
        poolId = BoxDefinition.newId(); originalId = ""; revision = ""; selected = -1; rewardPage = 0; grade = 1; page = 0;
        dirty = false; amountError = ""; status = ""; needsValidation = true; rebuild();
    }

    private void load(String id) {
        pending = 1; pendingTicks = 0; requestId++;
        status = text("loading"); statusError = false; rebuild();
        Networking.INSTANCE.sendToServer(new PacketLoadBox(menu.containerId, requestId, id));
    }

    private void save() {
        validateDraft();
        if (!issue.isEmpty() || pending != 0) { fail(issue); return; }
        BoxDefinition definition = definition();
        String expected = definition.id().equals(originalId) ? revision : "";
        pending = 2; pendingTicks = 0; requestId++;
        status = text("saving"); statusError = false; rebuild();
        Networking.INSTANCE.sendToServer(new PacketSaveBox(menu.containerId, requestId,
                BoxEditorCodec.encode(definition), expected));
    }

    public void receive(PacketEditorState packet) {
        if (packet.containerId() != menu.containerId || packet.requestId() != requestId || pending == 0) return;
        boolean testResponse = pending == 3;
        pending = 0;
        if (!packet.error().isEmpty()) {
            fail(Component.translatable(packet.error()).getString()); rebuild(); return;
        }
        if (testResponse) {
            status = text("test_given"); statusError = false; rebuild(); return;
        }
        BoxDefinition box = BoxEditorCodec.decode(packet.json());
        luckEnabled = box.luckEnabled();
        name = box.name(); poolId = box.id(); originalId = box.id(); revision = packet.revision(); key = box.key();
        displayOrder.clear(); displayOrder.addAll(box.displayOrder()); sampleCache.clear();
        boxAcquisition = new AcquisitionDraft(box.acquisition());
        keyAcquisition = new AcquisitionDraft(box.keyAcquisition());
        for (int i = 0; i < 5; i++) {
            weights[i] = box.random().get(4 - i).toString();
            rewards.get(i).clear();
            for (String item : box.items(i + 1)) rewards.get(i).add(ItemNBT.parseStrict(item));
        }
        dirty = false; selected = -1; amountError = ""; rewardPage = 0; needsValidation = true;
        status = text(packet.saved() ? "saved" : "loaded"); statusError = false;
        rebuild();
    }

    private void getTestBox() {
        if (pending != 0 || dirty || originalId.isEmpty()) return;
        pending = 3; pendingTicks = 0; requestId++;
        status = text("test_pending"); statusError = false; rebuild();
        Networking.INSTANCE.sendToServer(new PacketTestBox(menu.containerId, requestId, originalId, revision));
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
        if (grade < 5) displayOrder.add(grade);
        selected = current().size() - 1; rewardPage = selected / PAGE_SIZE;
        amountError = ""; changed(); rebuild();
    }

    private void removeSelected() {
        if (selectedStack().isEmpty()) return;
        if (grade < 5) {
            int ordinal = 0;
            for (int i = 0; i < displayOrder.size(); i++) {
                if (displayOrder.get(i) == grade && ordinal++ == selected) { displayOrder.remove(i); break; }
            }
        }
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
        BoxDefinition grouped = BoxDefinition.create(poolId, name.trim(), entries);
        return new BoxDefinition(poolId, name.trim(), key.trim(), 0, parsedWeights(), List.of(),
                grouped.grade1(), grouped.grade2(), grouped.grade3(), grouped.grade4(), grouped.grade5(), luckEnabled,
                displayOrder, boxAcquisition.definition(), key.isBlank() ? AcquisitionDefinition.disabled() : keyAcquisition.definition());
    }

    private void validateDraft() {
        if (!needsValidation) return;
        needsValidation = false; issue = "";
        if (name.isBlank()) { issue = text("name_required"); return; }
        if (!amountError.isEmpty()) { issue = amountError; return; }
        if (!key.isBlank() && ItemNBT.getStacks(key.trim()).isEmpty()) {
            issue = text("bad_key"); return;
        }
        try { parsedWeights(); } catch (RuntimeException ex) { issue = text("bad_weights"); return; }
        try {
            BoxDefinition box = definition();
            WeightedSelection.effectiveWeights(box.random(), g -> !box.items(g).isEmpty(), 0);
            box.validate(value -> !ItemNBT.getStacks(value).isEmpty());
            com.reclizer.csgobox.server.ConfiguredAcquisition.validate(box);
        } catch (RuntimeException ex) { issue = Component.translatable(Objects.toString(ex.getMessage(), "editor.csgobox.empty_pool")).getString(); }
    }

    private double[] chances(float previewLuck) {
        try {
            long[] effective = WeightedSelection.effectiveWeights(parsedWeights(), g -> !rewards.get(g - 1).isEmpty(), luckEnabled ? previewLuck : 0);
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
        clipped(g, text("pool_id", poolId), 310, 15, 294, MUTED);
        List<BoxCatalog.Entry> pools = filteredPools();
        catalogPage = Math.min(catalogPage, Math.max(0, (pools.size() - 1) / 12));
        if (discardAction == null) {
            for (int i = 0; i < catalogButtons.size(); i++) {
                int offset = catalogPage * 12 + i;
                var b = catalogButtons.get(i);
                b.visible = offset < pools.size();
                if (b.visible) b.showPool(pools.get(offset));
            }
            label(g, (catalogPage+1) + "/" + Math.max(1, (pools.size()+11)/12), 54, 350, MUTED);
            if (pools.isEmpty()) label(g, text("no_pools"), 22, 82, MUTED);
            if (page == 0) renderRewards(g, mx, my);
            if (page == 1) renderAcquisition(g, mx, my);
            if (page == 2) renderProbabilities(g);
            String footer = !status.isEmpty() ? status : issue.isEmpty() ? text("ready", rewardCount()) : issue;
            if (inside(mx, my, 350, 402, 98, 20) && pending == 0)
                footer = text(dirty || originalId.isEmpty() ? "test_save_first" : "test_hint");
            int footerColor = statusError || (status.isEmpty() && !issue.isEmpty()) ? 0xFFF19A9A : MUTED;
            clipped(g, footer, 16, 407, 326, footerColor);
            testButton.active = pending == 0 && !dirty && !originalId.isEmpty();
            saveButton.active = pending == 0 && issue.isEmpty() && dirty;
            saveButton.setMessage(tr(pending == 2 ? "saving_short" : "save"));
        } else {
            panel(g, 0, 0, imageWidth, imageHeight, 0xD0101722);
            label(g, text("unsaved"), 170, 185, TEXT);
            label(g, text("discard_hint"), 170, 207, MUTED);
        }
        for (Renderable widget : renderables) widget.render(g, mx, my, partialTick);
        if (sampleDragging && !pressedSample.isEmpty()) {
            g.pose().pushPose(); g.pose().translate(0, 0, 600);
            g.renderItem(pressedSample, mx - 8, my - 8);
            g.pose().popPose();
        }
        g.pose().popPose();
        if (!tooltip.isEmpty() && discardAction == null && pressedSample.isEmpty()) {
            List<Component> lines = new ArrayList<>(getTooltipFromItem(minecraft, tooltip));
            if (!tooltipChance.isEmpty()) lines.add(Component.literal(tooltipChance));
            g.renderTooltip(font, lines, tooltip.getTooltipImage(), tooltip, mouseX, mouseY);
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
            if (!previewVisible(grade, index)) label(g, "—", x+1, y+1, MUTED);
            if (inside(mx, my, x, y, 26, 26)) {
                tooltip = stack;
                tooltipChance = text("entry_chance", percent(chance/current().size()));
                if (!previewVisible(grade, index)) tooltipChance += " · " + text("hidden_reward");
            }
        }
        label(g, text("page_count", rewardPage+1, Math.max(1, (current().size()+PAGE_SIZE-1)/PAGE_SIZE), current().size()), 190, 244, MUTED);
        ItemStack selectedItem = selectedStack();
        clipped(g, selectedItem.isEmpty() ? text("select_reward") : selectedItem.getHoverName().getString(), 438, 143, 164, TEXT);
        String modelId = com.reclizer.csgobox.compat.TaczCompat.gunId(selectedItem);
        clipped(g, modelId.isEmpty() ? selectedItem.hasTag() ? text("nbt_preserved") : text("no_nbt")
                : modelId, 438, 161, 164, MUTED);
        label(g, text("quantity"), 438, 174, MUTED);
        if (!selectedItem.isEmpty()) label(g, "1-" + selectedItem.getMaxStackSize(), 520, 192, MUTED);
        label(g, text("item_id"), 152, 258, MUTED);
        label(g, text("key"), 438, 231, MUTED);
        sampleSlot(g, mx, my, 438, 242, key, false);
        renderSamples(g, mx, my);
        clipped(g, text("sample_drag_key"), 350, 307, 254, TEXT);
        label(g, text("regular_total", displayOrder.size(), BoxDefinition.RECOMMENDED_REGULAR_LIMIT), 350, 326,
                displayOrder.size() > BoxDefinition.RECOMMENDED_REGULAR_LIMIT ? COLORS[4] : MUTED);
        clipped(g, text("regular_limit_hint"), 350, 343, 254, MUTED);
        label(g, text("reward_total", rewardCount()), 350, 370, COLORS[4]);
    }

    private void renderSamples(GuiGraphics g, int mx, int my) {
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
    }

    private boolean previewVisible(int tier, int index) {
        if (tier == 5) return true;
        int ordinal = 0;
        for (int i = 0; i < Math.min(displayOrder.size(), BoxDefinition.RECOMMENDED_REGULAR_LIMIT); i++)
            if (displayOrder.get(i) == tier && ordinal++ == index) return true;
        return false;
    }

    private void sampleSlot(GuiGraphics g, int mx, int my, int x, int y, String value, boolean selected) {
        panel(g, x, y, 26, 26, selected ? 0xFF40516A : 0xFF0E1621);
        ItemStack stack = sampleCache.computeIfAbsent(value, ItemNBT::getStacks);
        g.renderItem(stack, leftPos+x+5, topPos+y+5);
        if (inside(mx, my, x, y, 26, 26) && !stack.isEmpty()) {
            tooltip = stack; tooltipChance = text("sample_target_hint");
        }
    }

    private void renderAcquisition(GuiGraphics g, int mx, int my) {
        if (acquireKey && key.isBlank()) {
            label(g, text("key_disabled_hint"), 152, 150, MUTED); return;
        }
        var a = acquisition();
        if (acquisitionPage == 0) {
            label(g, text("recipe_grid"), 152, 166, TEXT);
            for (int i = 0; i < 9; i++)
                sampleSlot(g, mx, my, 152+i%3*28, 181+i/3*28, a.ingredients.get(i), sampleTarget == i);
            label(g, "→", 249, 213, MUTED);
            renderAcquisitionOutput(g, 285, 208);
            label(g, text("output_count"), 353, 197, MUTED);
            clipped(g, text("recipe_hint"), 432, 209, 172, MUTED);
        } else if (acquisitionPage == 1) {
            label(g, text("acquisition_drop_chance"), 152, 174, TEXT);
            label(g, text("output_count"), 280, 174, MUTED);
            label(g, text("entities"), 152, 222, TEXT);
            clipped(g, text("entities_hint"), 152, 268, 452, MUTED);
        } else {
            label(g, text("profession"), 152, 167, MUTED);
            label(g, text("trade_level"), 390, 167, MUTED);
            label(g, text("trade_uses"), 461, 167, MUTED);
            label(g, text("trade_xp"), 542, 167, MUTED);
            label(g, text("cost"), 152, 208, MUTED);
            label(g, text("second_cost"), 239, 208, MUTED);
            label(g, text("output_count"), 326, 208, MUTED);
            sampleSlot(g, mx, my, 152, 225, a.cost, sampleTarget == 0);
            sampleSlot(g, mx, my, 239, 225, a.secondCost, sampleTarget == 1);
            renderAcquisitionOutput(g, 326, 225);
            clipped(g, text("trade_hint"), 432, 225, 172, MUTED);
        }
        renderSamples(g, mx, my);
        clipped(g, text("acquisition_sample_hint"), 350, 307, 254, TEXT);
        clipped(g, text("sample_preserves"), 350, 326, 254, MUTED);
        clipped(g, text(acquisitionPage == 2 ? "existing_trade_hint" : "acquisition_apply_hint"), 350, 347, 254, MUTED);
        clipped(g, text("methods_independent"), 350, 373, 254, COLORS[4]);
    }

    private void renderAcquisitionOutput(GuiGraphics g, int x, int y) {
        ItemStack stack;
        if (acquireKey) stack = sampleCache.computeIfAbsent(key, ItemNBT::getStacks);
        else {
            stack = new ItemStack(com.reclizer.csgobox.item.ModItems.ITEM_CSGOBOX.get());
            stack.getOrCreateTag().putString("PoolId", poolId);
        }
        panel(g, x, y, 26, 26, 0xFF0E1621);
        g.renderItem(stack, leftPos+x+5, topPos+y+5);
    }

    private void renderProbabilities(GuiGraphics g) {
        for (int i = 0; i < 5; i++) label(g, text("grade"+(i+1)) + " · " + text("weight"), 152+i*91, 112, COLORS[i]);
        clipped(g, text("weights_hint"), 152, 150, 452, MUTED);
        label(g, text("luck"), 152, 167, TEXT);
        float previewLuck;
        try {
            previewLuck = luckEnabled ? Float.parseFloat(luck) : 0;
            if (!Float.isFinite(previewLuck)) throw new NumberFormatException();
        } catch (RuntimeException ex) {
            label(g, text("bad_luck"), 152, 214, 0xFFF19A9A); return;
        }
        double[] values = chances(previewLuck);
        for (int i = 0; i < 5; i++) {
            int y = 214 + i*29;
            label(g, text("grade"+(i+1)), 152, y, COLORS[i]);
            panel(g, 224, y, 192, 10, 0xFF0E1621);
            panel(g, 224, y, (int)Math.round(192*values[i]), 10, COLORS[i]);
            label(g, percent(values[i]), 428, y, TEXT);
            int count = rewards.get(i).size();
            label(g, text("entries", count), 518, y, MUTED);
            label(g, text("entry_chance", percent(count == 0 ? 0 : values[i]/count)), 224, y+14, MUTED);
        }
        clipped(g, text("overview_total", displayOrder.size(), rewards.get(4).size(),
                Math.min(displayOrder.size(), BoxDefinition.RECOMMENDED_REGULAR_LIMIT)), 152, 364, 452, MUTED);
        clipped(g, text("hidden_chance_hint"), 152, 379, 452, MUTED);
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
            if (inside(mx, my, 438, 242, 26, 26)) {
                if (button == 1) setKeySample(ItemStack.EMPTY);
                return true;
            }
        }
        if (discardAction == null && page == 1 && !(acquireKey && key.isBlank())) {
            int target = acquisitionTargetAt(mx, my);
            if (target >= 0) {
                sampleTarget = target;
                if (button == 1) setAcquisitionSample(ItemStack.EMPTY, target); else rebuild();
                return true;
            }
        }
        if (discardAction == null && (page == 0 || page == 1 && !(acquireKey && key.isBlank()))) {
            for (var slot : menu.slots) if (inside(mx, my, slot.x-1, slot.y-1, 18, 18)) {
                if (button == 0 && !slot.getItem().isEmpty()) {
                    pressedSample = slot.getItem().copy(); samplePressX = mx; samplePressY = my; sampleDragging = false;
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private int acquisitionTargetAt(double mx, double my) {
        if (acquisitionPage == 0) {
            for (int i = 0; i < 9; i++) if (inside(mx, my, 152+i%3*28, 181+i/3*28, 26, 26)) return i;
        } else if (acquisitionPage == 2) {
            if (inside(mx, my, 152, 225, 26, 26)) return 0;
            if (inside(mx, my, 239, 225, 26, 26)) return 1;
        }
        return -1;
    }

    private void setKeySample(ItemStack sample) {
        ItemStack copy = sample.copy();
        if (!copy.isEmpty()) copy.setCount(1);
        key = copy.isEmpty() ? "" : ItemNBT.getStacksData(copy);
        if (key.isEmpty()) keyAcquisition = new AcquisitionDraft(AcquisitionDefinition.disabled());
        sampleCache.clear(); changed(); rebuild();
    }

    private void setAcquisitionSample(ItemStack sample, int target) {
        var a = acquisition();
        ItemStack copy = sample.copy();
        if (acquisitionPage == 0) {
            if (!copy.isEmpty()) copy.setCount(1);
            a.ingredients.set(target, copy.isEmpty() ? "" : ItemNBT.getStacksData(copy));
        } else if (acquisitionPage == 2) {
            String value = copy.isEmpty() ? "" : ItemNBT.getStacksData(copy);
            String count = Integer.toString(copy.isEmpty() ? 1 : copy.getCount());
            if (target == 0) { a.cost = value; a.costCount = count; }
            else { a.secondCost = value; a.secondCount = count; }
        } else return;
        sampleTarget = target; sampleCache.clear(); changed(); rebuild();
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && !pressedSample.isEmpty()) {
            double mx = x/uiScale, my = y/uiScale;
            ItemStack sample = pressedSample;
            boolean dragged = sampleDragging;
            pressedSample = ItemStack.EMPTY; sampleDragging = false;
            if (pending != 0 || discardAction != null) return true;
            if (page == 0) {
                if (dragged && inside(mx, my, 438, 242, 26, 26)) setKeySample(sample);
                else if (!dragged || inside(mx, my, 152, 140, 270, 90)) addSample(sample);
            } else if (page == 1 && !(acquireKey && key.isBlank())) {
                int target = dragged ? acquisitionTargetAt(mx, my) : sampleTarget;
                if (target >= 0) setAcquisitionSample(sample, target);
            }
            return true;
        }
        return super.mouseReleased(x/uiScale, y/uiScale, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && !pressedSample.isEmpty()) {
            if (Math.hypot(x/uiScale - samplePressX, y/uiScale - samplePressY) > 3) sampleDragging = true;
            return true;
        }
        return super.mouseDragged(x/uiScale, y/uiScale, button, dx/uiScale, dy/uiScale);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (pending != 0 || discardAction != null) return true;
        if (inside(x/uiScale, y/uiScale, 8, 34, 132, 360)) {
            catalogPage = Math.max(0, Math.min(Math.max(0, (filteredPools().size()-1)/12), catalogPage + (delta < 0 ? 1 : -1)));
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
        if (pending == 2 || pending == 3) return;
        if (discardAction != null) { discardAction = null; rebuild(); return; }
        if (pending == 1) { pending = 0; requestId++; }
        guard(() -> super.onClose());
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {}

    private class UiButton extends Button {
        private final int accent;
        private BoxCatalog.Entry catalogEntry;
        UiButton(int x, int y, int width, Component label, Runnable action, int accent) {
            super(x, y, width, 20, label, ignored -> action.run(), DEFAULT_NARRATION);
            this.accent = accent;
        }
        void showPool(BoxCatalog.Entry pool) {
            if (pool.equals(catalogEntry)) return;
            catalogEntry = pool;
            setMessage(Component.literal(pool.name()));
            setTooltip(Tooltip.create(Component.literal(pool.name() + "\n" + pool.id())));
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

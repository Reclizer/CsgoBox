package com.reclizer.csgobox.gui;

import com.reclizer.csgobox.packet.Networking;
import com.reclizer.csgobox.packet.PacketSaveBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CsgoBoxCraftScreen extends AbstractContainerScreen<CsgoBoxCraftMenu> {
    private EditBox boxName;

    public CsgoBoxCraftScreen(CsgoBoxCraftMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 206;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        String previousName = boxName == null ? "" : boxName.getValue();
        super.init();
        boxName = new EditBox(font, leftPos + 63, topPos + 42, 76, 16,
                Component.translatable("gui.csgobox.csgo_box_craft.box_name"));
        boxName.setMaxLength(128);
        boxName.setValue(previousName);
        addRenderableWidget(boxName);
        addRenderableWidget(Button.builder(Component.translatable("gui.csgobox.csgo_box_craft.button_down"), button -> {
            if (!boxName.getValue().isBlank())
                Networking.INSTANCE.sendToServer(new PacketSaveBox(menu.containerId, boxName.getValue()));
        }).bounds(leftPos + 146, topPos + 39, 45, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(new ResourceLocation("csgobox:textures/screens/csgo_table.png"),
                leftPos - 4, topPos - 38, 0, 0, 204, 241, 512, 512);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    protected void containerTick() { super.containerTick(); boxName.tick(); }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256) { onClose(); return true; }
        if (boxName.isFocused()) return boxName.keyPressed(key, scanCode, modifiers);
        return super.keyPressed(key, scanCode, modifiers);
    }
}

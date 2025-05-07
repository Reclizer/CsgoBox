package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reclizer.csgobox.capability.ModCapability;
import com.reclizer.csgobox.capability.csbox.ICsboxCap;
import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.BlurHandler;
import com.reclizer.csgobox.utils.ChatMessageUtils;
import com.reclizer.csgobox.utils.ColorTools;
import com.reclizer.csgobox.utils.GuiItemMove;
import com.reclizer.csgobox.utils.ItemNBT;
import com.reclizer.csgobox.utils.RenderFontTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CsLookItemScreen extends Screen {

    private final Level world;
    private final Player entity;
    ImageButton imagebutton_back_box;
    
    public CsLookItemScreen() {
        super(Component.literal("look_item"));
        this.minecraft = Minecraft.getInstance();

        if (Minecraft.getInstance().player != null) {
            this.world = Minecraft.getInstance().player.level();
            this.entity = Minecraft.getInstance().player;
        } else {
            this.world = null;
            this.entity = null;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        this.renderLabels(guiGraphics, mouseX, mouseY);
        this.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public float itemRotX;
    public float itemRotY;
    private float xMouse;
    private float yMouse;

    // Flag to track if we've already shown the message
    private boolean hasDisplayedMessage = false;

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        boolean isInRange = (pMouseX >= this.width * 37F / 100 && pMouseX <= this.width * 37F / 100 + 200) && (pMouseY >= this.height * 12F / 100 && pMouseY <= this.height * 12F / 100 + 176);

        if (pButton == 0 && isInRange) {
            this.itemRotX = GuiItemMove.renderRotAngleX(pDragX, this.itemRotX);
            this.itemRotY = GuiItemMove.renderRotAngleY(pDragY, this.itemRotY);
            this.xMouse = (float) pDragX;
            this.yMouse = (float) pDragY;
        }

        super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            BlurHandler.updateShader(true);
            this.minecraft.options.hideGui = false;
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics) {
        if (this.minecraft.level != null) {
            pGuiGraphics.fillGradient(0, 0, this.width, this.height, BlurHandler.getBackgroundColor(), BlurHandler.getBackgroundColor());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ScreenEvent.BackgroundRendered(this, pGuiGraphics));
        } else {
            this.renderDirtBackground(pGuiGraphics);
        }
    }

    public ItemStack openItem;
    public int grade;
    public boolean openSwitch = true;

    @Override
    public final void tick() {
        super.tick();

        if (this.minecraft == null) {
            return;
        }
        if (this.minecraft.player == null) {
            return;
        }
        if (this.minecraft.player.isAlive() && !this.minecraft.player.isRemoved()) {
            this.containerTick();
        } else {
            this.minecraft.player.closeContainer();
        }
    }

    public void containerTick() {
        // Don't process if we've already loaded the item
        if (!openSwitch) {
            return;
        }
        
        // Play finish sound
        entity.playSound(ModSounds.CS_FINSH.get(), 10F, 1F);
        
        // Get the item data from capability
        ICsboxCap iCsboxCap = ModCapability.getSeed(this.entity).orElse(null);
        if (iCsboxCap == null) {
            return;
        }

        // Get the item and grade
        ItemStack itemStack = ItemNBT.getStacks(iCsboxCap.getItem());
        if (!itemStack.isEmpty()) {
            openItem = itemStack;
            grade = iCsboxCap.getGrade();
            
            // Display the message after a short delay to avoid competing with sound
            Minecraft.getInstance().tell(() -> {
                if (entity != null && !hasDisplayedMessage) {
                    ChatMessageUtils.sendItemObtainedMessage(entity, itemStack, itemStack.getCount(), grade);
                    hasDisplayedMessage = true;
                }
            });
        }
        
        openSwitch = false;
    }

    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Style style = Style.EMPTY.withBold(true);
        if (openItem == null) {
            return;
        }

        Component component = openItem.getItem().getName(openItem);
        component.getStyle().applyTo(style);
        FormattedCharSequence pText = component.getVisualOrderText();
        renderText(guiGraphics, pText, this.width * 45F / 100F, this.height * 5F / 100F, 1.8F);

        renderText(guiGraphics, Component.translatable("gui.csgobox.csgo_box.grade" + grade).getVisualOrderText(), this.width * 45F / 100F, this.height * 11F / 100F, 1F);

        renderText(guiGraphics, Component.translatable("gui.csgobox.csgo_box.back_box").withStyle(style).getVisualOrderText(), (float) this.width * 72.5F / 100F, (float) this.height * 95 / 100, 0.8F);
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BlurHandler.updateShader(false);
        this.minecraft.options.hideGui = true;

        int FrameWidth = width * 26 / 100;
        float scale = FrameWidth / 16F;
        if (openItem == null) {
            return;
        }
        guiGraphics.fill(this.width * 25 / 100, this.height * 92 / 100, this.width * 75 / 100, this.height * 92 / 100 + 1, 0xFFD3D3D3);

        guiGraphics.fill(this.width * 37 / 100, this.height * 16 / 100, this.width * 63 / 100, this.height * 16 / 100 + 2, ColorTools.colorItems(grade));

        GuiItemMove.renderItemInInventoryFollowsMouse(guiGraphics, this.width * 37 / 100, this.height * 30 / 100, this.itemRotX, this.itemRotY, openItem, this.entity, scale);
        RenderSystem.disableBlend();
    }

    private void renderText(GuiGraphics guiGraphics, FormattedCharSequence pText, float px, float py, float scale) {
        RenderFontTool.drawString(guiGraphics, this.font, pText, px, py, 0, 0, scale, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public void init() {
        super.init();
        imagebutton_back_box = new ImageButton(this.width * 72 / 100, this.height * 94 / 100, this.width * 4 / 100, this.height * 5 / 100, 0, 0, 64, new ResourceLocation("csgobox:textures/screens/atlas/back_box.png"), 82, 128, e -> {
            this.minecraft.player.closeContainer();
            BlurHandler.updateShader(true);
            this.minecraft.options.hideGui = false;
        });
        this.addRenderableWidget(imagebutton_back_box);
    }
}

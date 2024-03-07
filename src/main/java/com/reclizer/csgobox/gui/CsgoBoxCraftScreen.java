package com.reclizer.csgobox.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
@OnlyIn(Dist.CLIENT)
public class CsgoBoxCraftScreen extends AbstractContainerScreen<CsgoBoxCraftMenu> {
    private final static HashMap<String, Object> guistate = CsgoBoxCraftMenu.guistate;
    private final Level world;
    private final int x, y, z;
    private final Player entity;
    EditBox boxName;

    public CsgoBoxCraftScreen(CsgoBoxCraftMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);

        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.imageWidth = 176;
        this.imageHeight = 166;

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        boxName.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        guiGraphics.blit(new ResourceLocation("csgobox:textures/screens/csgo_table.png"), this.leftPos + -4, this.topPos + -38, 0, 0, 512, 512, 512, 512);

        RenderSystem.disableBlend();
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        if (boxName.isFocused())
            return boxName.keyPressed(key, b, c);
        return super.keyPressed(key, b, c);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        boxName.tick();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    Button buttonDown;
    public List<String> itemListExport(Entity entity){
        List<String> itemName=new ArrayList<>();
        if (entity == null)
            return null;
        if(entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots){
            ItemStack stack=ItemStack.EMPTY;
            for(int i=0;i<35;i++){
                stack=((Slot) _slots.get(i)).getItem();



                String itemid= ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();

                if(stack.getTag()!=null){
                    //System.out.println(stack.getTag().getType().)
                    //System.out.println(stack.getTag().getType().getName());
                    itemid=itemid+".withTags"+ ItemNBT.readTags(stack);
                }


                if(!itemid.equals("minecraft:air")){
                itemName.add(itemid);
                }
            }

        }
        return itemName;
    }
    public List<Integer> gradeListExport(Entity entity){
        List<Integer> itemGrade=new ArrayList<>();
        if (entity == null)
            return null;
        if(entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots){
            ItemStack stack=ItemStack.EMPTY;
            int grade=1;
            for(int i=0;i<35;i++){
                stack=((Slot) _slots.get(i)).getItem();
                String itemid= ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                if(i>6){
                    grade=2;
                }
                if(i>11){
                    grade=3;
                }
                if(i>14){
                    grade=4;
                }
                if(i>16){
                    grade=5;
                }
                if(!itemid.equals("minecraft:air")){
                    itemGrade.add(grade);
                }
            }

        }
        return itemGrade;
    }

    @Override
    public void init() {
        super.init();
        boxName = new EditBox(this.font, this.leftPos + 63, this.topPos + 42, 60, 9, Component.translatable("gui.rec.csgo_box_craft.box_name")) {
            @Override
            public void insertText(String text) {
                super.insertText(text);
                if (getValue().isEmpty())
                    setSuggestion(Component.translatable("gui.csgobox.csgo_box_craft.box_name").getString());
                else
                    setSuggestion(null);
            }

            @Override
            public void moveCursorTo(int pos) {
                super.moveCursorTo(pos);
                if (getValue().isEmpty())
                    setSuggestion(Component.translatable("gui.csgobox.csgo_box_craft.box_name").getString());
                else
                    setSuggestion(null);
            }
        };
        boxName.setSuggestion(Component.translatable("gui.csgobox.csgo_box_craft.box_name").getString());
        boxName.setMaxLength(32767);
        guistate.put("text:box_name", boxName);
        this.addWidget(this.boxName);


        buttonDown = Button.builder(Component.translatable("gui.csgobox.csgo_box_craft.button_down"), e -> {
            if(boxName !=null){
                itemListExport(this.entity);
                try {
                    CsgoBoxManage.updateBoxJson(this.boxName.getValue(),itemListExport(this.entity),gradeListExport(this.entity));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                //System.out.println(boxName.getValue());
            }

        }).bounds(this.leftPos + 146, this.topPos + 31, 35, 20).build();
        guistate.put("button:button_down", buttonDown);
        this.addRenderableWidget(buttonDown);

    }
}


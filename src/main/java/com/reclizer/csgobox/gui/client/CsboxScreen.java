package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reclizer.csgobox.CsgoBox;

import com.reclizer.csgobox.item.ItemCsgoBox;
import com.reclizer.csgobox.packet.Networking;
import com.reclizer.csgobox.packet.PacketCsgoProgress;
import com.reclizer.csgobox.utils.BlurHandler;
import com.reclizer.csgobox.utils.GuiItemMove;
import com.reclizer.csgobox.utils.IconListTools;
import com.reclizer.csgobox.utils.RenderFontTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Supplier;

public class CsboxScreen extends Screen {


    private final Level world;


    private final Player entity;
    public int gameTick=0;
    //private final PoseStack pose;
    //private final ItemRenderer itemRenderer;
    //private  ItemRenderer itemRenderer;
    ImageButton imagebutton_open_box;
    ImageButton imagebutton_back_box;

    public  float itemRotX;
    public  float itemRotY;
    private Supplier<ItemStack> iconGenerator = () -> {
        return ItemStack.EMPTY;
    };

    public Map<ItemStack , Integer> itemGroup;


    List<ItemStack> itemsList;
    List <Integer> gradeList;

    private int leftPos=0;
    private  int topPos=0;
    public CsboxScreen() {
        super(Component.literal("cs_screen"));

        if(Minecraft.getInstance().player!=null){
            this.minecraft=Minecraft.getInstance();
            this.entity=Minecraft.getInstance().player;
            this.world=entity.level();
            ItemStack boxStack=Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemCsgoBox box=(ItemCsgoBox)boxStack.getItem();

            //this.itemList=
            this.itemMenu=boxStack;

            this.itemGroup=box.getItemGroup(itemMenu);

            this.itemsList=itemsListProgress(this.itemGroup);
            this.gradeList=gradeListProgress(this.itemGroup);

            if(ItemCsgoBox.getKey(itemMenu)!=null){
                ResourceLocation resourceLocation=new ResourceLocation(ItemCsgoBox.getKey(itemMenu));

                itemKey=new ItemStack(ForgeRegistries.ITEMS.getValue(resourceLocation));
                //boxName=ItemCsgoBox.getName(itemMenu);
            }
        }else {
            this.entity=null;
            this.world=null;
        }


    }
    //public Component boxName;

    public ItemStack itemKey;
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static List <ItemStack> itemsListProgress(Map<ItemStack , Integer> itemList){
        List <ItemStack> itemStacks=new ArrayList<>();
        for(int i=1;i<6;i++){
            for (Map.Entry<ItemStack, Integer> entry : itemList.entrySet()) {
                if (entry.getValue() == i) {
                    itemStacks.add(entry.getKey());
                }
            }
        }
        return itemStacks;
    }

    public static List <Integer> gradeListProgress(Map<ItemStack , Integer> itemList){
        List <Integer> itemStacks=new ArrayList<>();
        for(int i=1;i<6;i++){
            for (Map.Entry<ItemStack, Integer> entry : itemList.entrySet()) {
                if (entry.getValue() == i) {
                    itemStacks.add(i);
                }
            }
        }
        return itemStacks;
    }



    // 元素的初始位置
    private int startX = this.leftPos + -11;
    private int startY = this.topPos + -7;

    // 元素的目标位置
    private int targetX = this.leftPos + -11+500;
    private int targetY = this.topPos + -7;


    private int currentX=startX;

    private int currentY=startY;

    public ItemStack itemMenu;

    /** The old x position of the mouse pointer */
    private float xMouse;
    /** The old y position of the mouse pointer */
    private float yMouse;


    @Override
    public void renderBackground(GuiGraphics pGuiGraphics){
        if(this.minecraft==null){
            return;
        }
        if(this.minecraft.player==null){
            return;
        }
        if (this.minecraft.level != null) {
            pGuiGraphics.fillGradient(0, 0, this.width, this.height, BlurHandler.getBackgroundColor(), BlurHandler.getBackgroundColor());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ScreenEvent.BackgroundRendered(this, pGuiGraphics));
        } else {
            this.renderDirtBackground(pGuiGraphics);
        }
    }



    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        boolean isInRange = (pMouseX >= this.width*37F/100 && pMouseX <= this.width*37F/100+200)&&(pMouseY >= this.height*12F/100 && pMouseY <= this.height*12F/100+176);
        //this.width*37/100 this.height*12/100
        if( pButton==0&&isInRange){
            this.itemRotX= GuiItemMove.renderRotAngleX(pDragX,this.itemRotX);
            this.itemRotY=GuiItemMove.renderRotAngleY(pDragY,this.itemRotY);
            this.xMouse = (float)pDragX;
            this.yMouse = (float)pDragY;
        }

        super.mouseDragged(pMouseX,pMouseY,pButton,pDragX,pDragY);
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        this.renderLabels(guiGraphics,mouseX,mouseY);
        this.renderBg(guiGraphics,partialTicks,mouseX,mouseY);

        // 启用混合和模糊效果
        RenderSystem.enableBlend();

        ItemStack itemStack = new ItemStack(Items.WOODEN_SWORD);
        //guiGraphics.fill( this.leftPos + -11, this.topPos + 20, this.leftPos + -11+500, this.topPos + 20+10,0xFFFF0000);
        //guiGraphics.fill(pX,pY,pX+2,pY+32+scale*2,color);
        int pX=this.width*4/100;
        int pY=this.height*54/100;


        RenderSystem.disableBlend();
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        //this.renderTooltip(guiGraphics, mouseX, mouseY);


    }



    //@Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        // 获取屏幕纹理
        // 启用混合和模糊效果
        RenderSystem.enableBlend();

        //文字ui部分===========================================================================================================================

        guiGraphics.fill( this.width*3/100,this.height*53/100,this.width*97/100,this.height*53/100+1,0xFFD3D3D3);

        guiGraphics.fill( this.width*25/100,this.height*92/100,this.width*75/100,this.height*92/100+1,0xFFD3D3D3);
        //test
        //guiGraphics.fill( this.width*25/100,this.height*92/100,this.width*75/100,this.height*92/100+1,0xFFFF0000);

        //=====================================================================================================================================
        //NativeImage capturedBackground = captureScreenBackground();
        BlurHandler.updateShader(false);
        this.minecraft.options.hideGui=true;
        // 在此处渲染你的GUI元素
        //Framebuffer.
        // 禁用混合
        RenderSystem.disableBlend();
        //==========================
        //GuiItemMove.renderEntityInInventoryFollowsMouse(guiGraphics, i + 51, j + 75, 60, (float)(i + 51) - this.xMouse, (float)(j + 75 - 50) - this.yMouse, ModItems.ITEM_CSGOBOX.get(),this.minecraft.player);

        //GuiItemMove.renderItemInInventoryFollowsMouse(guiGraphics,this.leftPos,this.topPos,this.itemRotY,this.itemRotX,ModItems.ITEM_CSGOBOX.get(), this.entity);
        ItemStack itemStack = new ItemStack(Items.WOODEN_SWORD);

        int FrameWidth=width*26/100;

        float scale=FrameWidth/16F;
        //ModItems.ITEM_CSGOBOX.get()
        GuiItemMove.renderItemInInventoryFollowsMouse(guiGraphics,this.width*37/100,this.height*12/100,this.itemRotX,this.itemRotY,itemMenu, this.entity,scale);
        int x=0;
        int y=0;

        for (int i=0;i<itemsList.size();i++){
            int py=55;
            int px=i;
            if(i>9){
                py=73;
                px=i-10;
            }
            ItemStack itemStack1=itemsList.get(i);
            int grade=gradeList.get(i);
            x=px;
            y=py;
            if(grade==5)break;
            IconListTools.renderItemFrame(this.entity,guiGraphics,itemStack1,this.width*4/100+px*this.width*9/100, this.height*py/100,this.width, this.height,grade );
        }
        if(gradeList.get(gradeList.size()-1)==5){

            IconListTools.renderItemFrame(this.entity,guiGraphics,ItemStack.EMPTY,this.width*4/100+x*this.width*9/100, this.height*y/100,this.width, this.height,5 );
        }

        if(itemKey!=null){
            //System.out.println(ForgeRegistries.ITEMS.getKey(itemKey.getItem()));
            IconListTools.renderGuiItem(this.entity,this.world,guiGraphics,itemKey,this.width*25F/100, this.height*93F/100, 1);
        }

        //GuiIconTool.renderItemFrame(this.entity,guiGraphics,itemStack,this.width, this.height,0xFFFF0000 ,2);
        //renderEntityInInventoryFollowsMouse(guiGraphics, i + 51, j + 75, 30, (float)(i + 51) - this.xMouse, (float)(j + 75 - 50) - this.yMouse, this.minecraft.player);
    }



    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            //System.out.println("width:"+this.width+"height"+this.height);
            this.minecraft.player.closeContainer();
            BlurHandler.updateShader(true);
            this.minecraft.options.hideGui=false;
            return true;
        }
        return super.keyPressed(key, b, c);
    }



    //@Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

        Style style=Style.EMPTY.withBold(true);
        int x=0;
        int y=0;

        for (int i=0;i<itemsList.size();i++){
            int py=67;
            int px=i;
            if(i>9){
                py=85;
                px=i-10;
            }
            ItemStack itemStack1=itemsList.get(i);
            int grade=gradeList.get(i);
            x=px;
            y=py;
            if(grade>4)break;
            Component component=itemStack1.getItem().getName(itemStack1);
            FormattedCharSequence pText=component.getVisualOrderText();
            renderText(guiGraphics,pText,this.width*4F/100+px*this.width*9F/100,this.height*py/100F,0.6F);
        }
        renderText(guiGraphics,Component.translatable("gui.csgobox.csgo_box.label_gold").getVisualOrderText(),this.width*4/100F+x*this.width*9/100F,this.height*y/100F,0.6F);
        //guiGraphics.drawWordWrap(this.font,formattedText,-100,66,100,-12829636);
        renderText(guiGraphics,Component.translatable("gui.csgobox.csgo_box.label_box").getVisualOrderText(),this.width*46F/100F,this.height*13F/100F,0.8F);
        renderText(guiGraphics,itemMenu.getItem().getName(itemMenu).getVisualOrderText(),this.width*50F/100F,this.height*13F/100F,0.8F);
        //component.getStyle().applyTo(style);
        //FormattedCharSequence pText=component.getVisualOrderText();

        if(itemKey!=null&&!itemKey.isEmpty()){
            if(boxKeyCount>0){
                String count=" × "+boxKeyCount;
                renderText(guiGraphics,count,this.width*28F/100F,this.height*94F/100F,0.8F);
            }else {
                renderText(guiGraphics,Component.translatable("gui.csgobox.csgo_box.label_open").getVisualOrderText(),this.width*28F/100F,this.height*94F/100F,0.8F);
                renderText(guiGraphics,Component.translatable("gui.csgobox.csgo_box.label_open_1").getVisualOrderText(),this.width*40F/100F,this.height*94F/100F,0.8F);
                renderText(guiGraphics,itemKey.getItem().getName(itemKey).getVisualOrderText(),this.width*35F/100F,this.height*94F/100F,0.8F);

            }

        }


        renderText(guiGraphics,Component.translatable("gui.csgobox.csgo_box.label_items").withStyle(style).getVisualOrderText(),this.width*3F/100F,this.height*50.3F/100F,0.8F);

        renderText(guiGraphics,Component.translatable("gui.csgobox.csgo_box.title").withStyle(style).getVisualOrderText(),this.width*47.6F/100F,this.height*5.9F/100F,2F);

        renderText(guiGraphics,Component.translatable("gui.csgobox.csgo_box.open_box").withStyle(style).getVisualOrderText(),(float)this.width*67.5F/100F,(float) this.height*95/100,0.8F);
        renderText(guiGraphics,Component.translatable("gui.csgobox.csgo_box.back_box").withStyle(style).getVisualOrderText(),(float)this.width*72.5F/100F,(float)this.height*95/100,0.8F);



    }
    public int boxKeyCount;
    public int isBoxKey(){
        int i=0;
        for (ItemStack stack : entity.getInventory().items) {

            if (Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).toString().equals(ItemCsgoBox.getKey(itemMenu)) ) {
                i=stack.getCount();
                return i;
            }
        }
        return i;
    }


    @Override
    public final void tick() {
        super.tick();

        if(this.minecraft==null){
            return;
        }
        if(this.minecraft.player==null){
            return;
        }
        if (this.minecraft.player.isAlive() && !this.minecraft.player.isRemoved()) {
            this.containerTick();
        } else {
            this.minecraft.player.closeContainer();
        }

    }


    //@Override
    public void containerTick() {
        //super.containerTick();
        gameTick++;
        if(gameTick==1){
            if(isBoxKey()>0){
                boxKeyCount=isBoxKey();
            }

        }
        if(gameTick%50==0){
            if(isBoxKey()>0){
                boxKeyCount=isBoxKey();
            }

        }
        if(gameTick>100000){
            gameTick=0;
        }
        // 限制已经过去的刻数在 0 到 100 之间（5秒 * 20 TPS = 100刻）
        gameTick = Mth.clamp(gameTick, 0, 100);

        // 计算平移的比例
        float progress = (float) gameTick / 100;

        // 计算当前 X 坐标
        currentX = (int) Mth.lerp(progress, startX, targetX);

    }
    private void renderText(GuiGraphics guiGraphics, FormattedCharSequence pText,float px, float py, float scale){
        RenderFontTool.drawString(guiGraphics,this.font,  pText,px,py,this.leftPos,this.topPos,scale,0xD3D3D3);
    }
    private void renderText(GuiGraphics guiGraphics, String pText,float px, float py, float scale){
        RenderFontTool.drawString(guiGraphics,this.font,  pText,px,py,this.leftPos,this.topPos,scale,0xD3D3D3);
    }



    @Override
    public void onClose() {
        super.onClose();

    }

    @Override
    public void init() {
        super.init();
        imagebutton_open_box = new ImageButton(this.width*67/100, this.height*94/100, this.width*4/100, this.height*5/100, 0, 0, 64, new ResourceLocation("csgobox:textures/screens/atlas/open_box.png"), 82, 128, e -> {

            //ItemCsgoBox box=(ItemCsgoBox) itemMenu.getItem();
            ;

            if(ItemCsgoBox.getKey(itemMenu)!=null&&entity.getMainHandItem().getItem() instanceof ItemCsgoBox){

                for (ItemStack stack : entity.getInventory().items) {
                    if (Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).toString().equals(ItemCsgoBox.getKey(itemMenu)) ) {
                        //stack.shrink(1);
                        Minecraft.getInstance().setScreen(new CsboxProgressScreen());
                        Networking.INSTANCE.sendToServer(new PacketCsgoProgress( 2,ItemCsgoBox.getKey(itemMenu)));
                    }
                }
                //Minecraft.getInstance().setScreen(new CsboxProgressScreen());

            }else {
                //Minecraft.getInstance().setScreen(new CsboxProgressScreen());
                //System.out.println("没有钥匙");
                //Networking.INSTANCE.sendToServer(new PacketCsgoProgress( 1));
            }

        });
        //guistate.put("button:imagebutton_open_box", imagebutton_open_box);
        this.addRenderableWidget(imagebutton_open_box);
        imagebutton_back_box = new ImageButton(this.width*72/100, this.height*94/100, this.width*4/100, this.height*5/100, 0, 0, 64, new ResourceLocation("csgobox:textures/screens/atlas/back_box.png"), 82, 128, e -> {
            assert this.minecraft != null;
            assert this.minecraft.player != null;
            this.minecraft.player.closeContainer();
            BlurHandler.updateShader(true);
            this.minecraft.options.hideGui=false;
        });
        //guistate.put("button:imagebutton_back_box", imagebutton_back_box);
        this.addRenderableWidget(imagebutton_back_box);


    }


}

package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reclizer.csgobox.capability.ModCapability;
import com.reclizer.csgobox.capability.csbox.ICsboxCap;
import com.reclizer.csgobox.sounds.ModSounds;

import com.reclizer.csgobox.item.ItemCsgoBox;
import com.reclizer.csgobox.packet.Networking;
import com.reclizer.csgobox.packet.PacketGiveItem;
import com.reclizer.csgobox.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class CsboxProgressScreen extends Screen {


    private final Level world;

    private final Player entity;;
    public Map<ItemStack, Integer> itemList;
    private List<ItemStack> itemInput=new ArrayList<>();
    private List<Integer> gradeInput=new ArrayList<>();

    //private ItemStack giveStack=new ItemStack(ModItems.ITEM_CSGOBOX.get());
    //private int grade=5;
    public CsboxProgressScreen() {
        super(Component.literal("cs_progress"));


        if(Minecraft.getInstance().player!=null){
            this.entity= Minecraft.getInstance().player;
            this.minecraft=Minecraft.getInstance();

            this.world =entity.level();

            ItemStack boxStack=Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemCsgoBox box=(ItemCsgoBox)boxStack.getItem();
            this.boxStack=boxStack;

            this.itemList=box.getItemGroup(boxStack);

            this.velocityExport=renderCount();
        }else {
            this.entity= null;
            this.world =null;
        }




    }
    public ItemStack boxStack;

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //============================================================================================
    public void renderGradeItems(){
        for(int i=0;i<50;i++){
            Random random1=new Random();
            int grade= RandomItem.randonmItemsGrade(random1,ItemCsgoBox.getRandom(boxStack),this.entity);
            ItemStack itemStack= RandomItem.randomItems(random1,grade,this.itemList);
            gradeInput.add(grade);
            itemInput.add(itemStack);
        }
    }


    //==============================================================================================================================
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics,partialTicks,mouseX,mouseY);
        //this.renderTooltip(guiGraphics, mouseX, mouseY);
    }




    public float lastRenderWidth = 0F;





    //@Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        //guiGraphics.blit(new ResourceLocation("CsgoBox:textures/screens/csgo_background.png"), 0, 0, 0, 0, this.width, this.height, this.width, this.height);



        BlurHandler.updateShader(false);
        if(this.minecraft==null){
            return;
        }
        this.minecraft.options.hideGui=true;

        if(openTime<5){
            return;
        }
        //运动7s


        float widthNewAdd= RenderWidthAdd;

        if(this.width!=startWidth){
            widthNewAdd*=this.width/startWidth;
        }
        //速度取值1-35,35时插帧为0,1时插帧到峰值
        float progress= Math.min(1,partialTicks+velocityLerp) ;

        //Math.lerp(startValue, endValue, t);
        //停在45
        for(int i=0;i<50;i++){
            int grade=gradeInput.get(i);
            ItemStack itemStack=itemInput.get(i);

            //int color=ColorTools.colorItems(grade);
            IconListTools.renderItemProgress(this.entity,guiGraphics,itemStack,this.width*randomWidth/100F+i*this.width*20F/100F- Mth.lerp(progress,lastRenderWidth,widthNewAdd), this.height*37F/100F,this.width, this.height,grade );
        }

        lastRenderWidth=widthNewAdd;

        //黄色扫描线
        int goldLineTo=this.height*37/100+height*25/100;
        //guiGraphics.fill(width/2,this.height*37/100,width/2+2,goldLineTo+2, ColorTools.argbColor(128,255,215,0));
        IconListTools.fill(guiGraphics,this.width/2F,this.height*37/100F,width/2F+2,goldLineTo+2, ColorTools.argbColor(128,255,215,0));
        RenderSystem.disableBlend();

        RenderSystem.enableBlend();
        IconListTools.blit(guiGraphics,new ResourceLocation("csgobox:textures/screens/csgo_background.png"), 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        RenderSystem.disableBlend();


    }

    public boolean startSwitch=true;



    public float velocityLerp=0;
    private float soundWidthAdd =0;
    private  boolean soundSwitch=true;
    private static final Random random = new Random();
    //;
    //76  93.5
    //random.nextFloat(93.5F,111F);
    private float randomWidth=random.nextFloat(93.5F,111F);;



    public int startTime = 0;
    public float startWidth=this.width;
    public float gameTick=0;
    public float RenderWidthAdd = 0F;

    public boolean ifSwitch=true;

    List  <Float> renderExport;
    List   <Float> velocityExport;
    public  List <Float>  renderCount(){
        List <Float> renderMove=new ArrayList<>();
        //float RenderWidthAdd=0;
        for(int i=0;i<150;i++){
            int startTime=i;
            float time=startTime/20F;
            float velocity=(1.6F*time+0.8F)/((float) Math.pow(2, 1.5*time-5.2));
            if(velocity<0.3){
                velocity=0;
            }
            renderMove.add(velocity);
        }
        return renderMove;

    }
    public  List <Float>  renderMove(List<Float> list){
        List <Float> renderMove=new ArrayList<>();
        float RenderWidthAdd=0;
        for(int i=0;i<list.size();i++){
            float velocity=list.get(i);
            RenderWidthAdd +=startWidth/173F*velocity;
            renderMove.add(RenderWidthAdd);
        }
        return renderMove;
    }

    public int openTime;
    //@Override

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
    public void containerTick() {
        //super.containerTick();
        openTime++;
        if(openTime==2){
            renderGradeItems();
            //System.out.println(itemInput);
            //System.out.println(gradeInput);
        }
        if(openTime<2){
            return;
        }
        if(startSwitch){
            //this.entity.getInventory().add(giveStack);
            startWidth=this.width;
            startSwitch=false;
            this.renderExport=renderMove(this.velocityExport);


            Networking.INSTANCE.sendToServer(new PacketGiveItem(ItemNBT.getStacksData(itemInput.get(45))));
            ICsboxCap iCsboxCap= ModCapability.getSeed(this.entity).orElse(null);
            if(iCsboxCap==null){return;}
            iCsboxCap.setItem(ItemNBT.getStacksData(itemInput.get(45)));
            iCsboxCap.setGrade(gradeInput.get(45));
            //System.out.println(iCsboxCap.getItem());
        }
        if(openTime<5){
            return;
        }

        if(startTime<145){
            startTime++;
        }

        if(startTime==145){
            //Networking.INSTANCE.sendToServer(new PacketLookItem(1));
            Minecraft.getInstance().setScreen(new  CsLookItemScreen());
        }
        float velocity=velocityExport.get(startTime);

        velocityLerp=velocity/35;


        RenderWidthAdd=renderExport.get(startTime);

        if(ifSwitch){
            ifSwitch=false;
        }

        //====================================================================================================================
        //声音
        if(RenderWidthAdd>startWidth*randomWidth/100F-startWidth/2&&RenderWidthAdd<startWidth*randomWidth/100F-startWidth/2+startWidth*20F*35/100F){
            //System.out.println((int)soundWidthAdd%(int)(startWidth*20F/100F));
            soundWidthAdd +=  (startWidth/173F*velocity);
            if(soundWidthAdd>(startWidth*20F/100F)){
                soundWidthAdd =0;
                this.world.playSound(this.entity, this.entity.getX(), this.entity.getY(), this.entity.getZ(), ModSounds.CS_DITA.get(), SoundSource.NEUTRAL, 10F, 1F);

            }
        }else if(RenderWidthAdd>=startWidth*randomWidth/100F-startWidth/2+startWidth*20F*35/100F){
            if(soundSwitch){
                soundWidthAdd =0;
                soundSwitch=false;
            }

            soundWidthAdd +=  (startWidth/173F*velocity);
            if(soundWidthAdd>(startWidth*20F/100F)){
                soundWidthAdd =0;
                this.world.playSound(this.entity, this.entity.getX(), this.entity.getY(), this.entity.getZ(), ModSounds.CS_DITA.get(), SoundSource.NEUTRAL, 10F, 1F);

            }
        }

        //声音处理


        //====================================================================================


    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            entity.closeContainer();
            BlurHandler.updateShader(true);
            if(this.minecraft!=null){
                this.minecraft.options.hideGui=false;
            }

            return true;
        }
        return super.keyPressed(key, b, c);
    }




    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public void init() {

        super.init();
    }
}

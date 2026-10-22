package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.block.*;
import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.packet.*;
import com.reclizer.csgobox.server.*;
import com.reclizer.csgobox.utils.ItemNBT;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.nio.file.Files;
import java.util.*;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class ConsumptionEditorGameTests {
    private static final String KEY="minecraft:tripwire_hook.withTags{sampled:1b}";
    private static FakePlayer player(GameTestHelper helper, boolean operator) {
        return new FakePlayer(helper.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"[ConsumptionTest]")) {
            @Override public boolean hasPermissions(int level) { return operator || level<=0; }
        };
    }
    private static BoxDefinition box(ConsumptionMode mode) {
        return new BoxDefinition(BoxDefinition.newId(),"Consumption test",KEY,0,List.of(0,0,0,0,1),List.of(),
                List.of("minecraft:diamond.withCount(3)"),null,null,null,null).withConsumption(mode.id());
    }
    private static ItemStack keys(int count) { var key=ItemNBT.parseStrict(KEY); key.setCount(count); return key; }
    private static void held(FakePlayer player, BoxDefinition box) {
        player.getInventory().selected=0;
        player.getInventory().setItem(0,ItemCsgoBox.setBoxInfo(box,new ItemStack(ModItems.ITEM_CSGOBOX.get(),2)));
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void withConfig(GameTestHelper helper, Checked test) throws Exception {
        CsgoBoxManage.loadConfigBox();
        var path=net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("csbox/box.json");
        byte[] before=Files.readAllBytes(path);
        try { test.run(); }
        finally {
            Files.write(path,before); CsgoBoxManage.loadConfigBox();
            ConfiguredAcquisition.refresh(helper.getLevel().getServer()); BoxService.clearSessions();
        }
    }

    @GameTest(template="empty")
    public static void inventoryModesConsumeExactlyTheirConfiguredItems(GameTestHelper helper) {
        for (var mode:ConsumptionMode.values()) {
            var box=box(mode); var player=player(helper,true); held(player,box);
            if (mode.requiresKey()) player.getInventory().setItem(1,keys(3));
            var result=BoxOpening.open(player,box,new Random(0));
            helper.assertTrue(result.success(),"Mode did not open: "+mode);
            helper.assertTrue(player.getMainHandItem().getCount()==(mode.consumesBox()?1:2),"Wrong box cost: "+mode);
            helper.assertTrue(player.getInventory().countItem(Items.TRIPWIRE_HOOK)==(mode.requiresKey()?2:0),"Wrong required-item cost: "+mode);
            helper.assertTrue(player.getInventory().countItem(Items.DIAMOND)==3,"Reward count changed: "+mode);
        }
        var player=player(helper,true); var box=box(ConsumptionMode.BOX); held(player,box); player.getInventory().setItem(1,keys(3));
        helper.assertTrue(BoxOpening.open(player,box,new Random()).success() && player.getInventory().getItem(1).getCount()==3,
                "Box-only mode consumed an optional configured key");
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void reusableInventoryBoxRequiresAnExactKeyOnEveryOpening(GameTestHelper helper) {
        var box=box(ConsumptionMode.KEY); var player=player(helper,true); held(player,box);
        player.getInventory().setItem(1,new ItemStack(Items.TRIPWIRE_HOOK,3));
        helper.assertTrue(!BoxOpening.open(player,box,new Random()).success(),"Key-only accepted wrong NBT");
        player.getInventory().setItem(1,keys(2));
        for (int i=0;i<2;i++) helper.assertTrue(BoxOpening.open(player,box,new Random()).success(),"Reusable box stopped early");
        helper.assertTrue(!BoxOpening.open(player,box,new Random()).success(),"Reusable box opened with no key");
        helper.assertTrue(player.getMainHandItem().getCount()==2 && player.getInventory().countItem(Items.DIAMOND)==6,
                "Reusable inventory box/cost changed");
        player.getInventory().setItem(0,ItemStack.EMPTY); player.getInventory().setItem(1,keys(1));
        helper.assertTrue(!BoxOpening.open(player,box,new Random()).success() && player.getInventory().getItem(1).getCount()==1,
                "Key alone opened without a case");
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void groundModesChargeRequiredKeysAndLeaveOnlyDecorativeCases(GameTestHelper helper) throws Exception {
        withConfig(helper,()-> {
            var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2));
            for (var mode:ConsumptionMode.values()) {
                var box=box(mode); CsgoBoxManage.saveDefinition(box,"");
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(pos,ModBlocks.BOX.get().defaultBlockState());
                var entity=(PlacedBoxEntity)level.getBlockEntity(pos); entity.setPoolId(box.id());
                var player=player(helper,true); player.setPos(Vec3.atCenterOf(pos).add(0,0,2));
                player.setItemInHand(InteractionHand.MAIN_HAND,keys(2));
                int before=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2))
                        .stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
                helper.assertTrue(PlacedBoxOpening.open(player,pos,InteractionHand.MAIN_HAND,new Random()).success(),"Ground mode failed: "+mode);
                helper.assertTrue(entity.isOpening() && player.getInventory().countItem(Items.DIAMOND)==0,"Ground prize was not delayed");
                int remaining=mode.requiresKey()?1:2;
                helper.assertTrue(player.getMainHandItem().getCount()==remaining,"Wrong ground key cost: "+mode);
                helper.assertTrue(!PlacedBoxOpening.open(player,pos,InteractionHand.MAIN_HAND,new Random()).success(),"Opening was replayed");
                PlacedBoxGameTests.complete(helper,entity);
                helper.assertTrue(entity.isEmpty() && level.getBlockState(pos).is(ModBlocks.BOX.get()),"Ground case did not become decorative: "+mode);
                PlacedBoxOpening.open(player,pos,InteractionHand.MAIN_HAND,new Random());
                helper.assertTrue(player.getMainHandItem().getCount()==remaining,"Decorative case consumed another key");
                int after=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2))
                        .stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
                helper.assertTrue(after-before==3,"Ground case lost or duplicated the delayed reward");
            }
        }); helper.succeed();
    }

    @GameTest(template="empty")
    public static void previewAndDeleteCodecsCarryConsumptionAndVersion(GameTestHelper helper) {
        for (var mode:ConsumptionMode.values()) {
            var definition=box(mode); var preview=BoxService.previewData(UUID.randomUUID(),definition);
            var buffer=new FriendlyByteBuf(Unpooled.buffer());
            try {
                preview.toBytes(buffer); var decoded=new PacketBoxPreview(buffer);
                helper.assertTrue(decoded.consumption().equals(mode.id()) && decoded.key().isEmpty()!=mode.requiresKey()
                        && buffer.readableBytes()==0,"Preview lost consumption or wrong required item");
            } finally { buffer.release(); }
        }
        var request=new PacketDeleteBox(7,9,"csgobox:delete_test","a".repeat(64));
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try { request.toBytes(buffer); helper.assertTrue(request.equals(new PacketDeleteBox(buffer)),"Delete codec lost version/identity"); }
        finally { buffer.release(); }
        boolean rejected=false;
        try { new PacketBoxPreview(UUID.randomUUID(),new ItemStack(Items.CHEST),ItemStack.EMPTY,List.of(),List.of(),"key"); }
        catch (IllegalArgumentException expected) { rejected=true; }
        helper.assertTrue(rejected,"Key consumption accepted a missing required item");
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void deletionChecksAuthorityVersionReplayAndRemovesRecipes(GameTestHelper helper) throws Exception {
        withConfig(helper,()-> {
            var base=box(ConsumptionMode.KEY);
            var grid=new ArrayList<>(Collections.nCopies(9,"")); grid.set(4,"minecraft:stone");
            var rule=new AcquisitionDefinition(new AcquisitionDefinition.Crafting(true,grid,1),null,null);
            var box=new BoxDefinition(base.id(),base.name(),base.key(),0,base.random(),List.of(),base.grade1(),null,null,null,null,
                    true,null,rule,null,"green","key");
            CsgoBoxManage.saveDefinition(box,""); ConfiguredAcquisition.refresh(helper.getLevel().getServer());
            var recipeId=ConfiguredAcquisition.recipeId(box,false);
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(recipeId).isPresent(),"Delete fixture missing recipe");
            String revision=BoxConfigStore.revision(box);
            var denied=player(helper,false); denied.containerMenu=new CsgoBoxCraftMenu(7,denied.getInventory(),null);
            helper.assertTrue(!EditorDeleteBox.delete(denied,7,1,box.id(),revision).isEmpty(),"Non-op deleted a pool");
            var operator=player(helper,true);
            helper.assertTrue(!EditorDeleteBox.delete(operator,7,1,box.id(),revision).isEmpty(),"Closed editor accepted delete");
            operator.containerMenu=new CsgoBoxCraftMenu(7,operator.getInventory(),null);
            helper.assertTrue(!EditorDeleteBox.delete(operator,8,1,box.id(),revision).isEmpty(),"Wrong editor accepted delete");
            boolean stale=false;
            try { EditorDeleteBox.delete(operator,7,1,box.id(),"0".repeat(64)); }
            catch (IllegalArgumentException expected) { stale=true; }
            helper.assertTrue(stale && CsgoBoxManage.find(box.id())!=null,"Stale delete changed a pool");
            helper.assertTrue(EditorDeleteBox.delete(operator,7,2,box.id(),revision).isEmpty(),"Valid delete failed");
            helper.assertTrue(CsgoBoxManage.find(box.id())==null && CsgoBoxManage.readForEditing(box.id())==null
                    && helper.getLevel().getRecipeManager().byKey(recipeId).isEmpty(),"Deleted pool/recipe remained active");
            CsgoBoxManage.saveDefinition(box,"");
            helper.assertTrue(!EditorDeleteBox.delete(operator,7,2,box.id(),revision).isEmpty() && CsgoBoxManage.find(box.id())!=null,
                    "Replayed delete removed a newly recreated pool");
        }); helper.succeed();
    }
}

package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.block.*;
import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.server.PlacedBoxOpening;
import com.reclizer.csgobox.utils.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.nio.file.Files;
import java.util.*;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class GroundModeGameTests {
    private static final String KEY="minecraft:tripwire_hook.withTags{groundModeKey:1b,display:{Name:'{\"text\":\"Reusable test key\"}'}}";
    private static BlockPos pos(GameTestHelper h) { return h.absolutePos(new BlockPos(2,1,2)); }
    private static BoxDefinition definition(ConsumptionMode mode) {
        return new BoxDefinition(BoxDefinition.newId(),"Ground mode test",KEY,0,List.of(0,0,0,0,1),List.of(),
                List.of("minecraft:diamond.withTags{groundModeReward:1b}.withCount(3)"),null,null,null,null).withConsumption(mode.id());
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    @FunctionalInterface private interface Checked { void run(BoxDefinition box,PlacedBoxEntity entity,FakePlayer player) throws Exception; }
    private static void withDefinition(BoxDefinition box,Action action) throws Exception {
        CsgoBoxManage.loadConfigBox();
        var file=net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("csbox/box.json"); var before=Files.readAllBytes(file);
        try { CsgoBoxManage.saveDefinition(box,""); action.run(); }
        finally { Files.write(file,before); CsgoBoxManage.loadConfigBox(); }
    }
    private static void setup(GameTestHelper h,BoxDefinition box,Checked action) throws Exception {
        withDefinition(box,()-> {
            h.getLevel().setBlockAndUpdate(pos(h).below(),Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos(h),ModBlocks.BOX.get().defaultBlockState());
            var entity=(PlacedBoxEntity)h.getLevel().getBlockEntity(pos(h)); entity.setPoolId(box.id());
            var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"[GroundMode]"));
            p.setPos(Vec3.atCenterOf(pos(h)).add(0,0,2)); p.getAbilities().mayBuild=true;
            var keys=ItemNBT.parseStrict(KEY); keys.setCount(2); p.setItemInHand(InteractionHand.MAIN_HAND,keys);
            action.run(box,entity,p);
        });
    }
    private static List<ItemStack> drops(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos(h)).inflate(2)).stream().map(ItemEntity::getItem).toList();
    }
    private static int prizes(GameTestHelper h) {
        return drops(h).stream().filter(s->s.is(Items.DIAMOND) && s.hasTag() && s.getTag().getBoolean("groundModeReward")).mapToInt(ItemStack::getCount).sum();
    }
    private static int shells(GameTestHelper h) { return drops(h).stream().filter(s->s.is(ModItems.ITEM_EMPTY_BOX.get())).mapToInt(ItemStack::getCount).sum(); }
    private static boolean open(GameTestHelper h,FakePlayer player) { return PlacedBoxOpening.open(player,pos(h),InteractionHand.MAIN_HAND,new Random(9)).success(); }
    private static void resetNow(GameTestHelper h,PlacedBoxEntity entity) {
        var tag=entity.saveWithoutMetadata(); tag.putLong("RewardDroppedAt",h.getLevel().getGameTime()-GroundAnimation.REUSE_DELAY_TICKS); entity.load(tag);
        PlacedBoxEntity.serverTick(h.getLevel(),pos(h),entity.getBlockState(),entity);
    }

    @GameTest(template="empty",timeoutTicks=120)
    public static void keyOnlyWaitsTwoSecondsClosesAndCanOpenAgain(GameTestHelper h) throws Exception {
        setup(h,definition(ConsumptionMode.KEY),(box,entity,p)-> {
            h.assertTrue(open(h,p),"First key-only opening failed"); PlacedBoxGameTests.complete(h,entity);
            h.assertTrue(prizes(h)==3 && entity.isBusy() && !entity.isOpening() && !entity.isEmpty(),"Key-only reward did not start recovery");
            h.runAfterDelay(39,()->h.assertTrue(entity.isBusy() && entity.lidOpen() && prizes(h)==3,"Key-only case reset before two seconds"));
            h.runAfterDelay(41,()->h.assertTrue(!entity.isBusy() && !entity.isEmpty() && !entity.lidOpen(),"Key-only case did not start closing/reset at two seconds"));
            h.runAfterDelay(58,()-> {
                h.assertTrue(entity.lidProgress(0)==0 && ItemCsgoBox.boxId(entity.asItem()).equals(box.id()),"Automatic close lost case identity or did not finish");
                try {
                    withDefinition(box,()-> {
                        h.assertTrue(open(h,p) && p.getMainHandItem().isEmpty(),"Second opening did not consume exactly one more key");
                        PlacedBoxGameTests.complete(h,entity); h.assertTrue(prizes(h)==6,"Reusable case did not deliver the second prize exactly once");
                        resetNow(h,entity); h.assertTrue(!open(h,p) && prizes(h)==6,"Empty key stack reopened the reusable case");
                        h.assertTrue(p.gameMode.destroyBlock(pos(h)) && shells(h)==0 && drops(h).stream().anyMatch(s->ItemCsgoBox.boxId(s).equals(box.id())),"Ready reusable case did not drop its usable identity");
                    }); h.succeed();
                } catch (Exception error) { throw new RuntimeException(error); }
            });
        });
    }
    @GameTest(template="empty")
    public static void keyOnlyModeAndRecoverySurviveReloadAndPoolModeChanges(GameTestHelper h) throws Exception {
        setup(h,definition(ConsumptionMode.KEY),(box,entity,p)-> {
            h.assertTrue(open(h,p),"Key-only opening failed"); var saved=entity.saveWithFullMetadata();
            CsgoBoxManage.saveDefinition(box.withConsumption("both"),BoxConfigStore.revision(box));
            var loaded=(PlacedBoxEntity)BlockEntity.loadStatic(pos(h),entity.getBlockState(),saved); h.getLevel().setBlockEntity(loaded);
            PlacedBoxGameTests.complete(h,loaded);
            h.assertTrue(loaded.isBusy() && !loaded.isEmpty() && prizes(h)==3,"Config change/reload replaced the opening mode snapshot");
            var recovering=loaded.saveWithFullMetadata(); h.assertTrue(!recovering.contains("PendingReward"),"Paid reward remained pending during recovery");
            var again=(PlacedBoxEntity)BlockEntity.loadStatic(pos(h),loaded.getBlockState(),recovering); h.getLevel().setBlockEntity(again);
            var observer=new PlacedBoxEntity(pos(h),again.getBlockState()); observer.setLevel(h.getLevel()); observer.load(again.getUpdateTag());
            h.assertTrue(observer.isBusy() && !observer.isOpening() && observer.lidOpen(),"Recovery update tag lost client-visible phase");
            resetNow(h,again); h.assertTrue(!again.isBusy() && prizes(h)==3,"Recovery reload duplicated prize or did not reset");
            h.assertTrue(open(h,p),"Reset case could not open under updated pool"); PlacedBoxGameTests.complete(h,again);
            h.assertTrue(again.isEmpty() && prizes(h)==6,"Next opening did not use the new both-consumed mode");
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void boxOnlyRequiresMatchingHeldKeyWithoutConsumingIt(GameTestHelper h) throws Exception {
        setup(h,definition(ConsumptionMode.BOX),(box,entity,p)-> {
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.TRIPWIRE_HOOK));
            h.assertTrue(!open(h,p) && !entity.isBusy() && prizes(h)==0,"Box-only case accepted a mismatched configured key");
            var keys=ItemNBT.parseStrict(KEY); keys.setCount(2); p.setItemInHand(InteractionHand.OFF_HAND,keys);
            h.assertTrue(open(h,p) && p.getOffhandItem().getCount()==2 && p.getMainHandItem().getCount()==1,"Box-only opening consumed a key or rejected offhand");
            PlacedBoxGameTests.complete(h,entity); h.assertTrue(entity.isEmpty() && prizes(h)==3,"Box-only case did not become empty after one reward");
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void boxOnlyStillAllowsAnEmptyKeyConfiguration(GameTestHelper h) throws Exception {
        var base=definition(ConsumptionMode.BOX);
        var box=new BoxDefinition(base.id(),base.name(),"",0,base.random(),List.of(),base.grade1(),null,null,null,null);
        setup(h,box,(definition,entity,p)-> {
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            h.assertTrue(open(h,p),"No-key configuration stopped working"); PlacedBoxGameTests.complete(h,entity);
            h.assertTrue(entity.isEmpty() && prizes(h)==3,"No-key case did not complete normally");
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void everyModeDropsOnlyEmptyShellAndFixedPrizeWhenMinedDuringReel(GameTestHelper h) throws Exception {
        int completed=0;
        for (var mode:ConsumptionMode.values()) {
            final int before=completed;
            setup(h,definition(mode),(box,entity,p)-> {
                h.assertTrue(open(h,p),"Ground mode could not start: "+mode);
                var saved=entity.saveWithoutMetadata(); saved.putLong("OpeningStarted",h.getLevel().getGameTime()-GroundAnimation.LID_TICKS-40); entity.load(saved);
                h.assertTrue(p.gameMode.destroyBlock(pos(h)),"Mode still blocked mining: "+mode);
                entity.onRemoved(); PlacedBoxEntity.serverTick(h.getLevel(),pos(h),entity.getBlockState(),entity);
                h.assertTrue(prizes(h)==(before+1)*3 && shells(h)==before+1 && drops(h).stream().noneMatch(s->s.is(ModItems.ITEM_CSGOBOX.get())),"Broken mode lost/duplicated payout or returned a reusable case: "+mode);
                h.assertTrue(p.getMainHandItem().getCount()==(mode.consumesKey()?1:2),"Broken mode changed the key cost: "+mode);
            }); completed++;
        } h.succeed();
    }
    @GameTest(template="empty")
    public static void breakingDuringRecoveryDoesNotDropTheRewardTwice(GameTestHelper h) throws Exception {
        setup(h,definition(ConsumptionMode.KEY),(box,entity,p)-> {
            h.assertTrue(open(h,p),"Key-only opening failed"); PlacedBoxGameTests.complete(h,entity);
            h.assertTrue(entity.isBusy() && !entity.isOpening() && p.gameMode.destroyBlock(pos(h)),"Recovery case could not be mined");
            h.assertTrue(prizes(h)==3 && shells(h)==1 && drops(h).stream().noneMatch(s->s.is(ModItems.ITEM_CSGOBOX.get())),"Recovery break duplicated prize or refunded a usable case");
        }); h.succeed();
    }
}

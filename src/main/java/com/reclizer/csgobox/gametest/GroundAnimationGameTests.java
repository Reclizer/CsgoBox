package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.block.*;
import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.server.*;
import com.reclizer.csgobox.utils.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class GroundAnimationGameTests {
    private static BlockPos pos(GameTestHelper h) { return h.absolutePos(new BlockPos(2,1,2)); }
    private static FakePlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"[GroundAnimation]"));
        p.setPos(Vec3.atCenterOf(pos(h)).add(0,0,2)); p.getAbilities().mayBuild=true; return p;
    }
    private static BoxDefinition definition() {
        return new BoxDefinition(BoxDefinition.newId(),"Animated test","minecraft:tripwire_hook.withTags{display:{Name:'{\"text\":\"Named test key\"}'},key:1b}",
                0,List.of(0,0,0,0,1),List.of(),List.of("minecraft:diamond.withTags{pendingPrize:1b}.withCount(3)"),null,null,null,null);
    }
    @FunctionalInterface private interface Checked { void run(BoxDefinition box,PlacedBoxEntity entity,FakePlayer player) throws Exception; }
    private static void setup(GameTestHelper h,Checked test) throws Exception {
        CsgoBoxManage.loadConfigBox();
        var file=net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("csbox/box.json"); var before=java.nio.file.Files.readAllBytes(file);
        try {
            var box=definition(); CsgoBoxManage.saveDefinition(box,"");
            h.getLevel().setBlockAndUpdate(pos(h).below(),Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos(h),ModBlocks.BOX.get().defaultBlockState());
            var entity=(PlacedBoxEntity)h.getLevel().getBlockEntity(pos(h)); entity.setPoolId(box.id());
            var p=player(h); var keys=ItemNBT.parseStrict(box.key()); keys.setCount(2); p.setItemInHand(InteractionHand.MAIN_HAND,keys);
            test.run(box,entity,p);
        } finally { java.nio.file.Files.write(file,before); CsgoBoxManage.loadConfigBox(); }
    }
    private static int prizes(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos(h)).inflate(2)).stream()
                .filter(e->e.getItem().is(Items.DIAMOND) && e.getItem().hasTag() && e.getItem().getTag().getBoolean("pendingPrize"))
                .mapToInt(e->e.getItem().getCount()).sum();
    }
    private static void start(GameTestHelper h,FakePlayer p) {
        h.assertTrue(PlacedBoxOpening.open(p,pos(h),InteractionHand.MAIN_HAND,new Random(7)).success(),"Opening did not begin");
    }
    private static void duringReel(GameTestHelper h,PlacedBoxEntity entity) {
        var tag=entity.saveWithoutMetadata(); tag.putLong("OpeningStarted",h.getLevel().getGameTime()-GroundAnimation.LID_TICKS-40); entity.load(tag);
        h.assertTrue(entity.spinTicks(0)>0 && entity.elapsed(0)<GroundAnimation.TOTAL_TICKS,"Test did not run during the reel");
    }
    private static void settled(GameTestHelper h) {
        h.assertTrue(h.getLevel().getBlockState(pos(h)).isAir() && prizes(h)==3,"Destroyed paid case lost the fixed reward or remained placed");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos(h)).inflate(2));
        h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModItems.ITEM_CSGOBOX.get()))
                && drops.stream().filter(e->e.getItem().is(ModItems.ITEM_EMPTY_BOX.get())).mapToInt(e->e.getItem().getCount()).sum()==1,
                "Destroyed paid case did not drop exactly one empty shell");
    }
    @GameTest(template="empty",timeoutTicks=240)
    public static void actualTicksDelayTheDropUntilTheReelFinishes(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            start(h,p);
            h.assertTrue(prizes(h)==0 && p.getInventory().countItem(Items.DIAMOND)==0 && p.getMainHandItem().getCount()==1,"Start delivered a prize or charged twice");
            h.runAfterDelay(GroundAnimation.TOTAL_TICKS-2,()->h.assertTrue(prizes(h)==0 && entity.isOpening(),"Reward appeared before the reel ended"));
            h.runAfterDelay(GroundAnimation.TOTAL_TICKS+2,()-> {
                h.assertTrue(prizes(h)==3 && entity.isEmpty() && entity.lidOpen(),"Finished reel did not eject reward and retain open empty case"); h.succeed();
            });
        });
    }
    @GameTest(template="empty")
    public static void missingKeyMessageIncludesTheConfiguredCustomName(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
            var result=PlacedBoxOpening.open(p,pos(h),InteractionHand.MAIN_HAND,new Random());
            var message=PlacedBoxOpening.errorMessage(p,pos(h),result.error());
            h.assertTrue(!result.success() && message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getArgs()[0] instanceof net.minecraft.network.chat.Component c && c.getString().equals("Named test key"),"Missing-key prompt omitted specific/custom key name");
            h.assertTrue(!entity.isOpening() && prizes(h)==0,"Wrong key started or rewarded the case");
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void savedPendingResultSurvivesPoolDeletionAndLoadsOnlyOnce(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            start(h,p); var tag=entity.saveWithFullMetadata();
            h.assertTrue(tag.contains("PendingReward") && !entity.getUpdateTag().contains("Opener"),"Pending result was not durable or leaked opener metadata");
            CsgoBoxManage.deleteDefinition(box.id(),BoxConfigStore.revision(box));
            var loaded=(PlacedBoxEntity)BlockEntity.loadStatic(pos(h),entity.getBlockState(),tag);
            h.getLevel().setBlockEntity(loaded);
            PlacedBoxGameTests.complete(h,loaded);
            h.assertTrue(loaded.isEmpty() && prizes(h)==3,"Reload/deletion changed the paid result");
            PlacedBoxEntity.serverTick(h.getLevel(),pos(h),loaded.getBlockState(),loaded);
            h.assertTrue(prizes(h)==3,"Reloaded result was emitted twice");
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void canceledRewardSpawnKeepsPendingAndRetriesWithoutReroll(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            start(h,p);
            Consumer<EntityJoinLevelEvent> cancel=e->{if (e.getEntity() instanceof ItemEntity item && item.getItem().is(Items.DIAMOND)) e.setCanceled(true);};
            MinecraftForge.EVENT_BUS.addListener(cancel);
            try { PlacedBoxGameTests.complete(h,entity); } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
            h.assertTrue(entity.isOpening() && prizes(h)==0 && !entity.winner().stack().isEmpty(),"Canceled drop lost the pending prize");
            PlacedBoxEntity.serverTick(h.getLevel(),pos(h),entity.getBlockState(),entity);
            h.assertTrue(entity.isEmpty() && prizes(h)==3 && p.getMainHandItem().getCount()==1,"Retry lost/duplicated reward or charged again");
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void miningDuringReelDropsTheAlreadyFixedRewardAndEmptyShell(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            start(h,p); duringReel(h,entity);
            CsgoBoxManage.deleteDefinition(box.id(),BoxConfigStore.revision(box));
            h.assertTrue(entity.getBlockState().getDestroyProgress(p,h.getLevel(),pos(h))>0,"Client mining progress is still blocked");
            h.assertTrue(prizes(h)==0 && p.gameMode.destroyBlock(pos(h)),"Normal mining was denied during the reel");
            settled(h);
            h.assertTrue(p.getMainHandItem().getCount()==1,"Breaking the paid case consumed another key");
            entity.onRemoved(); PlacedBoxEntity.serverTick(h.getLevel(),pos(h),entity.getBlockState(),entity);
            settled(h);
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void supportLossDuringReelImmediatelySettlesThePaidCase(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            start(h,p); duringReel(h,entity);
            h.getLevel().destroyBlock(pos(h).below(),false);
            settled(h);
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void creativeBreakingDuringReelAlsoDropsRewardAndEmptyShell(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            start(h,p); duringReel(h,entity); p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            h.assertTrue(p.gameMode.destroyBlock(pos(h)),"Creative could not break the paid case");
            settled(h);
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void explosionDuringReelDropsTheFixedRewardAndEmptyShell(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            start(h,p); duringReel(h,entity);
            h.getLevel().explode(null,pos(h).getX()+.5,pos(h).getY()+.2,pos(h).getZ()+.5,1.5F,net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
            settled(h);
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void externalBreakCancellationKeepsTheRewardPending(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            start(h,p); duringReel(h,entity);
            Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> cancel=e->{if (e.getPos().equals(pos(h))) e.setCanceled(true);};
            MinecraftForge.EVENT_BUS.addListener(cancel);
            try { h.assertTrue(!p.gameMode.destroyBlock(pos(h)),"External break protection was bypassed"); }
            finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
            h.assertTrue(entity.isOpening() && prizes(h)==0 && !entity.winner().stack().isEmpty(),"Canceled breaking settled or lost the prize");
            h.assertTrue(p.gameMode.destroyBlock(pos(h)),"Case did not break after protection was removed"); settled(h);
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void forcedRemovalReturnsEmptyCaseAndSettlesPendingOnce(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            start(h,p); h.getLevel().destroyBlock(pos(h),true);
            h.assertTrue(prizes(h)==3,"Forced removal lost the paid prize");
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos(h)).inflate(2));
            h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModItems.ITEM_CSGOBOX.get()))
                    && drops.stream().filter(e->e.getItem().is(ModItems.ITEM_EMPTY_BOX.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Forced removal refunded an unopened case");
            entity.onRemoved(); h.assertTrue(prizes(h)==3,"Removal fallback duplicated reward");
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void rewardSpawnHookCannotReenterAndDuplicateThePaidResult(GameTestHelper h) throws Exception {
        setup(h,(box,entity,p)-> {
            start(h,p); var removed=new java.util.concurrent.atomic.AtomicBoolean();
            Consumer<EntityJoinLevelEvent> remove=e->{
                if (e.getEntity() instanceof ItemEntity item && item.getItem().is(Items.DIAMOND) && removed.compareAndSet(false,true))
                    h.getLevel().destroyBlock(pos(h),true);
            };
            MinecraftForge.EVENT_BUS.addListener(remove);
            try { PlacedBoxGameTests.complete(h,entity); } finally { MinecraftForge.EVENT_BUS.unregister(remove); }
            h.assertTrue(removed.get() && prizes(h)==3,"Reentrant block removal duplicated the prize");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos(h)).inflate(2)).stream()
                    .filter(e->e.getItem().is(ModItems.ITEM_EMPTY_BOX.get())).mapToInt(e->e.getItem().getCount()).sum()==1,
                    "Reentrant removal duplicated the empty shell");
        }); h.succeed();
    }
    @GameTest(template="empty")
    public static void emptyCasesPlaceNormallyKeepAppearanceAndNeverNeedKeys(GameTestHelper h) {
        var p=player(h); h.getLevel().setBlockAndUpdate(pos(h).below(),Blocks.STONE.defaultBlockState());
        for (var appearance:BoxAppearance.values()) {
            var stack=EmptyBoxItem.create(appearance); stack.setCount(2);
            stack.getOrCreateTagElement("BlockEntityTag").putString("PoolId","csgobox:forged");
            p.setItemInHand(InteractionHand.MAIN_HAND,stack); p.setShiftKeyDown(false);
            var placement=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos(h).below()),Direction.UP,pos(h).below(),false));
            h.assertTrue(stack.useOn(placement).consumesAction() && stack.getCount()==1,"Empty case could not place normally");
            var entity=(PlacedBoxEntity)h.getLevel().getBlockEntity(pos(h));
            h.assertTrue(entity.isEmpty() && entity.poolId().isEmpty() && entity.getBlockState().getValue(PlacedBoxBlock.APPEARANCE)==appearance,"Empty placement restored a loot identity or lost appearance");
            PlacedBoxOpening.open(p,pos(h),InteractionHand.MAIN_HAND,new Random()); h.assertTrue(entity.lidOpen(),"Empty case did not open freely");
            PlacedBoxOpening.open(p,pos(h),InteractionHand.MAIN_HAND,new Random()); h.assertTrue(!entity.lidOpen() && !entity.isOpening(),"Empty case started another draw");
            var recovered=entity.asItem();
            h.assertTrue(recovered.is(ModItems.ITEM_EMPTY_BOX.get()) && EmptyBoxItem.appearance(recovered)==appearance && ItemCsgoBox.boxId(recovered).isEmpty(),"Empty identity not preserved on recovery");
            var saved=entity.saveWithFullMetadata(); var loaded=(PlacedBoxEntity)BlockEntity.loadStatic(pos(h),entity.getBlockState(),saved);
            h.assertTrue(loaded.isEmpty() && loaded.poolId().isEmpty(),"Empty world save became usable");
            h.getLevel().destroyBlock(pos(h),true);
        } h.assertTrue(prizes(h)==0,"Empty case yielded a reward"); h.succeed();
    }
}

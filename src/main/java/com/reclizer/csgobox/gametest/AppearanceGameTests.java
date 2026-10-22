package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.block.*;
import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.packet.*;
import com.reclizer.csgobox.server.BoxService;
import com.reclizer.csgobox.utils.ItemNBT;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class AppearanceGameTests {
    @GameTest(template="empty")
    public static void catalogCodecUpdatesExistingIdentityAndRejectsUnknownAppearance(GameTestHelper helper) {
        var entries = Arrays.stream(BoxAppearance.values()).map(a -> new BoxCatalog.Entry("csgobox:"+a.id(), a.id(), a.id())).toList();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        var before = List.copyOf(BoxCatalog.entries());
        try {
            new PacketBoxCatalog(entries).toBytes(buffer);
            var decoded = new PacketBoxCatalog(buffer);
            helper.assertTrue(entries.equals(decoded.entries()) && buffer.readableBytes()==0, "Catalog lost appearance data");
            BoxCatalog.replace(decoded.entries());
            for (var appearance : BoxAppearance.values())
                helper.assertTrue(BoxCatalog.appearance("csgobox:"+appearance.id())==appearance, "Catalog selected wrong item model");
            var stack = ItemCsgoBox.setBoxId("csgobox:gold", new ItemStack(ModItems.ITEM_CSGOBOX.get()));
            BoxCatalog.replace(List.of(new BoxCatalog.Entry("csgobox:gold", "Changed", "green")));
            helper.assertTrue(BoxCatalog.appearance(ItemCsgoBox.boxId(stack))==BoxAppearance.GREEN && stack.getTag().size()==1,
                    "Existing stack needs duplicate appearance NBT");
            boolean rejected=false;
            try { BoxCatalog.replace(List.of(new BoxCatalog.Entry("csgobox:gold", "Bad", "unknown"))); }
            catch (IllegalArgumentException expected) { rejected=true; }
            helper.assertTrue(rejected && BoxCatalog.appearance("csgobox:gold")==BoxAppearance.GREEN, "Bad catalog replaced valid models");
            helper.assertTrue(BoxCatalog.appearance("csgobox:missing")==BoxAppearance.CLASSIC, "Unknown box has no fallback");
        } finally { buffer.release(); BoxCatalog.replace(before); }
        helper.succeed();
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void withConfig(Checked action) throws Exception {
        CsgoBoxManage.loadConfigBox();
        var path=net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("csbox/box.json");
        byte[] before=Files.readAllBytes(path);
        try { action.run(); }
        finally { Files.write(path,before); CsgoBoxManage.loadConfigBox(); }
    }
    private static BoxDefinition definition(BoxAppearance appearance) {
        return new BoxDefinition("csgobox:appearance_test", "Appearance", "", 0, List.of(0,0,0,0,1), List.of(),
                List.of("minecraft:diamond"), null,null,null,null).withAppearance(appearance.id());
    }

    @GameTest(template="empty")
    public static void allAppearancesPlaceAndPersistWithPoolOnlyIdentity(GameTestHelper helper) throws Exception {
        withConfig(() -> {
            var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2));
            var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"[AppearanceTest]"));
            player.setPos(Vec3.atCenterOf(pos).add(0,0,2)); player.setShiftKeyDown(true);
            String revision="";
            for (var appearance : BoxAppearance.values()) {
                var box=definition(appearance); CsgoBoxManage.saveDefinition(box,revision); revision=BoxConfigStore.revision(box);
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
                var stack=ItemCsgoBox.setBoxInfo(box,new ItemStack(ModItems.ITEM_CSGOBOX.get(),2));
                player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
                helper.assertTrue(stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Placement failed");
                var state=level.getBlockState(pos); var entity=(PlacedBoxEntity)level.getBlockEntity(pos);
                helper.assertTrue(state.getValue(PlacedBoxBlock.APPEARANCE)==appearance && stack.getCount()==1,"Wrong placed appearance or count");
                helper.assertTrue(entity.getUpdateTag().size()==1 && entity.asItem().getTag().size()==1,"Appearance duplicated configuration into NBT");
                var loaded=(PlacedBoxEntity)BlockEntity.loadStatic(pos,state,entity.saveWithFullMetadata());
                helper.assertTrue(loaded!=null && loaded.poolId().equals(box.id()) && loaded.getBlockState().getValue(PlacedBoxBlock.APPEARANCE)==appearance,
                        "World persistence lost appearance/identity");
                for (var direction : Direction.Plane.HORIZONTAL) {
                    var rotated=state.setValue(PlacedBoxBlock.FACING,direction);
                    double expected=appearance.height()/16D;
                    helper.assertTrue(Math.abs(rotated.getShape(level,pos).max(Direction.Axis.Y)-expected)<.00001,"Wrong collision height");
                }
                var preview=BoxService.previewData(UUID.randomUUID(),box);
                helper.assertTrue(ItemCsgoBox.boxId(preview.box()).equals(box.id()) && preview.box().getTag().size()==1,"Preview embeds appearance snapshots");
            }
        }); helper.succeed();
    }

    @GameTest(template="empty")
    public static void editedAppearanceRefreshesPlacedBoxWithoutChangingPoolOrFacing(GameTestHelper helper) throws Exception {
        withConfig(() -> {
            var box=definition(BoxAppearance.GOLD); CsgoBoxManage.saveDefinition(box,"");
            var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2));
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos,ModBlocks.BOX.get().defaultBlockState().setValue(PlacedBoxBlock.FACING,Direction.WEST));
            var entity=(PlacedBoxEntity)level.getBlockEntity(pos); entity.setPoolId(box.id());
            helper.assertTrue(level.getBlockState(pos).getValue(PlacedBoxBlock.APPEARANCE)==BoxAppearance.GOLD,"Initial appearance not applied");
            CsgoBoxManage.saveDefinition(box.withAppearance("green"),BoxConfigStore.revision(box));
            entity.refreshAppearance();
            var next=level.getBlockState(pos);
            helper.assertTrue(next.getValue(PlacedBoxBlock.APPEARANCE)==BoxAppearance.GREEN && next.getValue(PlacedBoxBlock.FACING)==Direction.WEST,
                    "Reload did not change appearance or changed facing");
            helper.assertTrue(level.getBlockEntity(pos)==entity && entity.poolId().equals(box.id()),"Refresh lost block entity identity");
            helper.assertTrue(ItemCsgoBox.boxId(entity.asItem()).equals(box.id()),"Break/pick item lost pool");
        }); helper.succeed();
    }

    @GameTest(template="empty")
    public static void bundledAppearanceExamplesAreUniqueCompleteAndValid(GameTestHelper helper) throws Exception {
        try (var input=AppearanceGameTests.class.getResourceAsStream("/examples/box-appearances.json")) {
            helper.assertTrue(input!=null,"Missing appearance examples");
            var examples=new com.google.gson.Gson().fromJson(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8),BoxDefinition[].class);
            var ids=new HashSet<String>(); var appearances=EnumSet.noneOf(BoxAppearance.class);
            for (var example : examples) {
                example.validate(value -> !ItemNBT.parseStrict(value).isEmpty());
                helper.assertTrue(ids.add(example.id()),"Duplicate example identity");
                appearances.add(BoxAppearance.byId(example.appearance()));
                helper.assertTrue(!example.grade5().isEmpty() && example.previewRewards().size()>0,"Example lacks rewards");
                helper.assertTrue(!example.acquisition().drop().enabled(),"Examples added automatic world drops");
            }
            helper.assertTrue(appearances.size()==BoxAppearance.values().length,"Examples do not cover every appearance");
        } helper.succeed();
    }
}

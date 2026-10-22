package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.block.*;
import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Manual development-only fixture; entering a world must never prepare or reset it. */
@Mod.EventBusSubscriber(modid=CsgoBox.MODID)
public final class GroundClientFixture {
    private record Demo(ServerPlayer viewer,long start,boolean[] begun) {}
    private static Demo demo;
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        if (!Boolean.getBoolean("csgobox.groundFixtureCommands")) return;
        event.getDispatcher().register(Commands.literal("csgobox_test_ground").requires(source -> source.hasPermission(2))
                .then(Commands.literal("prepare").executes(context -> prepare(context.getSource().getPlayerOrException(),false)))
                .then(Commands.literal("demo").executes(context -> prepare(context.getSource().getPlayerOrException(),true))));
    }
    private static int prepare(ServerPlayer player,boolean animate) {
        // Validate the complete QA configuration before changing the world or inventory.
        for (var appearance:BoxAppearance.values()) {
            var definition=CsgoBoxManage.find("csgobox:ground_qa_"+appearance.id());
            if (definition==null || definition.key().isEmpty()) {
                player.sendSystemMessage(Component.literal("Missing ground QA pools; test scene was not prepared."));
                return 0;
            }
            ItemNBT.parseStrict(definition.key());
        }
        demo=null;
        var level=player.serverLevel(); level.setDayTime(6000);
        player.server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
        player.setHealth(player.getMaxHealth()); player.getFoodData().setFoodLevel(20);
        for (int x=63;x<=70;x++) for (int z=186;z<=195;z++) level.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.STONE.defaultBlockState());
        for (var appearance:BoxAppearance.values()) {
            var definition=CsgoBoxManage.find("csgobox:ground_qa_"+appearance.id());
            var pos=new BlockPos(65+appearance.ordinal()%3,-60,190+appearance.ordinal()/3*2);
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos,ModBlocks.BOX.get().defaultBlockState().setValue(PlacedBoxBlock.FACING,Direction.NORTH)
                    .setValue(PlacedBoxBlock.APPEARANCE,appearance));
            ((PlacedBoxEntity)level.getBlockEntity(pos)).setPoolId(definition.id());
        }
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().clearContent();
        var keys=ItemNBT.parseStrict(CsgoBoxManage.find("csgobox:ground_qa_gold").key()); keys.setCount(32);
        player.getInventory().setItem(0,keys); player.getInventory().setItem(1,new ItemStack(Items.IRON_PICKAXE));
        player.getInventory().setItem(2,new ItemStack(ModItems.ITEM_CSGO_CRAFT.get()));
        for (var appearance:BoxAppearance.values()) player.getInventory().setItem(3+appearance.ordinal(),EmptyBoxItem.create(appearance));
        player.getInventory().setItem(10,ItemCsgoBox.setBoxId("csgobox:ground_qa_gold",new ItemStack(ModItems.ITEM_CSGOBOX.get(),4)));
        player.getInventory().selected=0;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        player.connection.teleport(66.5,-60,187.3,0,22);
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
        CsgoBox.LOGGER.info("Prepared six isolated ground cases and named keys for client inspection");
        if (animate) demo=new Demo(player,level.getGameTime(),new boolean[6]);
        return 1;
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (demo!=null && demo.viewer()==event.getEntity()) demo=null;
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { demo=null; }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase!=net.minecraftforge.event.TickEvent.Phase.END || demo==null) return;
        var level=demo.viewer().serverLevel(); long elapsed=level.getGameTime()-demo.start();
        for (var appearance:BoxAppearance.values()) {
            int i=appearance.ordinal();
            if (demo.begun()[i] || elapsed<200+i*50) continue;
            demo.begun()[i]=true;
            var pos=new BlockPos(65+i%3,-60,190+i/3*2);
            if (!(level.getBlockEntity(pos) instanceof PlacedBoxEntity box) || box.isOpening() || box.isEmpty()) continue;
            var actor=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"[GroundDemo]"));
            actor.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0,0,-2));
            actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemNBT.parseStrict(CsgoBoxManage.find(box.poolId()).key()));
            var result=com.reclizer.csgobox.server.PlacedBoxOpening.open(actor,pos,net.minecraft.world.InteractionHand.MAIN_HAND,new java.util.Random(i));
            CsgoBox.LOGGER.info("Ground demo started {}: success={}, pending={}, keyRemaining={}",appearance.id(),result.success(),box.isOpening(),actor.getMainHandItem().getCount());
        }
        if (elapsed>650) demo=null;
    }
}

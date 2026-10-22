package com.reclizer.csgobox.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class ClientFixtureGameTests {
    @GameTest(template="empty")
    public static void worldLoginNeverResetsPlayerOrSceneEvenWithLegacyFixtureFlags(GameTestHelper h) {
        var level=h.getLevel();
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"[LoginIsolation]"));
        player.setGameMode(GameType.CREATIVE);
        var position=h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5,2,2.5));
        player.moveTo(position.x,position.y,position.z,53,12);
        player.setHealth(11); player.getFoodData().setFoodLevel(13);
        var item=new ItemStack(Items.DIAMOND,7); item.getOrCreateTag().putBoolean("keepOnLogin",true);
        player.getInventory().setItem(4,item); player.getInventory().selected=4;
        var inventory=player.getInventory().save(new ListTag());
        long time=level.getDayTime(); var difficulty=level.getDifficulty();
        var fixturePosition=new BlockPos(63,-61,186); var block=level.getBlockState(fixturePosition);
        // Exercise the fixture's real event subscriptions without unrelated network-handshake
        // listeners: Forge FakePlayer deliberately has no live client channel.
        var events=BusBuilder.builder().build(); events.register(GroundClientFixture.class);
        String[] properties={"csgobox.groundFixture","csgobox.groundAutoOpen","csgobox.groundFixtureCommands"};
        var old=new String[properties.length];
        for (int i=0;i<properties.length;i++) old[i]=System.getProperty(properties[i]);
        try {
            for (var property:properties) System.setProperty(property,"true");
            for (int login=0;login<2;login++) {
                events.post(new PlayerEvent.PlayerLoggedInEvent(player));
                h.assertTrue(player.gameMode.getGameModeForPlayer()==GameType.CREATIVE,"Login changed the chosen game mode");
                h.assertTrue(player.position().equals(position) && player.getYRot()==53 && player.getXRot()==12,"Login moved the player to the test scene");
                h.assertTrue(player.getInventory().save(new ListTag()).equals(inventory) && player.getInventory().selected==4,"Login replaced the player's inventory");
                h.assertTrue(player.getHealth()==11 && player.getFoodData().getFoodLevel()==13,"Login reset player health/food");
                h.assertTrue(level.getDayTime()==time && level.getDifficulty()==difficulty,"Login changed time or difficulty");
                h.assertTrue(level.getBlockState(fixturePosition).equals(block),"Login rebuilt the ground test platform");
                events.post(new PlayerEvent.PlayerLoggedOutEvent(player));
            }
        } finally {
            for (int i=0;i<properties.length;i++) {
                if (old[i]==null) System.clearProperty(properties[i]); else System.setProperty(properties[i],old[i]);
            }
        }
        h.succeed();
    }
}

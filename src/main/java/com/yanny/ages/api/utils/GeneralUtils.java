package com.yanny.ages.api.utils;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IWorldInfo;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.hooks.BasicEventHooks;

import javax.annotation.Nonnull;

public class GeneralUtils {

    private static final int HOURS = 20 * 60 * 60;
    private static final int MINUTES = 20 * 60;
    private static final int SECONDS = 20;

    @Nonnull
    public static String tickToTime(int burnTime) {
        int hours = burnTime / HOURS;
        burnTime = burnTime % HOURS;
        int minutes = burnTime / MINUTES;
        burnTime = burnTime % MINUTES;
        int seconds = burnTime / SECONDS;

        return (hours > 0 ? hours + "h:":"") + ((hours > 0 || minutes > 0) ? minutes + "m:" : "") + (seconds + "s");
    }

    public static void changeDim(ServerPlayerEntity player, double x, double y, double z, RegistryKey<World> type) {
        if (!ForgeHooks.onTravelToDimension(player, type)) {
            return;
        }

        RegistryKey<World> dimensiontype = player.func_241141_L_();
        ServerWorld srcWorld = player.server.getWorld(dimensiontype);
        //player.dimension = type; TODO ??? 1.16 ???
        ServerWorld destWorld = player.server.getWorld(type);
        IWorldInfo worldinfo = player.world.getWorldInfo();
        player.connection.sendPacket(new SRespawnPacket(srcWorld.func_230315_m_(), type, 20523245, player.interactionManager.getGameType(), player.interactionManager.getGameType(), false, false, false)); //TODO 1.16 ???
        player.connection.sendPacket(new SServerDifficultyPacket(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));
        PlayerList playerlist = player.server.getPlayerList();
        playerlist.updatePermissionLevel(player);
        srcWorld.removeEntity(player, true);
        player.revive();
        float f = player.rotationPitch;
        float f1 = player.rotationYaw;

        player.setLocationAndAngles(x, y, z, f1, f);
        player.setWorld(destWorld);
        destWorld.addNewPlayer(player); //TODO 1.16 ????
        player.connection.setPlayerLocation(x, y, z, f1, f);
        player.interactionManager.setWorld(destWorld);
        player.connection.sendPacket(new SPlayerAbilitiesPacket(player.abilities));
        playerlist.sendWorldInfo(player, destWorld);
        playerlist.sendInventory(player);

        for(EffectInstance effectinstance : player.getActivePotionEffects()) {
            player.connection.sendPacket(new SPlayEntityEffectPacket(player.getEntityId(), effectinstance));
        }

        player.connection.sendPacket(new SPlaySoundEventPacket(1032, BlockPos.ZERO, 0, false));
        BasicEventHooks.firePlayerChangedDimensionEvent(player, dimensiontype, type);
    }
}

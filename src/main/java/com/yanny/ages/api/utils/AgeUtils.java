package com.yanny.ages.api.utils;

import com.yanny.ages.api.enums.Age;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import static com.yanny.ages.api.Reference.MODID;

public class AgeUtils {
    private static final String PLAYER_AGE_NBT = MODID + "_PLAYER_AGE";

    public static void initPlayerAge(PlayerEntity playerEntity) {
        CompoundNBT nbt = playerEntity.getPersistentData();
        CompoundNBT persistent;

        if (!nbt.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
            nbt.put(PlayerEntity.PERSISTED_NBT_TAG, (persistent = new CompoundNBT()));
        } else {
            persistent = nbt.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        }

        if (!persistent.contains(PLAYER_AGE_NBT)) {
            persistent.putInt(PLAYER_AGE_NBT, Age.STONE_AGE.value);
        }
    }

    public static int getPlayerAge(PlayerEntity playerEntity) {
        CompoundNBT nbt = playerEntity.getPersistentData();
        CompoundNBT persistent;

        if (!nbt.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
            nbt.put(PlayerEntity.PERSISTED_NBT_TAG, (persistent = new CompoundNBT()));
        } else {
            persistent = nbt.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        }

        if (!persistent.contains(PLAYER_AGE_NBT)) {
            persistent.putInt(PLAYER_AGE_NBT, Age.STONE_AGE.value);
        }

        return persistent.getInt(PLAYER_AGE_NBT);
    }

    public static void setPlayerAge(PlayerEntity playerEntity, Age age) {
        CompoundNBT nbt = playerEntity.getPersistentData();
        CompoundNBT persistent;

        if (!nbt.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
            nbt.put(PlayerEntity.PERSISTED_NBT_TAG, (persistent = new CompoundNBT()));
        } else {
            persistent = nbt.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        }

        persistent.putInt(PLAYER_AGE_NBT, age.value);
    }
}

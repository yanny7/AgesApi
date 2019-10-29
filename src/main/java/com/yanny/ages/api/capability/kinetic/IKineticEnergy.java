package com.yanny.ages.api.capability.kinetic;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

public interface IKineticEnergy extends INBTSerializable<CompoundNBT> {
    int getAvailableEnergy();

    void setAvailableEnergy(int energy);
}

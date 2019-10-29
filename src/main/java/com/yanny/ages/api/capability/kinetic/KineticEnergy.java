package com.yanny.ages.api.capability.kinetic;

import net.minecraft.nbt.CompoundNBT;

public class KineticEnergy implements IKineticEnergy {
    private int currentEnergy;

    public KineticEnergy() {
        currentEnergy = 0;
    }

    @Override
    public int getAvailableEnergy() {
        return currentEnergy;
    }

    @Override
    public void setAvailableEnergy(int energy) {
        currentEnergy = energy;
    }

    @Override
    public String toString() {
        return "[energy: " + currentEnergy + "]";
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT tag = new CompoundNBT();
        tag.putInt("energy", currentEnergy);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundNBT tag) {
        currentEnergy = tag.getInt("energy");
    }
}

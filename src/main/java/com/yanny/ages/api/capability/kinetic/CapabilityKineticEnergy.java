package com.yanny.ages.api.capability.kinetic;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class CapabilityKineticEnergy {
    @CapabilityInject(IKineticEnergy.class)
    public static Capability<IKineticEnergy> ENERGY_CAPABILITY = null;

    public static void register()
    {
        CapabilityManager.INSTANCE.register(IKineticEnergy.class, new Capability.IStorage<IKineticEnergy>() {
                @Override
                public INBT writeNBT(Capability<IKineticEnergy> capability, IKineticEnergy instance, Direction side) {
                    ListNBT nbtTagList = new ListNBT();
                    CompoundNBT tag = new CompoundNBT();
                    tag.putInt("energy", instance.getAvailableEnergy());
                    return nbtTagList;
                }

                @Override
                public void readNBT(Capability<IKineticEnergy> capability, IKineticEnergy instance, Direction side, INBT nbt) {
                    ListNBT tagList = (ListNBT) nbt;
                    CompoundNBT tag = tagList.getCompound(0);
                    instance.setAvailableEnergy(tag.getInt("energy"));
                }
        }, KineticEnergy::new);
    }
}

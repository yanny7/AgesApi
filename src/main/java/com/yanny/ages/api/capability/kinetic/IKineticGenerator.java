package com.yanny.ages.api.capability.kinetic;

import net.minecraft.util.Direction;

import javax.annotation.Nullable;

public interface IKineticGenerator extends IKinetic {
    float getRotationPerTick();

    @Nullable
    Direction getDirection();

    int getGeneratedPower();
}

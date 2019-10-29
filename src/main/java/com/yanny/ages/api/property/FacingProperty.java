package com.yanny.ages.api.property;

import com.google.common.collect.Maps;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;

import java.util.Map;

public final class FacingProperty {
    public static final int FALSE = 0;
    public static final int FORCE_FALSE = 1;
    public static final int TRUE = 2;
    public static final int FORCE_TRUE = 3;

    public static final IntegerProperty UP = IntegerProperty.create("up", 0, 3);
    public static final IntegerProperty DOWN = IntegerProperty.create("down", 0, 3);
    public static final IntegerProperty NORTH = IntegerProperty.create("north", 0, 3);
    public static final IntegerProperty EAST = IntegerProperty.create("east", 0, 3);
    public static final IntegerProperty SOUTH = IntegerProperty.create("south", 0, 3);
    public static final IntegerProperty WEST = IntegerProperty.create("west", 0, 3);

    public static final Map<Direction, IntegerProperty> FACING_TO_PROPERTY_MAP = Util.make(Maps.newEnumMap(Direction.class), (map) -> {
        map.put(Direction.NORTH, NORTH);
        map.put(Direction.EAST, EAST);
        map.put(Direction.SOUTH, SOUTH);
        map.put(Direction.WEST, WEST);
        map.put(Direction.UP, UP);
        map.put(Direction.DOWN, DOWN);
    });

    private FacingProperty() {}
}

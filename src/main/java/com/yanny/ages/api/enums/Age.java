package com.yanny.ages.api.enums;

public enum Age {
    STONE_AGE(10),
    BRONZE_AGE(20),
    ;

    public int value;

    Age(int value) {
        this.value = value;
    }
}

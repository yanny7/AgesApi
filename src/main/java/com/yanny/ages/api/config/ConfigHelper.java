package com.yanny.ages.api.config;

import net.minecraftforge.fml.config.ModConfig;

import javax.annotation.Nonnull;

public class ConfigHelper {

    public static void bakeServer() {
        Config.forceReplaceVanillaResources = ConfigHolder.SERVER.forceReplaceVanillaResources.get();
    }

    public static void bakeClient() {
    }

    public static void setValueAndSave(@Nonnull final ModConfig modConfig, @Nonnull final String path, @Nonnull final Object newValue) {
        modConfig.getConfigData().set(path, newValue);
        modConfig.save();
    }
}

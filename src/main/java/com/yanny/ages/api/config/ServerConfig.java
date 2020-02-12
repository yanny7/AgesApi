package com.yanny.ages.api.config;

import com.yanny.ages.api.Reference;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nonnull;

class ServerConfig {
    final ForgeConfigSpec.BooleanValue forceReplaceVanillaResources;

    ServerConfig(@Nonnull final ForgeConfigSpec.Builder builder) {
        builder.push("general");
        forceReplaceVanillaResources = builder
                .comment("Replace vanilla recipes/loot_tables after merging from another mods. This is enabled due bug" +
                        " in forge when resources from this mod are loaded before minecraft resources and thus replaced")
                .translation(Reference.MODID + ".config.force_replace_vanilla_resources")
                .define("forceReplaceVanillaResources", true);
        builder.pop();
    }
}

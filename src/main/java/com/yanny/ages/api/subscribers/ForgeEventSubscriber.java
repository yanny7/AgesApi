package com.yanny.ages.api.subscribers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.yanny.ages.api.config.Config;
import com.yanny.ages.api.enums.Age;
import com.yanny.ages.api.utils.AgeUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import static com.yanny.ages.api.Reference.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventSubscriber {

    private static final Set<ResourceLocation> LOOT_TABLES_TO_REPLACE = Sets.newHashSet(
            new ResourceLocation(MODID, "blocks/acacia_leaves"),
            new ResourceLocation(MODID, "blocks/birch_leaves"),
            new ResourceLocation(MODID, "blocks/cobweb"),
            new ResourceLocation(MODID, "blocks/dark_oak_leaves"),
            new ResourceLocation(MODID, "blocks/dead_bush"),
            new ResourceLocation(MODID, "blocks/fern"),
            new ResourceLocation(MODID, "blocks/grass"),
            new ResourceLocation(MODID, "blocks/jungle_leaves"),
            new ResourceLocation(MODID, "blocks/large_fern"),
            new ResourceLocation(MODID, "blocks/oak_leaves"),
            new ResourceLocation(MODID, "blocks/seagrass"),
            new ResourceLocation(MODID, "blocks/spruce_leaves"),
            new ResourceLocation(MODID, "blocks/tall_grass"),
            new ResourceLocation(MODID, "blocks/tall_seagrass"),
            new ResourceLocation(MODID, "blocks/vine")
    );

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void FMLServerStartingEvent(FMLServerStartingEvent event) {
        if (Config.forceReplaceVanillaResources) {
            LootTableManager lootTableManager = event.getServer().getLootTableManager();
            Class<?> lootTableManagerClass = lootTableManager.getClass();

            try {
                Field lootTables = lootTableManagerClass.getDeclaredFields()[2];
                lootTables.setAccessible(true);
                Map<ResourceLocation, LootTable> lootTableMap = (Map<ResourceLocation, LootTable>) lootTables.get(lootTableManager);
                Map<ResourceLocation, LootTable> map = Maps.newHashMap();
                map.putAll(lootTableMap);

                LOOT_TABLES_TO_REPLACE.forEach(resourceLocation -> {
                    LootTable loot = lootTableMap.get(resourceLocation);
                    map.put(new ResourceLocation("minecraft", resourceLocation.getPath()), loot);
                    map.remove(resourceLocation);
                });

                lootTables.set(lootTableManager, ImmutableMap.copyOf(map));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public static void setupPlayerAge(PlayerEvent.PlayerLoggedInEvent event) {
        AgeUtils.initPlayerAge(event.getPlayer());
    }
}

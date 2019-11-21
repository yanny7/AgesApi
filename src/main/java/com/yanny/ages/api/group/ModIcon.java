package com.yanny.ages.api.group;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

import static com.yanny.ages.api.Reference.*;

@SuppressWarnings("WeakerAccess")
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
@ObjectHolder(MODID)
public class ModIcon {
    public static final Item icon = null;

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        registry.register(new Item(new Item.Properties().maxStackSize(1)).setRegistryName(MODID, "icon"));
    }
}

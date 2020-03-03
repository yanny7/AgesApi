package com.yanny.ages.api.subscribers;

import com.yanny.ages.api.utils.AgeUtils;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.yanny.ages.api.Reference.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventSubscriber {

    @SubscribeEvent
    public static void setupPlayerAge(PlayerEvent.PlayerLoggedInEvent event) {
        AgeUtils.initPlayerAge(event.getPlayer());
    }
}

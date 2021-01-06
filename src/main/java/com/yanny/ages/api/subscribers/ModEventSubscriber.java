package com.yanny.ages.api.subscribers;

import com.yanny.ages.api.config.ConfigHelper;
import com.yanny.ages.api.config.ConfigHolder;
import com.yanny.ages.api.datagen.BlockTagGenerator;
import com.yanny.ages.api.datagen.ItemTagGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

import javax.annotation.Nonnull;

import static com.yanny.ages.api.Reference.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventSubscriber {

    @SubscribeEvent
    public static void onModConfigEvent(ModConfig.ModConfigEvent event) {
        final ModConfig config = event.getConfig();

        if (!config.getModId().equals(MODID)) {
            return;
        }

        // Rebake the configs when they change
        if (config.getSpec() == ConfigHolder.CLIENT_SPEC) {
            ConfigHelper.bakeClient();
        } else if (config.getSpec() == ConfigHolder.SERVER_SPEC) {
            ConfigHelper.bakeServer();
        }
    }

    @SubscribeEvent
    public static void dataGen(@Nonnull GatherDataEvent event) {
        if (event.includeServer()) {
            BlockTagGenerator blockTagGenerator = new BlockTagGenerator(event.getGenerator(), MODID, event.getExistingFileHelper());
            ItemTagGenerator itemTagGenerator = new ItemTagGenerator(event.getGenerator(), blockTagGenerator, MODID, event.getExistingFileHelper());

            event.getGenerator().addProvider(blockTagGenerator);
            event.getGenerator().addProvider(itemTagGenerator);
        }
    }
}

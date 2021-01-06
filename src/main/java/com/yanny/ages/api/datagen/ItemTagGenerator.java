package com.yanny.ages.api.datagen;

import com.yanny.ages.api.utils.Tags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.item.Items;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class ItemTagGenerator extends ItemTagsProvider {

    public ItemTagGenerator(DataGenerator dataGenerator) {
        super(dataGenerator);
    }

    @Override
    protected void registerTags() {
        getBuilder(Tags.Items.AXES).add(Items.WOODEN_AXE).add(Items.STONE_AXE).add(Items.IRON_AXE).add(Items.GOLDEN_AXE).add(Items.DIAMOND_AXE);
        getBuilder(Tags.Items.BONES).add(Items.BONE);
        getBuilder(Tags.Items.FISHING_NET_MESHES);
        getBuilder(Tags.Items.HAMMERS);
        getBuilder(Tags.Items.KNIVES);
    }
}

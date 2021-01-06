package com.yanny.ages.api.datagen;

import com.yanny.ages.api.utils.Tags;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.item.Items;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.lwjgl.system.NonnullDefault;

import javax.annotation.Nullable;

@NonnullDefault
public class ItemTagGenerator extends ItemTagsProvider {

    public ItemTagGenerator(DataGenerator dataGenerator, BlockTagsProvider blockTagProvider, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(dataGenerator, blockTagProvider, modId, existingFileHelper);
    }

    @Override
    protected void registerTags() {
        getOrCreateBuilder(Tags.Items.AXES).add(Items.WOODEN_AXE).add(Items.STONE_AXE).add(Items.IRON_AXE).add(Items.GOLDEN_AXE).add(Items.DIAMOND_AXE);
        getOrCreateBuilder(Tags.Items.BONES).add(Items.BONE);
        getOrCreateBuilder(Tags.Items.FISHING_NET_MESHES);
        getOrCreateBuilder(Tags.Items.HAMMERS);
        getOrCreateBuilder(Tags.Items.KNIVES);
    }
}

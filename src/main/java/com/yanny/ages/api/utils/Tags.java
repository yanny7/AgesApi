package com.yanny.ages.api.utils;

import com.yanny.ages.api.Reference;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

public class Tags {
    public static class Blocks {

    }

    public static class Items {
        public static final ITag.INamedTag<Item> AXES = tag(new ResourceLocation(Reference.MODID, "axes"));
        public static final ITag.INamedTag<Item> HAMMERS = tag(new ResourceLocation(Reference.MODID, "hammers"));
        public static final ITag.INamedTag<Item> KNIFES = tag(new ResourceLocation(Reference.MODID, "knifes"));
        public static final ITag.INamedTag<Item> FISHING_NET_MESHES = tag(new ResourceLocation(Reference.MODID, "fishing_net_meshes"));
        public static final ITag.INamedTag<Item> SHEARS = tag(new ResourceLocation("forge", "shears"));

        private static ITag.INamedTag<Item> tag(ResourceLocation resourceLocation) {
            return ItemTags.makeWrapperTag(resourceLocation.toString());
        }
    }
}

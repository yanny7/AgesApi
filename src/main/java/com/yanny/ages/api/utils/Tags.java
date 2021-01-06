package com.yanny.ages.api.utils;

import com.yanny.ages.api.Reference;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

public class Tags {
    public static class Blocks {

    }

    public static class Items {
        public static final ItemTags.Wrapper AXES = new ItemTags.Wrapper(new ResourceLocation(Reference.MODID, "axes"));
        public static final ItemTags.Wrapper HAMMERS = new ItemTags.Wrapper(new ResourceLocation(Reference.MODID, "hammers"));
        public static final ItemTags.Wrapper KNIVES = new ItemTags.Wrapper(new ResourceLocation(Reference.MODID, "knives"));
        public static final ItemTags.Wrapper FISHING_NET_MESHES = new ItemTags.Wrapper(new ResourceLocation(Reference.MODID, "fishing_net_meshes"));
        public static final ItemTags.Wrapper BONES = new ItemTags.Wrapper(new ResourceLocation(Reference.MODID, "bones"));
    }
}

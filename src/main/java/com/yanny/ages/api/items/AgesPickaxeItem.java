package com.yanny.ages.api.items;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraftforge.common.ToolType;

import java.util.Set;

import static net.minecraft.block.material.Material.*;

public class AgesPickaxeItem extends AgesToolItem {
    private static final Set<Block> EFFECTIVE_ON;
    private static final Set<Material> EFFECTIVE_MATERIALS;

    static {
        EFFECTIVE_ON = getPrivateValue(PickaxeItem.class, null, 0);
        EFFECTIVE_MATERIALS = Sets.newHashSet(ROCK, IRON, ANVIL);
    }

    public AgesPickaxeItem(IItemTier tier, float attackDamageIn, float attackSpeedIn, Properties builder) {
        super(attackDamageIn, attackSpeedIn, tier, EFFECTIVE_ON, builder, true, true, true);
    }

    /**
     * Check whether this Item can harvest the given Block
     */
    @Override
    public boolean canHarvestBlock(BlockState blockIn) {
        int i = this.getTier().getHarvestLevel();

        if (blockIn.getHarvestTool() == ToolType.PICKAXE) {
            return i >= blockIn.getHarvestLevel();
        }

        return EFFECTIVE_MATERIALS.contains(blockIn.getMaterial());
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return !EFFECTIVE_MATERIALS.contains(state.getMaterial()) ? super.getDestroySpeed(stack, state) : this.efficiency + getAdditionalEfficiency(stack);
    }
}

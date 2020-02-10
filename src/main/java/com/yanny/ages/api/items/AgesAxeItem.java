package com.yanny.ages.api.items;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

import static net.minecraft.block.material.Material.*;

public class AgesAxeItem extends AgesToolItem {
    private static final Set<Block> EFFECTIVE_ON;
    private static final Map<Block, Block> BLOCK_STRIPPING_MAP;
    private static final Set<Material> EFFECTIVE_MATERIALS;

    static {
        EFFECTIVE_ON = getPrivateValue(AxeItem.class, null, 0);
        BLOCK_STRIPPING_MAP = getPrivateValue(AxeItem.class, null, 1);
        EFFECTIVE_MATERIALS = Sets.newHashSet(WOOD, PLANTS, TALL_PLANTS, BAMBOO, LEAVES, CACTUS);
    }

    public AgesAxeItem(IItemTier tier, float attackDamageIn, float attackSpeedIn, Properties builder) {
        super(attackDamageIn, attackSpeedIn, tier, EFFECTIVE_ON, builder, true, true, true);
    }
    @Override
    public boolean canHarvestBlock(BlockState blockIn) {
        int i = this.getTier().getHarvestLevel();

        if (blockIn.getHarvestTool() == ToolType.AXE) {
            return i >= blockIn.getHarvestLevel();
        }

        return EFFECTIVE_MATERIALS.contains(blockIn.getMaterial());
    }

    @Override
    public float getDestroySpeed(@Nonnull ItemStack stack, BlockState state) {
        return !EFFECTIVE_MATERIALS.contains(state.getMaterial()) ? super.getDestroySpeed(stack, state) : efficiency + getAdditionalEfficiency(stack);
    }

    @Nonnull
    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        BlockPos blockpos = context.getPos();
        BlockState blockstate = world.getBlockState(blockpos);
        Block block = BLOCK_STRIPPING_MAP.get(blockstate.getBlock());

        if (block != null) {
            PlayerEntity playerentity = context.getPlayer();
            world.playSound(playerentity, blockpos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);

            if (!world.isRemote) {
                world.setBlockState(blockpos, block.getDefaultState().with(RotatedPillarBlock.AXIS, blockstate.get(RotatedPillarBlock.AXIS)), 11);

                if (playerentity != null) {
                    context.getItem().damageItem(1, playerentity, (playerEntity) -> playerEntity.sendBreakAnimation(context.getHand()));
                }
            }

            return ActionResultType.SUCCESS;
        } else {
            return ActionResultType.PASS;
        }
    }
}

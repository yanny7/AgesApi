package com.yanny.ages.api.items;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.ShovelItem;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

import static net.minecraft.block.material.Material.SNOW;
import static net.minecraft.block.material.Material.SNOW_BLOCK;

public class AgesShovelItem extends AgesToolItem {
    private static final Set<Block> EFFECTIVE_ON;
    private static final Map<Block, BlockState> SHOVEL_LOOKUP;
    private static final Set<Material> EFFECTIVE_MATERIALS;

    static {
        EFFECTIVE_ON = getPrivateValue(ShovelItem.class, null, 0);
        SHOVEL_LOOKUP = getPrivateValue(ShovelItem.class, null, 1);
        EFFECTIVE_MATERIALS = Sets.newHashSet(SNOW, SNOW_BLOCK);
    }

    public AgesShovelItem(IItemTier tier, float attackDamageIn, float attackSpeedIn, Properties builder) {
        super(attackDamageIn, attackSpeedIn, tier, EFFECTIVE_ON, builder, true, true, true);
    }

    @Override
    public boolean canHarvestBlock(BlockState blockIn) {
        return EFFECTIVE_MATERIALS.contains(blockIn.getMaterial());
    }

    /**
     * Called when this item is used when targetting a Block
     */
    @Nonnull
    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        BlockPos blockpos = context.getPos();
        BlockState blockstate = world.getBlockState(blockpos);

        if (context.getFace() == Direction.DOWN) {
            return ActionResultType.PASS;
        } else {
            PlayerEntity playerentity = context.getPlayer();
            BlockState effectiveBlockState = SHOVEL_LOOKUP.get(blockstate.getBlock());
            BlockState effective = null;

            if (effectiveBlockState != null && world.isAirBlock(blockpos.up())) {
                world.playSound(playerentity, blockpos, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
                effective = effectiveBlockState;
            } else if (blockstate.getBlock() instanceof CampfireBlock && blockstate.get(CampfireBlock.LIT)) {
                world.playEvent(null, 1009, blockpos, 0);
                effective = blockstate.with(CampfireBlock.LIT, Boolean.FALSE);
            }

            if (effective != null) {
                if (!world.isRemote) {
                    world.setBlockState(blockpos, effective, 11);

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
}

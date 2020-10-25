package com.yanny.ages.api.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Map;

public class AgesHoeItem extends AgesToolItem {
    private static final Map<Block, BlockState> HOE_LOOKUP;

    static {
        HOE_LOOKUP = getPrivateValue(HoeItem.class, null, 1);
    }

    public AgesHoeItem(IItemTier tier, float attackSpeedIn, Properties builder) {
        super(0f, attackSpeedIn, tier, Sets.newHashSet(), builder, false, true, true);
    }

    /**
     * Called when this item is used when targetting a Block
     */
    @Nonnull
    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        BlockPos blockpos = context.getPos();
        int hook = net.minecraftforge.event.ForgeEventFactory.onHoeUse(context);

        if (hook != 0) {
            return hook > 0 ? ActionResultType.SUCCESS : ActionResultType.FAIL;
        }

        if (context.getFace() != Direction.DOWN && world.isAirBlock(blockpos.up())) {
            BlockState blockstate = HOE_LOOKUP.get(world.getBlockState(blockpos).getBlock());

            if (blockstate != null) {
                PlayerEntity playerentity = context.getPlayer();
                world.playSound(playerentity, blockpos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);

                if (!world.isRemote) {
                    world.setBlockState(blockpos, blockstate, 11);

                    if (playerentity != null) {
                        context.getItem().damageItem(1, playerentity, (playerEntity) -> playerEntity.sendBreakAnimation(context.getHand()));
                    }
                }

                return ActionResultType.SUCCESS;
            }
        }

        return ActionResultType.PASS;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack stack) {
        if (slot == EquipmentSlotType.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> attributeModifierBuilder = ImmutableMultimap.builder();
            attributeModifierBuilder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_MODIFIER,
                    "Tool modifier", 0, AttributeModifier.Operation.ADDITION));
            attributeModifierBuilder.put(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_MODIFIER,
                    "Tool modifier", this.attackSpeed + getAdditionalAttackSpeed(stack), AttributeModifier.Operation.ADDITION));
            return attributeModifierBuilder.build();
        } else {
            return super.getAttributeModifiers(slot, stack);
        }
    }

    /**
     * Current implementations of this method in child classes do not use the entry argument beside ev. They just raise
     * the damage on the stack.
     */
    @Override
    public boolean hitEntity(ItemStack stack, @Nonnull LivingEntity target, @Nonnull LivingEntity attacker) {
        stack.damageItem(1, attacker, (livingEntity) -> livingEntity.sendBreakAnimation(EquipmentSlotType.MAINHAND));
        return true;
    }
}

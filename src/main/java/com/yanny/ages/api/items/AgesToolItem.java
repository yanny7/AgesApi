package com.yanny.ages.api.items;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IItemTier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TieredItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class AgesToolItem extends TieredItem {
    private final Set<Block> effectiveBlocks;
    private final boolean displayAttackDamage;
    private final boolean displayAttackSpeed;
    private final boolean displayEfficiency;
    protected float efficiency;
    protected float attackDamage;
    protected float attackSpeed;

    public AgesToolItem(float attackDamageIn, float attackSpeedIn, IItemTier tier, Set<Block> effectiveBlocksIn, Item.Properties builder,
                        boolean displayAttackDamage, boolean displayAttackSpeed, boolean displayEfficiency) {
        super(tier, builder);
        this.effectiveBlocks = effectiveBlocksIn;
        this.efficiency = tier.getEfficiency();
        this.attackDamage = attackDamageIn + tier.getAttackDamage();
        this.attackSpeed = attackSpeedIn;
        this.displayAttackDamage = displayAttackDamage;
        this.displayAttackSpeed = displayAttackSpeed;
        this.displayEfficiency = displayEfficiency;
    }

    @Override
    public float getDestroySpeed(@Nonnull ItemStack stack, BlockState state) {
        if (getToolTypes(stack).stream().anyMatch(state::isToolEffective)) {
            return efficiency + getAdditionalEfficiency(stack);
        }

        return this.effectiveBlocks.contains(state.getBlock()) ? this.efficiency + getAdditionalEfficiency(stack) : 1.0F;
    }

    /**
     * Current implementations of this method in child classes do not use the entry argument beside ev. They just raise
     * the damage on the stack.
     */
    @Override
    public boolean hitEntity(ItemStack stack, @Nonnull LivingEntity target, @Nonnull LivingEntity attacker) {
        stack.damageItem(2, attacker, (livingEntity) -> livingEntity.sendBreakAnimation(EquipmentSlotType.MAINHAND));
        return true;
    }

    /**
     * Called when a Block is destroyed using this Item. Return true to trigger the "Use Item" statistic.
     */
    @Override
    public boolean onBlockDestroyed(@Nonnull ItemStack stack, World worldIn, @Nonnull BlockState state, @Nonnull BlockPos pos, @Nonnull LivingEntity entityLiving) {
        if (!worldIn.isRemote && state.getBlockHardness(worldIn, pos) != 0.0F) {
            stack.damageItem(1, entityLiving, (livingEntity) -> livingEntity.sendBreakAnimation(EquipmentSlotType.MAINHAND));
        }

        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack stack) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> attributeModifierBuilder = ImmutableMultimap.builder();
        attributeModifierBuilder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_MODIFIER,
                "Tool modifier", this.attackDamage + getAdditionalAttackDamage(stack), AttributeModifier.Operation.ADDITION));
        attributeModifierBuilder.put(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_MODIFIER,
                "Tool modifier", this.attackSpeed + getAdditionalAttackSpeed(stack), AttributeModifier.Operation.ADDITION));
        return attributeModifierBuilder.build();
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        if (stack.getItem() instanceof AgesToolItem) {
            if (displayAttackDamage) {
                float attackDamage = getAdditionalAttackDamage(stack);

                if (Math.abs(attackDamage) > 0.01) {
                    tooltip.add(new StringTextComponent("Attack damage: ").mergeStyle(TextFormatting.DARK_GREEN)
                            .append(new StringTextComponent(String.format("%.2f", attackDamage))
                                    .mergeStyle(attackDamage >= 0 ? TextFormatting.GREEN : TextFormatting.RED)));
                }
            }

            if (displayAttackSpeed) {
                float attackSpeed = getAdditionalAttackSpeed(stack);

                if (Math.abs(attackSpeed) > 0.01) {
                    tooltip.add(new StringTextComponent("Attack speed: ").mergeStyle(TextFormatting.DARK_GREEN)
                            .append(new StringTextComponent(String.format("%.2f", attackSpeed))
                                    .mergeStyle(attackSpeed >= 0 ? TextFormatting.GREEN : TextFormatting.RED)));
                }
            }

            if (displayEfficiency) {
                float efficiency = getAdditionalEfficiency(stack);

                if (Math.abs(efficiency) > 0.01) {
                    tooltip.add(new StringTextComponent("Efficiency: ").mergeStyle(TextFormatting.DARK_GREEN)
                            .append(new StringTextComponent(String.format("%.2f", efficiency))
                                    .mergeStyle(efficiency >= 0 ? TextFormatting.GREEN : TextFormatting.RED)));
                }
            }
        }
    }

    public static float getAdditionalAttackDamage(ItemStack itemStack) {
        CompoundNBT tag = itemStack.getOrCreateChildTag("additionalModifiers");
        return tag.getFloat("attackDamage");
    }

    public static float getAdditionalAttackSpeed(ItemStack itemStack) {
        CompoundNBT tag = itemStack.getOrCreateChildTag("additionalModifiers");
        return tag.getFloat("attackSpeed");
    }

    public static float getAdditionalEfficiency(ItemStack itemStack) {
        CompoundNBT tag = itemStack.getOrCreateChildTag("additionalModifiers");
        return tag.getFloat("efficiency");
    }

    public static void setAdditionalModifiers(ItemStack itemStack, float attackDamage, float attackSpeed, float efficiency) {
        CompoundNBT tag = itemStack.getOrCreateChildTag("additionalModifiers");
        tag.putFloat("attackDamage", attackDamage);
        tag.putFloat("attackSpeed", attackSpeed);
        tag.putFloat("efficiency", efficiency);
    }

    @SuppressWarnings("unchecked")
    public static <T, E> T getPrivateValue(Class <? super E > classToAccess, @Nullable E instance, int fieldIndex)
    {
        try
        {
            Field f = classToAccess.getDeclaredFields()[fieldIndex];
            f.setAccessible(true);
            return (T) f.get(instance);
        }
        catch (Exception e)
        {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }
}

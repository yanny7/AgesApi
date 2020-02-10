package com.yanny.ages.api.items;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class AgesPartItem extends Item {

    private IAdditionalProperties additionalProperties;

    public AgesPartItem(Properties properties, IAdditionalProperties additionalProperties) {
        super(properties);
        this.additionalProperties = additionalProperties;
    }

    public void applyStats(ItemStack itemStack) {
        AgesToolItem.setAdditionalModifiers(itemStack, additionalProperties.getAttackDamage(), additionalProperties.getAttackSpeed(), additionalProperties.getEfficiency());
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        if (stack.getItem() instanceof AgesPartItem) {
            float attackDamage = AgesToolItem.getAdditionalAttackDamage(stack);
            float attackSpeed = AgesToolItem.getAdditionalAttackSpeed(stack);
            float efficiency = AgesToolItem.getAdditionalEfficiency(stack);

            tooltip.add(new StringTextComponent("Attack damage: ").applyTextStyle(TextFormatting.DARK_GREEN)
                    .appendSibling(new StringTextComponent(String.format("%.2f", attackDamage))
                            .applyTextStyle(attackDamage >= 0 ? TextFormatting.GREEN : TextFormatting.RED)));
            tooltip.add(new StringTextComponent("Attack speed: ").applyTextStyle(TextFormatting.DARK_GREEN)
                    .appendSibling(new StringTextComponent(String.format("%.2f", attackSpeed))
                            .applyTextStyle(attackSpeed >= 0 ? TextFormatting.GREEN : TextFormatting.RED)));
            tooltip.add(new StringTextComponent("Efficiency: ").applyTextStyle(TextFormatting.DARK_GREEN)
                    .appendSibling(new StringTextComponent(String.format("%.2f", efficiency))
                            .applyTextStyle(efficiency >= 0 ? TextFormatting.GREEN : TextFormatting.RED)));
        }
    }
}

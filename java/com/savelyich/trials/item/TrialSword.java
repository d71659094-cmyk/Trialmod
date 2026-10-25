package com.savelyich.trials.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Базовый ближний клинок мода: общий урон/скорость и строка описания из локализации. */
public class TrialSword extends SwordItem {
    private final String id;

    /** @param damage итоговый урон по цели в руке игрока (с учётом базовой единицы) */
    public TrialSword(String id, int durability, float damage, float attackSpeed) {
        super(new TrialTier(durability, 6.0f), Math.round(damage) - 1, attackSpeed - 4.0f,
                new Properties().stacksTo(1));
        this.id = id;
    }

    /** Дополнительные атрибуты (дальность и т.п.), по умолчанию нет. */
    protected Multimap<Attribute, AttributeModifier> extraModifiers() { return ImmutableMultimap.of(); }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.MAINHAND) return base;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.putAll(base);
        b.putAll(extraModifiers());
        return b.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("item.savelyich_trials." + id + ".desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}

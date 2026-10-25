package com.savelyich.trials.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public record TrialTier(int uses, float speed) implements Tier {
    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    /** Бонус урона нулевой: весь урон задаётся в конструкторе оружия. */
    @Override public float getAttackDamageBonus() { return 0; }
    @Override public int getLevel() { return 3; }
    @Override public int getEnchantmentValue() { return 15; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
}

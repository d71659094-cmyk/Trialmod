package com.savelyich.trials.registry;

import com.savelyich.trials.TrialsMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TrialsMod.MODID);

    /** Кровавый долг: снижает максимальное здоровье на 2 сердца за уровень. */
    public static final RegistryObject<MobEffect> BLOOD_DEBT = EFFECTS.register("blood_debt", () ->
            new MobEffect(MobEffectCategory.HARMFUL, 0x8B0000) { }
                    .addAttributeModifier(Attributes.MAX_HEALTH, "5d6f0ba2-1234-4cd5-9a7b-6c3e7f2b1a01",
                            -4.0, AttributeModifier.Operation.ADDITION));

    /** Ледяные оковы: полная неподвижность (замедление + запрет прыжка). */
    public static final RegistryObject<MobEffect> FROZEN = EFFECTS.register("frozen", () ->
            new MobEffect(MobEffectCategory.HARMFUL, 0x9FDFFF) { }
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, "5d6f0ba2-1234-4cd5-9a7b-6c3e7f2b1a02",
                            -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, "5d6f0ba2-1234-4cd5-9a7b-6c3e7f2b1a03",
                            -0.9, AttributeModifier.Operation.MULTIPLY_TOTAL));

    /** Искажение: клиент инвертирует WASD (см. ClientEvents). Накладывается Болотным Шёпотом. */
    public static final RegistryObject<MobEffect> INVERTED = EFFECTS.register("inverted", () ->
            new MobEffect(MobEffectCategory.HARMFUL, 0x4A5D3A) { });

    /** Оглушение: сильное замедление и слабость. Применяется Эхо-трезубцем. */
    public static final RegistryObject<MobEffect> STUN = EFFECTS.register("stun", () ->
            new MobEffect(MobEffectCategory.HARMFUL, 0xFFE066) { }
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, "5d6f0ba2-1234-4cd5-9a7b-6c3e7f2b1a04",
                            -0.95, AttributeModifier.Operation.MULTIPLY_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, "5d6f0ba2-1234-4cd5-9a7b-6c3e7f2b1a05",
                            -0.6, AttributeModifier.Operation.MULTIPLY_TOTAL));
}

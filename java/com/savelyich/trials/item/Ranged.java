package com.savelyich.trials.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.savelyich.trials.entity.BaseBoss;
import com.savelyich.trials.registry.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Дальнобойное, метательное и «особое» оружие. */
public final class Ranged {
    private Ranged() {}

    private static void desc(String id, List<Component> tips) {
        tips.add(Component.translatable("item.savelyich_trials." + id + ".desc").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    /** Ближайшая живая цель на луче; стены блокируют. */
    public static LivingEntity rayTarget(Level level, LivingEntity shooter, Vec3 dir, double range) {
        Vec3 from = shooter.getEyePosition();
        Vec3 to = from.add(dir.scale(range));
        HitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        double maxDist = block.getType() == HitResult.Type.MISS ? range * range : block.getLocation().distanceToSqr(from);
        LivingEntity best = null;
        double bestD = maxDist;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(from, to).inflate(1.0), x -> x != shooter && x.isAlive())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(from, to);
            if (hit.isPresent()) {
                double d = hit.get().distanceToSqr(from);
                if (d < bestD) { bestD = d; best = e; }
            }
        }
        return best;
    }

    // 3. Призрачный лук -----------------------------------------------------------------
    public static class GhostBow extends BowItem {
        public GhostBow() { super(new Properties().durability(520)); }

        @Override
        public Predicate<ItemStack> getAllSupportedProjectiles() { return s -> s.is(ModItems.SOUL_ESSENCE.get()); }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) { desc("ghost_bow", t); }
    }

    /** Сущность-снаряд, оставляемая лучником: душа. */
    public static class SoulEssence extends Item {
        public SoulEssence() { super(new Properties()); }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) { desc("soul_essence", t); }
    }

    // 8. Эхо-трезубец (волна — в WeaponEvents) ----------------------------------------------
    public static class EchoTrident extends TridentItem {
        public EchoTrident() { super(new Properties().durability(300)); }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) { desc("echo_trident", t); }
    }

    // 10. Пружинный арбалет (веер — в WeaponEvents) --------------------------------------------
    public static class SpringCrossbow extends CrossbowItem {
        public SpringCrossbow() { super(new Properties().durability(480)); }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) { desc("spring_crossbow", t); }
    }

    // 11. Посох Пыли ------------------------------------------------------------------
    public static class DustStaff extends Item {
        public DustStaff() { super(new Properties().durability(160)); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                LivingEntity t = rayTarget(level, player, player.getLookAngle(), 16);
                level.playSound(null, player.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1f, 0.6f);
                if (t != null && !isBoss(t) && !(t instanceof Player)) {
                    com.savelyich.trials.events.WeaponEvents.makeSand(t, 100);
                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                    player.getCooldowns().addCooldown(this, 60);
                } else if (t != null && isBoss(t)) {
                    player.displayClientMessage(Component.literal("Босс не поддаётся пыли."), true);
                    player.getCooldowns().addCooldown(this, 20);
                } else {
                    player.getCooldowns().addCooldown(this, 10);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        public static boolean isBoss(LivingEntity e) {
            return e instanceof BaseBoss || e.getType().is(Tags.EntityTypes.BOSSES);
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) { desc("dust_staff", t); }
    }

    // 14. Кинетический щит-лезвие -----------------------------------------------------------
    public static class KineticBladeShield extends ShieldItem {
        public static final int MAX = 30;

        public KineticBladeShield() { super(new Properties().durability(520)); }

        public static int energy(ItemStack s) { return s.getOrCreateTag().getInt("Kinetic"); }

        @Override
        public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
            if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
            return ImmutableMultimap.of(
                    Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon", 4.0, AttributeModifier.Operation.ADDITION),
                    Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon", -2.4, AttributeModifier.Operation.ADDITION));
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            int e = energy(stack);
            if (e > 0 && attacker instanceof Player p && !attacker.level().isClientSide) {
                float bonus = e * 0.6f;
                target.hurt(attacker.level().damageSources().playerAttack(p), bonus);
                Vec3 d = target.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
                target.knockback(0.5 + e * 0.05, -d.x, -d.z);
                stack.getOrCreateTag().putInt("Kinetic", 0);
                if (attacker.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 30, 0.5, 0.5, 0.5, 0.3);
                    sl.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.8f);
                }
            }
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            desc("kinetic_blade_shield", t);
            t.add(Component.literal("Заряд: " + energy(s) + "/" + MAX).withStyle(net.minecraft.ChatFormatting.AQUA));
        }
    }

    // 15. Проклятый тотем-дробовик -----------------------------------------------------------
    public static class CursedTotemShotgun extends Item {
        public CursedTotemShotgun() { super(new Properties().durability(300)); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
            if (!level.isClientSide && level instanceof ServerLevel sl) {
                Vec3 look = player.getLookAngle();
                var rnd = player.getRandom();
                for (int i = 0; i < 8; i++) {
                    Vec3 dir = look.add((rnd.nextDouble() - 0.5) * 0.28, (rnd.nextDouble() - 0.5) * 0.2,
                            (rnd.nextDouble() - 0.5) * 0.28).normalize();
                    LivingEntity t = rayTarget(level, player, dir, 14);
                    if (t != null) {
                        t.hurt(level.damageSources().playerAttack(player), 3.0f);
                        t.invulnerableTime = 0;
                    }
                    for (int k = 1; k <= 10; k += 3) {
                        Vec3 p = player.getEyePosition().add(dir.scale(k));
                        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BONE)),
                                p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
                    }
                }
                sl.playSound(null, player.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 1.2f, 0.5f);
                sl.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 1.9f);
                // цена — удача
                MobEffectInstance cur = player.getEffect(MobEffects.UNLUCK);
                int amp = cur == null ? 0 : Math.min(cur.getAmplifier() + 1, 4);
                player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 20 * 45, amp));
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                player.knockback(0.35, look.x, look.z);
                player.hurtMarked = true;
            }
            player.getCooldowns().addCooldown(this, 32);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) { desc("cursed_totem_shotgun", t); }
    }
}

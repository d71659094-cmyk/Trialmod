package com.savelyich.trials.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import com.savelyich.trials.registry.ModEffects;

import java.util.UUID;

/** Ближние клинки мода с механиками, которым не нужны глобальные события. */
public final class Weapons {
    private Weapons() {}

    public static boolean inFrontArc(LivingEntity attacker, Entity e, double minDot) {
        Vec3 look = attacker.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 to = e.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
        return look.dot(to) >= minDot;
    }

    // 1. Кристальный клинок -----------------------------------------------------------
    public static class CrystalBlade extends TrialSword {
        public static final int MAX_CHARGE = 5;

        public CrystalBlade() { super("crystal_blade", 900, 7, 1.7f); }

        public static int charge(ItemStack s) { return s.getOrCreateTag().getInt("Charge"); }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            int c = charge(stack) + 1;
            if (c >= MAX_CHARGE && attacker.level() instanceof ServerLevel sl) {
                c = 0;
                sl.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.6f, 0.7f);
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.AMETHYST_SHARD)),
                        target.getX(), target.getY(0.5), target.getZ(), 60, 1.2, 0.8, 1.2, 0.25);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4),
                        x -> x != attacker && x.isAlive())) {
                    e.hurt(attacker instanceof Player p ? sl.damageSources().playerAttack(p) : sl.damageSources().mobAttack(attacker), 9.0f);
                    Vec3 away = e.position().subtract(target.position()).normalize();
                    e.knockback(0.6, -away.x, -away.z);
                }
            }
            stack.getOrCreateTag().putInt("Charge", c);
            return super.hurtEnemy(stack, target, attacker);
        }
    }

    // 2. Кувалда Голема ---------------------------------------------------------------
    public static class GolemSledge extends TrialSword {
        public GolemSledge() { super("golem_sledge", 1400, 13, 0.7f); }

        public static void smash(Level level, BlockPos center, int radius, LivingEntity user, ItemStack stack) {
            if (level.isClientSide) return;
            int broken = 0;
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 1, radius))) {
                if (p.distSqr(center) > radius * radius + 1) continue;
                BlockState st = level.getBlockState(p);
                float h = st.getDestroySpeed(level, p);
                if (st.isAir() || h < 0 || h > 3.0f || level.getBlockEntity(p) != null) continue;
                level.destroyBlock(p.immutable(), true, user);
                broken++;
            }
            if (broken > 0) stack.hurtAndBreak(Math.min(broken, 6), user, u -> u.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            level.playSound(null, center, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.4f);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            Vec3 dir = target.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
            target.knockback(2.2, -dir.x, -dir.z);
            target.hurtMarked = true;
            smash(attacker.level(), target.blockPosition(), 2, attacker, stack);
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Player p = ctx.getPlayer();
            if (p == null || p.getCooldowns().isOnCooldown(this)) return InteractionResult.PASS;
            smash(ctx.getLevel(), ctx.getClickedPos(), 2, p, ctx.getItemInHand());
            p.getCooldowns().addCooldown(this, 30);
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
    }

    // 4. Копьё Стража -----------------------------------------------------------------
    public static class GuardianSpear extends TrialSword {
        private static final UUID REACH_UUID = UUID.fromString("c1a3a5b0-6f3e-4c8e-9a41-7d2f8e0a1b11");

        public GuardianSpear() { super("guardian_spear", 1100, 6, 1.3f); }

        @Override
        protected Multimap<Attribute, AttributeModifier> extraModifiers() {
            return ImmutableMultimap.of(ForgeMod.ENTITY_REACH.get(),
                    new AttributeModifier(REACH_UUID, "Guardian reach", 2.5, AttributeModifier.Operation.ADDITION));
        }
    }

    // 5. Пламенный серп ---------------------------------------------------------------
    public static class FlameSickle extends TrialSword {
        public FlameSickle() { super("flame_sickle", 600, 5, 1.8f); }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            Level level = attacker.level();
            if (!level.isClientSide) {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                        attacker.getBoundingBox().inflate(3.2, 1.0, 3.2), x -> x != attacker && x.isAlive())) {
                    if (inFrontArc(attacker, e, 0.0)) e.setSecondsOnFire(5);
                }
                target.setSecondsOnFire(5);
                if (level instanceof ServerLevel sl) {
                    Vec3 l = attacker.getLookAngle();
                    for (int i = -5; i <= 5; i++) {
                        double a = i * 0.2;
                        double x = l.x * Math.cos(a) - l.z * Math.sin(a);
                        double z = l.x * Math.sin(a) + l.z * Math.cos(a);
                        sl.sendParticles(ParticleTypes.FLAME, attacker.getX() + x * 2.2, attacker.getY(0.6),
                                attacker.getZ() + z * 2.2, 2, 0.05, 0.05, 0.05, 0.01);
                    }
                }
                if (level.isRainingAt(attacker.blockPosition())) {
                    stack.hurtAndBreak(3, attacker, u -> u.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                }
            }
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
            if (!level.isClientSide && selected && holder instanceof Player p && holder.tickCount % 40 == 0
                    && level.isRainingAt(holder.blockPosition())) {
                stack.hurtAndBreak(1, p, u -> u.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
        }
    }

    // 6. Ледяной молот ----------------------------------------------------------------
    public static class FrostHammer extends TrialSword {
        public FrostHammer() { super("frost_hammer", 1000, 10, 0.8f); }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            if (!attacker.level().isClientSide) {
                target.addEffect(new MobEffectInstance(ModEffects.FROZEN.get(), 40, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6));
                target.setTicksFrozen(target.getTicksRequiredToFreeze() + 40);
                if (attacker.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5), target.getZ(),
                            35, 0.5, 0.7, 0.5, 0.05);
                    sl.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 0.6f);
                }
            }
            return super.hurtEnemy(stack, target, attacker);
        }
    }

    // 7. Кинжал Тени (механика в WeaponEvents) --------------------------------------------
    public static class ShadowDagger extends TrialSword {
        public ShadowDagger() { super("shadow_dagger", 700, 3, 2.6f); }
    }

    // 9. Кровавый топор (механика в WeaponEvents) -----------------------------------------
    public static class BloodAxe extends TrialSword {
        public BloodAxe() { super("blood_axe", 950, 9, 1.0f); }
    }

    // 12. Громовая булава (механика в WeaponEvents) ---------------------------------------
    public static class ThunderMace extends TrialSword {
        public ThunderMace() { super("thunder_mace", 1100, 9, 1.1f); }
    }

    // 13. Коса Жнеца ----------------------------------------------------------------------
    public static class ReaperScythe extends TrialSword {
        public ReaperScythe() { super("reaper_scythe", 1000, 7, 1.2f); }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            Level level = attacker.level();
            if (!level.isClientSide && attacker instanceof Player p) {
                AABB box = attacker.getBoundingBox().inflate(3.6, 0.8, 3.6);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != attacker && x != target && x.isAlive())) {
                    if (inFrontArc(attacker, e, -0.35)) {
                        e.hurt(level.damageSources().playerAttack(p), 5.0f);
                        Vec3 d = e.position().subtract(attacker.position()).normalize();
                        e.knockback(0.4, -d.x, -d.z);
                    }
                }
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, attacker.getX() + attacker.getLookAngle().x * 1.8,
                            attacker.getY(0.5), attacker.getZ() + attacker.getLookAngle().z * 1.8, 3, 1.0, 0.1, 1.0, 0);
                }
            }
            return super.hurtEnemy(stack, target, attacker);
        }
    }
}

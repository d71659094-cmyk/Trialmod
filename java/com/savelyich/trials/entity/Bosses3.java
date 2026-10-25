package com.savelyich.trials.entity;

import com.savelyich.trials.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Боссы 11–15: Стеклянный Наблюдатель, Болотный Шёпот, Кузнец-Призрак, Пожиратель Света, Двойник Луны. */
public final class Bosses3 {
    private Bosses3() {}

    // =============================== 11. Стеклянный Наблюдатель ===============================
    public static class GlassWatcher extends BaseBoss {
        private int flash = 0;
        private int nextFlashIn = 60;
        private Vec3 goal = null;
        private int goalTimer = 0;
        private int shotCooldown = 40;

        public GlassWatcher(EntityType<? extends Monster> t, Level l) { super(t, l, 10); this.setNoGravity(true); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(100, 5, 0.3).add(Attributes.FLYING_SPEED, 0.5); }

        @Override protected void registerAttackGoals() { /* летает и стреляет осколками */ }
        @Override public boolean causeFallDamage(float f, float m, DamageSource s) { return false; }

        public boolean isFlashing() { return flash > 0; }

        private boolean overWater() {
            BlockPos p = blockPosition();
            for (int i = 0; i < 14; i++) {
                BlockPos q = p.below(i);
                if (level().getFluidState(q).is(FluidTags.WATER)) return true;
                if (!level().getBlockState(q).isAir()) return false;
            }
            return false;
        }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            return flash > 0 ? amount * 1.3f : 0f;
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (!(level() instanceof ServerLevel sl)) return;
            setNoGravity(true);
            // блик: на солнце (днём под открытым небом) или над водой
            boolean sunlit = level().isDay() && !level().isRaining() && level().canSeeSky(blockPosition());
            if (flash > 0) flash--;
            else if (--nextFlashIn <= 0) {
                nextFlashIn = 90;
                if (sunlit || overWater()) {
                    flash = 30;
                    sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2f, 1.8f);
                }
            }
            double speed = getDeltaMovement().length();
            setInvisible(flash <= 0 && speed < 0.22);
            if (flash > 0 && tickCount % 2 == 0)
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.5), getZ(), 6, 0.5, 0.5, 0.5, 0.02);

            // патруль: чаще всего над бассейнами (локально ±6, 0)
            if (hasHome) {
                if (goal == null || --goalTimer <= 0 || position().distanceTo(goal) < 1.0) {
                    goalTimer = 80;
                    var r = getRandom();
                    if (r.nextFloat() < 0.5f) {
                        int sx = r.nextBoolean() ? 6 : -6;
                        goal = new Vec3(home.getX() + 0.5 + sx, home.getY() + 5.5, home.getZ() + 0.5);
                    } else {
                        goal = new Vec3(home.getX() + 0.5 + (r.nextDouble() - 0.5) * 18, home.getY() + 5 + r.nextDouble() * 3,
                                home.getZ() + 0.5 + (r.nextDouble() - 0.5) * 18);
                    }
                }
                Vec3 to = goal.subtract(position());
                Vec3 mv = to.length() > 0.1 ? to.normalize().scale(Math.min(0.28, to.length())) : Vec3.ZERO;
                setDeltaMovement(getDeltaMovement().scale(0.5).add(mv.scale(0.5)));
            }
            if (shotCooldown > 0) shotCooldown--;
            Player t = nearestPlayer(players);
            if (t != null && shotCooldown == 0 && hasLineOfSight(t) && distanceToSqr(t) < 22 * 22) {
                shotCooldown = 55;
                Vec3 a = getEyePosition(), d = t.getEyePosition().subtract(a);
                for (double k = 0.05; k < 1.0; k += 0.07) {
                    Vec3 v = a.add(d.scale(k));
                    sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GLASS_PANE)), v.x, v.y, v.z, 1, 0, 0, 0, 0);
                }
                t.hurt(damageSources().mobAttack(this), 5.0f);
                sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1f, 1.6f);
            }
        }
    }

    // =============================== 12. Болотный Шёпот ===============================
    public static class BogWhisper extends BaseBoss {
        public BogWhisper(EntityType<? extends Monster> t, Level l) { super(t, l, 11); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(100, 5, 0.27); }

        @Override public boolean fireImmune() { return false; }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            boolean elemental = src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypeTags.IS_EXPLOSION)
                    || src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.LIGHTNING_BOLT);
            if (elemental) return amount;
            return isOnFire() ? amount : amount * 0.05f;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean r = super.doHurtTarget(target);
            if (r && target instanceof Player p) p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
            return r;
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (!(level() instanceof ServerLevel sl)) return;
            // свет ранит: рядом с яркими источниками (костры, факелы) он горит
            if (tickCount % 20 == 0 && level().getBrightness(LightLayer.BLOCK, blockPosition()) >= 12) {
                hurt(damageSources().magic(), 3.0f);
                sl.sendParticles(ParticleTypes.FLAME, getX(), getY(1.0), getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            }
            if (tickCount % 160 == 0) {
                for (Player p : players) p.addEffect(new MobEffectInstance(ModEffects.INVERTED.get(), 70, 0));
                if (!players.isEmpty()) sl.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 2f, 0.6f);
            }
            if (tickCount % 3 == 0) sl.sendParticles(ParticleTypes.SOUL, getX(), getY(1.0), getZ(), 2, 0.3, 0.7, 0.3, 0.01);
        }
    }

    // =============================== 13. Кузнец-Призрак ===============================
    public static class GhostSmith extends BaseBoss {
        private boolean inSync = true;
        private int staggered = 0;

        public GhostSmith(EntityType<? extends Monster> t, Level l) { super(t, l, 12); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(130, 6, 0.30); }

        private double copiedDamage() {
            ItemStack s = getMainHandItem();
            if (s.isEmpty()) return 3;
            double sum = 1.0;
            for (AttributeModifier m : s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE))
                if (m.getOperation() == AttributeModifier.Operation.ADDITION) sum += m.getAmount();
            return Math.max(3, Math.min(14, sum));
        }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            return inSync ? amount * 0.8f : amount * 1.5f;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            if (staggered > 0) return false;
            if (!inSync && getRandom().nextFloat() < 0.65f) { // копия «не та» — промах и заминка
                staggered = 30;
                if (level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.CLOUD, getX(), getY(1.0), getZ(), 12, 0.4, 0.5, 0.4, 0.05);
                return false;
            }
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            return target.hurt(damageSources().mobAttack(this), (float) copiedDamage());
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (staggered > 0) staggered--;
            Player t = nearestPlayer(players);
            if (t == null) return;
            ItemStack held = t.getMainHandItem();
            if (tickCount % 60 == 0 && !held.isEmpty()) { // копия обновляется раз в 3 секунды
                setItemSlot(EquipmentSlot.MAINHAND, held.copyWithCount(1));
            }
            inSync = ItemStack.isSameItem(held, getMainHandItem());
            if (tickCount % 8 == 0 && level() instanceof ServerLevel sl)
                sl.sendParticles(inSync ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE, getX(), getY(1.0), getZ(), 3, 0.3, 0.6, 0.3, 0.02);
        }
    }

    // =============================== 14. Пожиратель Света ===============================
    public static class LightDevourer extends BaseBoss {
        public LightDevourer(EntityType<? extends Monster> t, Level l) { super(t, l, 13); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(160, 8, 0.28); }

        private int lightHere() { return level().getBrightness(LightLayer.BLOCK, blockPosition().above()); }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            int l = lightHere();
            if (l <= 4) return amount * 0.2f;
            if (l <= 8) return amount * 0.6f;
            return amount * 1.5f;
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (!(level() instanceof ServerLevel sl) || players.isEmpty()) return;
            int l = lightHere();
            if (tickCount % 10 == 0) {
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(l <= 4 ? 0.38 : l <= 8 ? 0.28 : 0.18);
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(l <= 4 ? 10 : l <= 8 ? 7 : 4);
            }
            if (l <= 4 && tickCount % 20 == 0) heal(2.0f);
            // гасит свет вокруг
            if (tickCount % 12 == 0) {
                int removed = 0;
                for (BlockPos p : BlockPos.betweenClosed(blockPosition().offset(-8, -2, -8), blockPosition().offset(8, 5, 8))) {
                    if (removed >= 3) break;
                    BlockState s = level().getBlockState(p);
                    if (s.isAir() || s.is(Blocks.LAVA) || s.is(Blocks.BEDROCK)) continue;
                    if (s.getLightEmission(level(), p) > 0 && !s.is(Blocks.FIRE) && !s.is(Blocks.SOUL_FIRE)
                            && !s.is(net.minecraft.tags.BlockTags.CANDLES) && !s.is(Blocks.LIGHT)) {
                        level().removeBlock(p.immutable(), false);
                        sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.1, 0.1, 0.1, 0.02);
                        sl.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.6f, 1.0f);
                        removed++;
                    }
                }
            }
            if (tickCount % 40 == 0) for (Player p : players)
                if (p.distanceToSqr(this) < 16 * 16) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false));
            if (tickCount % 4 == 0)
                sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(1.0), getZ(), 3, 0.4, 0.8, 0.4, 0.01);
        }
    }

    // =============================== 15. Двойник Луны ===============================
    public static class MoonDouble extends BaseBoss {
        private int strikeIn = 0, recover = 0, cooldown = 0;
        private Vec3 strikeCenter = Vec3.ZERO;

        public MoonDouble(EntityType<? extends Monster> t, Level l) { super(t, l, 14); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(170, 5, 0.26); }

        /** Вызывается из события взмаха игрока. Зеркальный удар падает позади игрока относительно босса. */
        public void onSwing(Player p) {
            if (strikeIn > 0 || recover > 0 || cooldown > 0 || !inArena(p)) return;
            Vec3 d = p.getLookAngle().multiply(1, 0, 1);
            if (d.lengthSqr() < 1.0e-4) return;
            d = d.normalize();
            strikeCenter = position().subtract(d.scale(2.4));
            strikeIn = 12;
            cooldown = 30;
        }

        public boolean isRecovering() { return recover > 0; }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            return recover > 0 ? amount * 1.5f : amount * 0.25f;
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (!(level() instanceof ServerLevel sl)) return;
            if (cooldown > 0) cooldown--;
            if (recover > 0) {
                recover--;
                if (tickCount % 3 == 0) sl.sendParticles(ParticleTypes.CRIT, getX(), getY(1.0), getZ(), 4, 0.4, 0.6, 0.4, 0.05);
            }
            if (strikeIn > 0) {
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    sl.sendParticles(ParticleTypes.END_ROD, strikeCenter.x + Math.cos(a) * 2.2, strikeCenter.y + 0.2,
                            strikeCenter.z + Math.sin(a) * 2.2, 1, 0, 0, 0, 0);
                }
                if (--strikeIn == 0) {
                    sl.playSound(null, BlockPos.containing(strikeCenter), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.5f, 0.5f);
                    sl.sendParticles(ParticleTypes.EXPLOSION, strikeCenter.x, strikeCenter.y + 0.5, strikeCenter.z, 1, 0, 0, 0, 0);
                    for (Player p : players) {
                        if (Math.hypot(p.getX() - strikeCenter.x, p.getZ() - strikeCenter.z) <= 2.3 && Math.abs(p.getY() - strikeCenter.y) < 2.5) {
                            p.hurt(damageSources().mobAttack(this), 9.0f);
                            Vec3 k = p.position().subtract(strikeCenter).normalize();
                            p.knockback(0.8, -k.x, -k.z);
                        }
                    }
                    recover = 35; // окно после удара (мимо или нет)
                }
            }
        }
    }
}

package com.savelyich.trials.entity;

import com.savelyich.trials.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Боссы 6–10: Песчаный Пловец, Ледяной Пилигрим, Ржавый Механик, Сновидец, Корень-Странник. */
public final class Bosses2 {
    private Bosses2() {}

    // =============================== 6. Песчаный Пловец ===============================
    public static class SandSwimmer extends BaseBoss {
        private boolean submerged = true;
        private int timer = 0;
        private int stuck = 0;
        private int hitCooldown = 0;

        public SandSwimmer(EntityType<? extends Monster> t, Level l) { super(t, l, 5); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(115, 7, 0.30); }

        @Override protected void registerAttackGoals() { /* свой ИИ */ }

        @Override
        protected float filterDamage(DamageSource src, float amount) { return submerged ? 0f : amount; }

        private boolean isSand(double x, double z) {
            return level().getBlockState(BlockPos.containing(x, home.getY(), z)).is(BlockTags.SAND);
        }

        private void dive() {
            submerged = true;
            stuck = 0;
            noPhysics = true;
            setNoGravity(true);
            setInvisible(true);
            setDeltaMovement(Vec3.ZERO);
            setPos(getX(), home.getY() - 1.6, getZ());
        }

        private void surface(double x, double z) {
            submerged = false;
            timer = 70;
            noPhysics = false;
            setNoGravity(false);
            setInvisible(false);
            setPos(x, home.getY() + 1.0, z);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                        x, home.getY() + 1.2, z, 45, 0.6, 0.4, 0.6, 0.15);
                sl.playSound(null, BlockPos.containing(x, home.getY(), z), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
            }
        }

        @Override
        public void tick() {
            if (submerged) { noPhysics = true; }
            super.tick();
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (hitCooldown > 0) hitCooldown--;
            Player t = nearestPlayer(players);
            if (submerged) {
                if (getY() > home.getY() - 1.0) setPos(getX(), home.getY() - 1.6, getZ());
                if (t == null) return;
                Vec3 d = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ());
                double dist = d.length();
                boolean playerOnSand = isSand(t.getX(), t.getZ()) && t.onGround();
                if (dist > 0.9) {
                    Vec3 step = d.normalize().scale(0.34);
                    double nx = getX() + step.x, nz = getZ() + step.z;
                    if (isSand(nx, nz)) { setPos(nx, home.getY() - 1.6, nz); stuck = 0; }
                    else stuck++;
                }
                if (tickCount % 3 == 0 && level() instanceof ServerLevel sl) {
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                            getX(), home.getY() + 1.05, getZ(), 3, 0.2, 0.05, 0.2, 0.02);
                }
                if (dist <= 1.4 && playerOnSand && hitCooldown == 0) {
                    surface(t.getX(), t.getZ());
                    t.hurt(damageSources().mobAttack(this), 6.0f);
                    t.setDeltaMovement(t.getDeltaMovement().add(0, 0.9, 0));
                    t.hurtMarked = true;
                    hitCooldown = 30;
                } else if (stuck > 35) { // игрок на камне — Пловец вынужден всплыть у края
                    surface(getX(), getZ());
                }
            } else {
                timer--;
                if (t != null) {
                    getNavigation().moveTo(t, 1.1);
                    if (hitCooldown == 0 && distanceToSqr(t) < 2.8 * 2.8) {
                        t.hurt(damageSources().mobAttack(this), 7.0f);
                        hitCooldown = 25;
                    }
                }
                if (timer <= 0 && isSand(getX(), getZ())) dive();
            }
        }

        @Override
        public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putBoolean("Submerged", submerged);
        }

        @Override
        public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            submerged = tag.getBoolean("Submerged");
            if (submerged) { setInvisible(true); setNoGravity(true); noPhysics = true; }
        }
    }

    // =============================== 7. Ледяной Пилигрим ===============================
    public static class IcePilgrim extends BaseBoss {
        private long auraOffUntil = 0;

        public IcePilgrim(EntityType<? extends Monster> t, Level l) { super(t, l, 6); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(130, 7, 0.22); }

        @Override public boolean fireImmune() { return false; }
        @Override public boolean canFreeze() { return false; }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            boolean fire = src.is(DamageTypeTags.IS_FIRE);
            if (fire || isOnFire()) auraOffUntil = level().getGameTime() + 100;
            return fire ? amount * 1.5f : amount;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean r = super.doHurtTarget(target);
            if (r && target instanceof LivingEntity le) le.setTicksFrozen(le.getTicksRequiredToFreeze() + 60);
            return r;
        }

        @Override
        protected void tickBoss(List<Player> players) {
            long now = level().getGameTime();
            if (isOnFire()) auraOffUntil = Math.max(auraOffUntil, now + 60);
            boolean aura = now >= auraOffUntil;
            Player t = nearestPlayer(players);
            if (tickCount % 10 == 0) {
                double speed = 0.20;
                if (t != null && aura) {
                    double d = t.distanceTo(this);
                    speed = d < 4 ? 0.42 : d < 8 ? 0.32 : 0.22;
                }
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(aura ? speed : 0.2);
                if (aura) {
                    for (Player p : players) {
                        double d = p.distanceTo(this);
                        if (d > 10) continue;
                        int amp = d < 3 ? 3 : d < 6 ? 2 : 1;
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, amp, true, false));
                        p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() - 1, p.getTicksFrozen() + 6));
                    }
                }
            }
            if (level() instanceof ServerLevel sl && tickCount % 4 == 0) {
                sl.sendParticles(aura ? ParticleTypes.SNOWFLAKE : ParticleTypes.SMOKE, getX(), getY(0.6), getZ(),
                        aura ? 12 : 3, aura ? 3.5 : 0.5, 0.8, aura ? 3.5 : 0.5, 0.01);
            }
        }
    }

    // =============================== 8. Ржавый Механик ===============================
    public static class RustyMechanic extends BaseBoss {
        private static final int MAX_PLATES = 6;
        private int plates = MAX_PLATES;
        private long lastPlateHit = -100, lastDamaged = -1000, noRepairUntil = 0;
        private int stun = 0;
        private final List<UUID> minions = new ArrayList<>();

        public RustyMechanic(EntityType<? extends Monster> t, Level l) { super(t, l, 7); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(150, 8, 0.25); }

        public int plates() { return plates; }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            long now = level().getGameTime();
            lastDamaged = now;
            if (stun > 0) return amount * 1.5f;
            if (plates > 0) {
                if (now - lastPlateHit < 8) return 0f;
                lastPlateHit = now;
                plates -= amount >= 8 ? 2 : 1;
                if (plates < 0) plates = 0;
                level().playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8f, 1.4f);
                if (level() instanceof ServerLevel sl)
                    sl.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_INGOT)),
                            getX(), getY(1.0), getZ(), 14, 0.4, 0.5, 0.4, 0.15);
                return amount * 0.2f;
            }
            return amount;
        }

        @Override
        public void thunderHit(ServerLevel level, LightningBolt bolt) {
            plates = 0;
            stun = 100;
            noRepairUntil = level.getGameTime() + 240;
            setNoAi(true);
            hurt(damageSources().lightningBolt(), 15f);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(1.0), getZ(), 60, 0.6, 1.0, 0.6, 0.4);
        }

        private boolean ironNearby() {
            for (BlockPos p : BlockPos.betweenClosed(blockPosition().offset(-5, -1, -5), blockPosition().offset(5, 2, 5)))
                if (level().getBlockState(p).is(Blocks.IRON_BLOCK) || level().getBlockState(p).is(Blocks.ANVIL)) return true;
            return false;
        }

        @Override
        protected void tickBoss(List<Player> players) {
            long now = level().getGameTime();
            if (stun > 0 && --stun == 0) setNoAi(false);
            if (stun == 0 && plates < MAX_PLATES && now >= noRepairUntil && now - lastDamaged > 60
                    && tickCount % (ironNearby() ? 20 : 50) == 0) {
                plates++;
                level().playSound(null, blockPosition(), SoundEvents.ANVIL_USE, SoundSource.HOSTILE, 0.7f, 1.2f);
            }
            if (!(level() instanceof ServerLevel sl)) return;
            if (tickCount % 240 == 0 && !players.isEmpty()) {
                minions.removeIf(id -> { Entity e = sl.getEntity(id); return e == null || !e.isAlive(); });
                if (minions.size() < 2) {
                    Zombie z = EntityType.ZOMBIE.create(sl);
                    if (z != null) {
                        z.moveTo(getX() + 1.5, getY(), getZ() + 1.5);
                        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                        z.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                        z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                        z.setPersistenceRequired();
                        z.setTarget(nearestPlayer(players));
                        sl.addFreshEntity(z);
                        minions.add(z.getUUID());
                    }
                }
            }
            if (tickCount % 6 == 0 && plates > 0)
                sl.sendParticles(ParticleTypes.CRIT, getX(), getY(1.0), getZ(), 1, 0.4, 0.6, 0.4, 0.0);
        }

        @Override
        public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putInt("Plates", plates);
        }

        @Override
        public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            if (tag.contains("Plates")) plates = tag.getInt("Plates");
        }
    }

    // =============================== 9. Сновидец ===============================
    public static class Dreamer extends BaseBoss {
        private final List<UUID> clones = new ArrayList<>();
        private long lastShuffle = 0;

        public Dreamer(EntityType<? extends Monster> t, Level l) { super(t, l, 8); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(120, 7, 0.30); }

        private void shuffle(ServerLevel sl) {
            for (UUID id : clones) { Entity e = sl.getEntity(id); if (e != null) e.discard(); }
            clones.clear();
            lastShuffle = sl.getGameTime();
            var rnd = getRandom();
            for (int i = 0; i < 4; i++) {
                DreamerClone c = DreamerClone.TYPE_SUPPLIER.get().create(sl);
                if (c == null) continue;
                double a = rnd.nextDouble() * Math.PI * 2, r = 3 + rnd.nextDouble() * 5;
                c.moveTo(getX() + Math.cos(a) * r, getY(), getZ() + Math.sin(a) * r);
                sl.addFreshEntity(c);
                clones.add(c.getUUID());
            }
            if (hasHome) { // настоящий тоже перемещается, чтобы его нельзя было отследить
                double a = rnd.nextDouble() * Math.PI * 2, r = 3 + rnd.nextDouble() * 7;
                teleportTo(home.getX() + 0.5 + Math.cos(a) * r, home.getY() + 1, home.getZ() + 0.5 + Math.sin(a) * r);
            }
            sl.sendParticles(ParticleTypes.PORTAL, getX(), getY(1), getZ(), 40, 0.6, 1, 0.6, 0.4);
        }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            if (level() instanceof ServerLevel sl && sl.getGameTime() - lastShuffle > 60) {
                // после удара — новая тасовка копий (но урон проходит)
                level().getServer().execute(() -> { if (isAlive()) shuffle(sl); });
            }
            return amount;
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (!(level() instanceof ServerLevel sl) || players.isEmpty()) return;
            if (tickCount % 160 == 0) shuffle(sl);
        }

        @Override
        public void die(DamageSource src) {
            super.die(src);
            if (level() instanceof ServerLevel sl) for (UUID id : clones) { Entity e = sl.getEntity(id); if (e != null) e.discard(); }
        }
    }

    /** Иллюзорная копия. Тени не отбрасывает (радиус тени в рендерере = 0), удар по ней оглушает бьющего. */
    public static class DreamerClone extends PathfinderMob {
        public static java.util.function.Supplier<EntityType<DreamerClone>> TYPE_SUPPLIER;

        public DreamerClone(EntityType<? extends PathfinderMob> t, Level l) { super(t, l); this.xpReward = 0; }

        public static AttributeSupplier.Builder attrs() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 2).add(Attributes.MOVEMENT_SPEED, 0.30)
                    .add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.FOLLOW_RANGE, 30);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        @Override
        public boolean hurt(DamageSource src, float amount) {
            if (level().isClientSide) return false;
            if (src.getEntity() instanceof LivingEntity att) {
                att.addEffect(new MobEffectInstance(ModEffects.STUN.get(), 60, 0));
                att.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                att.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 0));
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SMOKE, getX(), getY(0.5), getZ(), 25, 0.3, 0.5, 0.3, 0.05);
                    sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.5f);
                }
                discard();
                return true;
            }
            return false;
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide && tickCount > 400) discard();
        }

        @Override public boolean removeWhenFarAway(double d) { return true; }
    }

    // =============================== 10. Корень-Странник ===============================
    public static class RootWanderer extends BaseBoss {
        private int phase = 0; // 0 — ходит, 1 — вырывает корни (уязвим), 2 — пауза после удара
        private int phaseTimer = 70;

        public RootWanderer(EntityType<? extends Monster> t, Level l) { super(t, l, 9); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(140, 8, 0.22); }

        public boolean isExposed() { return phase == 1; }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            return phase == 1 ? amount * 1.5f : 0f;
        }

        private static boolean isDirtLike(BlockState s) {
            return s.is(BlockTags.DIRT) || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.MUD) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL);
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (players.isEmpty()) { phase = 0; phaseTimer = 70; setNoAi(false); return; }
            if (!(level() instanceof ServerLevel sl)) return;
            phaseTimer--;
            switch (phase) {
                case 0 -> {
                    if (phaseTimer <= 0) { phase = 1; phaseTimer = 22; setNoAi(true);
                        sl.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2f, 0.5f); }
                }
                case 1 -> {
                    if (tickCount % 2 == 0) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
                            getX(), getY(), getZ(), 14, 1.2, 0.2, 1.2, 0.2);
                    if (phaseTimer <= 0) {
                        for (Player p : players) {
                            if (p.distanceToSqr(this) > 15 * 15) continue;
                            BlockPos below = p.blockPosition().below();
                            if (!isDirtLike(level().getBlockState(below))) continue;
                            p.hurt(damageSources().mobAttack(this), 9.0f);
                            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()),
                                    p.getX(), p.getY() + 0.5, p.getZ(), 40, 0.4, 0.8, 0.4, 0.2);
                        }
                        sl.playSound(null, blockPosition(), SoundEvents.ROOTS_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
                        phase = 2; phaseTimer = 18;
                    }
                }
                default -> {
                    if (phaseTimer <= 0) { phase = 0; phaseTimer = 80; setNoAi(false); }
                }
            }
        }
    }
}

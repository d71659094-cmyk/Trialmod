package com.savelyich.trials.entity;

import com.savelyich.trials.item.Weapons;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

import java.util.*;

/** Боссы 1–5: Хранитель Эха, Плакальщик, Каменный Пастух, Светлячковый Рой, Грибной Двойник. */
public final class Bosses1 {
    private Bosses1() {}

    // =============================== 1. Хранитель Эха ===============================
    public static class EchoKeeper extends BaseBoss {
        public EchoKeeper(EntityType<? extends Monster> t, Level l) { super(t, l, 0); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(140, 7, 0.27); }

        @Override protected boolean canTargetPlayer(Player p) { return super.canTargetPlayer(p) && !p.isShiftKeyDown(); }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            if (src.getEntity() instanceof Player p) return p.isShiftKeyDown() ? amount * 2.0f : amount * 0.5f;
            return amount;
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (getTarget() instanceof Player t && t.isShiftKeyDown()) setTarget(null);
            if (tickCount % 30 != 0) return;
            for (Player p : players) {
                boolean loud = !p.isShiftKeyDown() && (p.isSprinting() || speedOf(p) > 0.12);
                if (!loud) continue;
                setTarget(p);
                if (distanceToSqr(p) < 22 * 22 && level() instanceof ServerLevel sl) {
                    sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.4f, 1.0f);
                    Vec3 from = getEyePosition();
                    Vec3 dir = p.getEyePosition().subtract(from).normalize();
                    for (int i = 1; i < (int) from.distanceTo(p.getEyePosition()); i += 2) {
                        Vec3 v = from.add(dir.scale(i));
                        sl.sendParticles(ParticleTypes.SONIC_BOOM, v.x, v.y, v.z, 1, 0, 0, 0, 0);
                    }
                    p.hurt(sl.damageSources().sonicBoom(this), 6.0f);
                    p.knockback(0.6, -dir.x, -dir.z);
                }
            }
        }
    }

    // =============================== 2. Плакальщик ===============================
    public static class Wailer extends BaseBoss {
        private int unseen = 0;
        private int cooldown = 0;

        public Wailer(EntityType<? extends Monster> t, Level l) { super(t, l, 1); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(110, 8, 0.30); }

        @Override protected void registerAttackGoals() { /* атакует только телепортом */ }

        private boolean isWatched(Player p) {
            Vec3 eye = p.getEyePosition();
            Vec3 look = p.getViewVector(1.0f);
            Vec3 to = getEyePosition().subtract(eye);
            double dist = to.length();
            Vec3 toN = to.normalize();
            if (dist < 32 && look.dot(toN) > 0.93 && p.hasLineOfSight(this)) return true;
            // отражение: игрок смотрит на воду или стекло, а Плакальщик у него за спиной
            if (look.dot(toN) < -0.45 && dist < 14) {
                var hit = level().clip(new ClipContext(eye, eye.add(look.scale(10)), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.SOURCE_ONLY, p));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos bp = hit.getBlockPos();
                    BlockState st = level().getBlockState(bp);
                    if (st.is(Tags.Blocks.GLASS) || st.is(Tags.Blocks.GLASS_PANES)
                            || level().getFluidState(bp).is(FluidTags.WATER)) return true;
                }
            }
            return false;
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (cooldown > 0) cooldown--;
            Player t = nearestPlayer(players);
            if (t == null) { setNoAi(false); return; }
            if (isWatched(t)) {
                setNoAi(true);
                setDeltaMovement(Vec3.ZERO);
                unseen = 0;
                if (tickCount % 10 == 0 && level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL, getX(), getY(1.0), getZ(), 2, 0.2, 0.4, 0.2, 0.01);
                }
                return;
            }
            setNoAi(false);
            unseen++;
            if (unseen >= 25 && cooldown == 0) {
                Vec3 look = t.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 spot = t.position().subtract(look.scale(1.5));
                if (level().noCollision(this, getBoundingBox().move(spot.subtract(position())))) {
                    teleportTo(spot.x, spot.y, spot.z);
                }
                lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
                t.hurt(damageSources().mobAttack(this), 8.0f);
                level().playSound(null, blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 0.8f, 1.6f);
                cooldown = 50;
                unseen = 0;
            }
        }
    }

    // =============================== 3. Каменный Пастух ===============================
    public static class StoneShepherd extends BaseBoss {
        private final List<UUID> herd = new ArrayList<>();
        private int exposed = 0;

        public StoneShepherd(EntityType<? extends Monster> t, Level l) { super(t, l, 2); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(160, 9, 0.24); }

        public int herdCount() { return herd.size(); }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            return herd.isEmpty() ? amount : 0f; // пока есть стадо — неуязвим
        }

        private void spawnStone(ServerLevel sl, Vec3 at, int slot) {
            HerdStone s = HerdStone.TYPE_SUPPLIER.get().create(sl);
            if (s == null) return;
            s.moveTo(at.x, at.y, at.z);
            s.bind(this.getUUID(), slot);
            sl.addFreshEntity(s);
            herd.add(s.getUUID());
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (!(level() instanceof ServerLevel sl)) return;
            if (tickCount == 5) { // при загрузке подхватываем уже существующее стадо, иначе создаём новое
                for (HerdStone hs : sl.getEntitiesOfClass(HerdStone.class, getBoundingBox().inflate(24),
                        x -> getUUID().equals(x.getOwnerId()))) herd.add(hs.getUUID());
                if (herd.isEmpty() && hasHome) for (int i = 0; i < 8; i++) spawnStone(sl, position().add(0, 1.5, 0), i);
            }
            herd.removeIf(id -> {
                Entity e = sl.getEntity(id);
                return e == null || !e.isAlive();
            });
            if (herd.isEmpty()) {
                exposed++;
                if (hasHome && exposed > 260) { // пополнение стада из куч по углам
                    exposed = 0;
                    int[][] c = {{-9, -9}, {9, -9}, {-9, 9}, {9, 9}};
                    for (int i = 0; i < 8; i++) {
                        int[] k = c[i % 4];
                        spawnStone(sl, new Vec3(home.getX() + 0.5 + k[0], home.getY() + 1.5, home.getZ() + 0.5 + k[1]), i);
                    }
                    sl.playSound(null, blockPosition(), SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 2f, 0.5f);
                }
                if (tickCount % 45 == 0) {
                    Player t = nearestPlayer(players);
                    if (t != null && distanceToSqr(t) < 18 * 18 && hasLineOfSight(t)) {
                        Vec3 a = getEyePosition(), b = t.getEyePosition();
                        Vec3 d = b.subtract(a);
                        for (double k = 0.1; k < 1.0; k += 0.1) {
                            Vec3 v = a.add(d.scale(k));
                            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBBLESTONE)),
                                    v.x, v.y, v.z, 1, 0, 0, 0, 0);
                        }
                        t.hurt(damageSources().mobAttack(this), 6.0f);
                        sl.playSound(null, blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.5f, 0.7f);
                    }
                }
            } else exposed = 0;
        }
    }

    /** «Овца» стада: живой булыжник, летающий вокруг Пастуха. */
    public static class HerdStone extends Mob {
        public static java.util.function.Supplier<EntityType<HerdStone>> TYPE_SUPPLIER;
        private UUID owner;
        private int slot;
        private int hitCooldown;

        public HerdStone(EntityType<? extends Mob> t, Level l) { super(t, l); this.setNoGravity(true); this.xpReward = 0; }

        public static AttributeSupplier.Builder attrs() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
        }

        public void bind(UUID owner, int slot) { this.owner = owner; this.slot = slot; }

        public UUID getOwnerId() { return owner; }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
            Entity o = owner == null ? null : sl.getEntity(owner);
            if (o == null || !o.isAlive()) { discard(); return; }
            double ang = tickCount * 0.06 + slot * (Math.PI * 2 / 8.0);
            Vec3 target = o.position().add(Math.cos(ang) * 3.0, 1.3 + Math.sin(tickCount * 0.1 + slot) * 0.3, Math.sin(ang) * 3.0);
            Vec3 pos = position();
            Vec3 step = target.subtract(pos);
            if (step.length() > 0.7) step = step.normalize().scale(0.7);
            setPos(pos.add(step));
            if (hitCooldown > 0) hitCooldown--;
            else for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.4))) {
                if (p.isCreative() || p.isSpectator()) continue;
                p.hurt(damageSources().mobAttack((net.minecraft.world.entity.LivingEntity) o), 3.0f);
                hitCooldown = 25;
            }
        }

        @Override public boolean isPushable() { return false; }
        @Override public boolean removeWhenFarAway(double d) { return true; }
        @Override public void push(double x, double y, double z) {}

        @Override
        public void die(DamageSource src) {
            super.die(src);
            if (level() instanceof ServerLevel sl)
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBBLESTONE)),
                        getX(), getY(0.5), getZ(), 15, 0.3, 0.3, 0.3, 0.1);
        }

        @Override
        public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            if (owner != null) tag.putUUID("Owner", owner);
            tag.putInt("Slot", slot);
        }

        @Override
        public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
            slot = tag.getInt("Slot");
        }
    }

    // =============================== 4. Светлячковый Рой ===============================
    public static class FireflySwarm extends BaseBoss {
        private long scatteredUntil = 0;

        public FireflySwarm(EntityType<? extends Monster> t, Level l) { super(t, l, 3); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(90, 4, 0.32); }

        private boolean isWide(DamageSource src) {
            if (src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_FIRE)
                    || src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)) return true;
            if (isInWaterOrBubble()) return true;
            if (src.getDirectEntity() instanceof Player p) {
                Item it = p.getMainHandItem().getItem();
                if (it instanceof Weapons.ReaperScythe || it instanceof Weapons.FlameSickle || it instanceof Weapons.GolemSledge) return true;
                if (it instanceof SwordItem && p.getAttackStrengthScale(0.5f) > 0.9f && p.onGround() && !p.isSprinting()) return true;
            }
            return false;
        }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            long now = level().getGameTime();
            if (isWide(src)) {
                scatteredUntil = now + 80;
                if (level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.5), getZ(), 40, 1.5, 0.8, 1.5, 0.08);
                return amount;
            }
            if (now < scatteredUntil) return amount;
            return Math.min(amount, 2.0f); // обычный удар выбивает лишь часть роя
        }

        @Override
        protected void tickBoss(List<Player> players) {
            boolean scattered = level().getGameTime() < scatteredUntil;
            if (!scattered && tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(4.0f);
            if (level() instanceof ServerLevel sl && tickCount % 2 == 0) {
                double spread = scattered ? 2.4 : 0.8;
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.6), getZ(), scattered ? 8 : 5, spread, 0.6, spread, 0.01);
                sl.sendParticles(ParticleTypes.GLOW, getX(), getY(0.6), getZ(), 2, spread, 0.6, spread, 0.0);
            }
        }
    }

    // =============================== 5. Грибной Двойник ===============================
    public static class MushroomDouble extends BaseBoss {
        private record Snap(Vec3 pos, boolean swing) {}
        private final Deque<Snap> history = new ArrayDeque<>();
        private boolean catchingUp = false;
        private int strikeCooldown = 0;

        public MushroomDouble(EntityType<? extends Monster> t, Level l) { super(t, l, 4); }

        public static AttributeSupplier.Builder attrs() { return baseAttributes(120, 7, 0.4); }

        @Override protected void registerAttackGoals() { /* бьёт только зеркально */ }

        @Override
        protected float filterDamage(DamageSource src, float amount) {
            return catchingUp ? amount * 1.25f : 0f; // синхронен с игроком — удар блокируется
        }

        @Override
        protected void tickBoss(List<Player> players) {
            if (strikeCooldown > 0) strikeCooldown--;
            Player t = nearestPlayer(players);
            if (t == null) { history.clear(); catchingUp = false; return; }
            history.addLast(new Snap(t.position(), t.swinging && t.swingTime <= 1));
            if (history.size() <= 40) return;
            Snap old = history.pollFirst();
            double mx = 2 * (home.getX() + 0.5) - old.pos().x;
            double mz = 2 * (home.getZ() + 0.5) - old.pos().z;
            double dist = Math.hypot(mx - getX(), mz - getZ());
            catchingUp = dist > 1.6;
            getMoveControl().setWantedPosition(mx, old.pos().y, mz, 1.5);
            if (dist > 8) teleportTo(mx, old.pos().y, mz);
            if (old.swing() && strikeCooldown == 0 && level() instanceof ServerLevel sl) {
                strikeCooldown = 10;
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY(0.6), getZ(), 3, 0.6, 0.2, 0.6, 0);
                sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 0.7f);
                for (Player p : players) {
                    if (p.distanceToSqr(this) < 3.4 * 3.4) {
                        p.hurt(damageSources().mobAttack(this), 7.0f);
                        Vec3 d = p.position().subtract(position()).normalize();
                        p.knockback(0.7, -d.x, -d.z);
                    }
                }
            }
            if (!catchingUp && tickCount % 10 == 0 && level() instanceof ServerLevel sl2)
                sl2.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY(1.0), getZ(), 4, 0.3, 0.6, 0.3, 0.0);
        }
    }
}

package com.savelyich.trials.entity;

import com.savelyich.trials.quest.ArenaLayout;
import com.savelyich.trials.quest.QuestData;
import com.savelyich.trials.quest.QuestManager;
import com.savelyich.trials.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Общая основа боссов: привязка к арене, полоса здоровья, награда за убийство, сброс боя. */
public abstract class BaseBoss extends Monster {
    protected final int bossIndex;
    protected BlockPos home = BlockPos.ZERO;
    protected boolean hasHome = false;
    protected int noPlayerTicks = 0;
    private final ServerBossEvent bar;

    protected BaseBoss(EntityType<? extends Monster> type, Level level, int bossIndex) {
        super(type, level);
        this.bossIndex = bossIndex;
        this.xpReward = 50;
        this.setPersistenceRequired();
        this.bar = new ServerBossEvent(Component.translatable("entity.savelyich_trials." + QuestData.BOSS_IDS[bossIndex]),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    }

    public static AttributeSupplier.Builder baseAttributes(double health, double damage, double speed) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
    }

    public int getBossIndex() { return bossIndex; }

    public void setHome(BlockPos p) { this.home = p.immutable(); this.hasHome = true; }

    public boolean inArena(Entity e) {
        if (!hasHome) return true;
        int h = ArenaLayout.HALF;
        return Math.abs(e.getX() - (home.getX() + 0.5)) <= h && Math.abs(e.getZ() - (home.getZ() + 0.5)) <= h
                && e.getY() >= home.getY() - 8 && e.getY() <= home.getY() + 14;
    }

    protected List<Player> playersInArena() {
        return level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(ArenaLayout.HALF * 1.6, 12, ArenaLayout.HALF * 1.6),
                p -> inArena(p) && p.isAlive() && !p.isCreative() && !p.isSpectator());
    }

    // ---- Цели и ИИ ----

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        registerAttackGoals();
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false,
                p -> inArena(p) && canTargetPlayer((Player) p)));
    }

    protected void registerAttackGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
    }

    protected boolean canTargetPlayer(Player p) { return !p.isCreative() && !p.isSpectator(); }

    // ---- Урон ----

    /** Модификатор входящего урона; значение ≤ 0 отменяет удар. */
    protected float filterDamage(DamageSource src, float amount) { return amount; }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (level().isClientSide) return super.hurt(src, amount);
        if (src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(src, amount);
        float a = filterDamage(src, amount);
        if (a <= 0) {
            level().playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.8f, 1.2f);
            return false;
        }
        return super.hurt(src, a);
    }

    // ---- Тик ----

    protected void tickBoss(List<Player> players) {}

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        List<Player> players = playersInArena();
        if (players.isEmpty()) {
            noPlayerTicks++;
            if (noPlayerTicks > 400 && tickCount % 10 == 0) heal(getMaxHealth() * 0.03f);
            if (getTarget() != null) setTarget(null);
        } else {
            noPlayerTicks = 0;
        }
        if (hasHome && !inArena(this)) {
            teleportTo(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5);
        }
        if (getTarget() != null && !inArena(getTarget())) setTarget(null);
        trackMovement(players);
        tickBoss(players);
        bar.setProgress(getHealth() / getMaxHealth());
    }

    // ---- Скорость игроков по серверным позициям (getDeltaMovement у ServerPlayer ненадёжен) ----
    private final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> lastPos = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Double> speeds = new java.util.HashMap<>();

    private void trackMovement(List<Player> players) {
        for (Player p : players) {
            net.minecraft.world.phys.Vec3 prev = lastPos.get(p.getUUID());
            net.minecraft.world.phys.Vec3 now = p.position();
            speeds.put(p.getUUID(), prev == null ? 0.0 : Math.hypot(now.x - prev.x, now.z - prev.z));
            lastPos.put(p.getUUID(), now);
        }
        if (lastPos.size() > 8) { lastPos.keySet().retainAll(players.stream().map(Player::getUUID).toList()); speeds.keySet().retainAll(lastPos.keySet()); }
    }

    /** Блоков за тик по горизонтали (шаг ≈ 0.215, бег ≈ 0.28, подкрадывание ≈ 0.065). */
    protected double speedOf(Player p) { return speeds.getOrDefault(p.getUUID(), 0.0); }

    protected Player nearestPlayer(List<Player> players) {
        Player best = null;
        double bd = Double.MAX_VALUE;
        for (Player p : players) {
            double d = p.distanceToSqr(this);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel sl) {
            QuestManager.onBossKilled(sl, blockPosition(), bossIndex);
            for (int i = 0; i < 4; i++) {
                level().addFreshEntity(new ItemEntity(level(), getX(), getY() + 0.5, getZ(),
                        new ItemStack(ModItems.SOUL_ESSENCE.get(), 2)));
            }
        }
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canChangeDimensions() { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean isPushedByFluid() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasHome", hasHome);
        tag.putInt("HomeX", home.getX());
        tag.putInt("HomeY", home.getY());
        tag.putInt("HomeZ", home.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hasHome = tag.getBoolean("HasHome");
        home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    protected AABB area(double r) { return getBoundingBox().inflate(r); }
}

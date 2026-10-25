package com.savelyich.trials.events;

import com.savelyich.trials.item.Ranged;
import com.savelyich.trials.registry.ModEffects;
import com.savelyich.trials.registry.ModItems;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Глобальные механики оружия: всё, что зависит от урона, снарядов и тиков. */
public class WeaponEvents {

    private static final String SAND_UNTIL = "TrialsSandUntil";
    private static final String SAND_AI = "TrialsSandWasNoAi";
    private static final String SAND_INV = "TrialsSandWasInvisible";
    private static final String GHOST_TAG = "TrialsGhostArrow";
    private static final String SPRING_CLONE = "TrialsSpringClone";

    private static boolean holds(LivingEntity e, Item item) {
        return e.getMainHandItem().is(item) || e.getOffhandItem().is(item);
    }

    private static Player attackerPlayer(DamageSource src) {
        return src.getEntity() instanceof Player p ? p : null;
    }

    private static boolean isDirectMelee(DamageSource src) {
        return src.getDirectEntity() == src.getEntity() && src.getEntity() instanceof LivingEntity;
    }

    // ---- Посох Пыли: превращение в песок -------------------------------------------------

    public static void makeSand(LivingEntity e, int ticks) {
        CompoundTag d = e.getPersistentData();
        if (d.getLong(SAND_UNTIL) > 0) return;
        d.putLong(SAND_UNTIL, e.level().getGameTime() + ticks);
        d.putBoolean(SAND_INV, e.isInvisible());
        if (e instanceof Mob m) {
            d.putBoolean(SAND_AI, m.isNoAi());
            m.setNoAi(true);
        }
        e.setInvisible(true);
        if (e.level() instanceof ServerLevel sl) {
            sl.playSound(null, e.blockPosition(), SoundEvents.SAND_PLACE, SoundSource.HOSTILE, 1f, 0.5f);
        }
    }

    private static void endSand(LivingEntity e) {
        CompoundTag d = e.getPersistentData();
        d.remove(SAND_UNTIL);
        e.setInvisible(d.getBoolean(SAND_INV));
        if (e instanceof Mob m) m.setNoAi(d.getBoolean(SAND_AI));
        if (e.level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    e.getX(), e.getY(0.5), e.getZ(), 40, 0.4, 0.6, 0.4, 0.1);
        }
    }

    @SubscribeEvent
    public void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent ev) {
        LivingEntity e = ev.getEntity();
        if (e.level().isClientSide) return;
        long until = e.getPersistentData().getLong(SAND_UNTIL);
        if (until <= 0) return;
        if (e.level().getGameTime() >= until) {
            endSand(e);
            return;
        }
        if (e.level() instanceof ServerLevel sl && e.tickCount % 2 == 0) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    e.getX(), e.getY(0.5), e.getZ(), 10, e.getBbWidth() * 0.4, e.getBbHeight() * 0.4, e.getBbWidth() * 0.4, 0.02);
        }
    }

    // ---- Урон: кинжал, жнец, копьё ---------------------------------------------------------

    @SubscribeEvent
    public void onHurt(LivingHurtEvent ev) {
        LivingEntity target = ev.getEntity();
        DamageSource src = ev.getSource();
        Player p = attackerPlayer(src);
        if (target.getPersistentData().getLong(SAND_UNTIL) > 0) ev.setAmount(ev.getAmount() * 1.5f);
        if (p == null || !isDirectMelee(src)) return;
        ItemStack stack = p.getMainHandItem();

        if (stack.is(ModItems.SHADOW_DAGGER.get())) {
            Vec3 look = target.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 toAtk = p.position().subtract(target.position()).multiply(1, 0, 1).normalize();
            if (look.dot(toAtk) < -0.3) { // атакующий сзади
                ev.setAmount(ev.getAmount() * 2.0f);
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
                }
            }
        } else if (stack.is(ModItems.REAPER_SCYTHE.get())) {
            float ratio = target.getHealth() / target.getMaxHealth();
            if (ratio < 0.3f) ev.setAmount(ev.getAmount() * 1.8f);
            else if (ratio < 0.5f) ev.setAmount(ev.getAmount() * 1.35f);
        } else if (stack.is(ModItems.GUARDIAN_SPEAR.get())) {
            if (p.isInWaterOrBubble() || target.isInWaterOrBubble()) ev.setAmount(ev.getAmount() * 1.6f);
        }
    }

    /** Вампиризм Кровавого топора — по итоговому уроку. */
    @SubscribeEvent
    public void onDamage(LivingDamageEvent ev) {
        Player p = attackerPlayer(ev.getSource());
        if (p == null || !isDirectMelee(ev.getSource()) || p.level().isClientSide) return;
        if (p.getMainHandItem().is(ModItems.BLOOD_AXE.get())) {
            p.heal(ev.getAmount() * 0.35f);
            MobEffectInstance cur = p.getEffect(ModEffects.BLOOD_DEBT.get());
            int amp = cur == null ? 0 : Math.min(cur.getAmplifier() + 1, 2);
            p.addEffect(new MobEffectInstance(ModEffects.BLOOD_DEBT.get(), 20 * 20, amp, false, true));
            if (p.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, ev.getEntity().getX(), ev.getEntity().getY(0.6),
                        ev.getEntity().getZ(), 6, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent ev) {
        if (ev.getEntity().level().isClientSide) return;
        Player p = attackerPlayer(ev.getSource());
        if (p != null && p.getMainHandItem().is(ModItems.SHADOW_DAGGER.get())) {
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false));
        }
    }

    // ---- Громовая булава: молния при крите --------------------------------------------------

    @SubscribeEvent
    public void onCrit(CriticalHitEvent ev) {
        Player p = ev.getEntity();
        if (p.level().isClientSide || !p.getMainHandItem().is(ModItems.THUNDER_MACE.get())) return;
        if (!ev.isVanillaCritical() && ev.getResult() != net.minecraftforge.eventbus.api.Event.Result.ALLOW) return;
        ServerLevel sl = (ServerLevel) p.level();
        Entity target = ev.getTarget();
        boolean backfire = p.getRandom().nextFloat() < 0.15f;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            Vec3 at = backfire ? p.position() : target.position();
            bolt.moveTo(at);
            sl.addFreshEntity(bolt);
        }
        if (backfire) p.displayClientMessage(net.minecraft.network.chat.Component.literal("Булава ударила и по вам!"), true);
    }

    // ---- Щит-лезвие: накопление энергии -----------------------------------------------------

    @SubscribeEvent
    public void onShieldBlock(ShieldBlockEvent ev) {
        LivingEntity def = ev.getEntity();
        if (def.level().isClientSide) return;
        ItemStack use = def.getUseItem();
        if (use.getItem() instanceof Ranged.KineticBladeShield) {
            int e = Math.min(Ranged.KineticBladeShield.MAX, Ranged.KineticBladeShield.energy(use) + Math.round(ev.getBlockedDamage() * 2));
            use.getOrCreateTag().putInt("Kinetic", e);
            if (def.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, def.getX(), def.getY(0.6), def.getZ(), 10, 0.4, 0.4, 0.4, 0.2);
            }
        }
    }

    // ---- Снаряды: призрачный лук, арбалет, трезубец -----------------------------------------

    private static final List<AbstractArrow> GHOSTS = new ArrayList<>();

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent ev) {
        if (ev.getLevel().isClientSide) return;
        Entity e = ev.getEntity();

        if (e instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity owner) {
            CompoundTag d = arrow.getPersistentData();
            if (arrow instanceof Arrow && holds(owner, ModItems.GHOST_BOW.get()) && owner.isUsingItem()
                    && !d.getBoolean(GHOST_TAG)) {
                d.putBoolean(GHOST_TAG, true);
                arrow.setNoPhysics(true);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                arrow.setBaseDamage(arrow.getBaseDamage() + 1.0);
                d.putInt("TrialsGhostAge", 0);
                GHOSTS.add(arrow);
            }
            // выстрел арбалета: игрок держит его и НЕ натягивает (лук стреляет при отпускании, арбалет — по клику)
            if (arrow instanceof Arrow && holds(owner, ModItems.SPRING_CROSSBOW.get()) && !owner.isUsingItem() && !d.getBoolean(SPRING_CLONE)
                    && arrow.level() instanceof ServerLevel sl) {
                Vec3 v = arrow.getDeltaMovement();
                double speed = v.length();
                for (int s = -1; s <= 1; s += 2) {
                    Arrow c = new Arrow(sl, owner);
                    c.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
                    double ang = Math.toRadians(11 * s);
                    Vec3 nv = new Vec3(v.x * Math.cos(ang) - v.z * Math.sin(ang), v.y, v.x * Math.sin(ang) + v.z * Math.cos(ang));
                    c.setDeltaMovement(nv.normalize().scale(speed));
                    c.setBaseDamage(arrow.getBaseDamage());
                    c.setCritArrow(arrow.isCritArrow());
                    c.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    c.getPersistentData().putBoolean(SPRING_CLONE, true);
                    sl.addFreshEntity(c);
                }
                if (owner instanceof Player p) p.getCooldowns().addCooldown(ModItems.SPRING_CROSSBOW.get(), 60);
            }
        }

        if (e instanceof ThrownTrident t && t.getOwner() instanceof LivingEntity owner && holds(owner, ModItems.ECHO_TRIDENT.get())) {
            t.getPersistentData().putBoolean("TrialsEcho", true);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        Iterator<AbstractArrow> it = GHOSTS.iterator();
        while (it.hasNext()) {
            AbstractArrow a = it.next();
            if (!a.isAlive()) { it.remove(); continue; }
            int age = a.getPersistentData().getInt("TrialsGhostAge") + 1;
            a.getPersistentData().putInt("TrialsGhostAge", age);
            if (a.level() instanceof ServerLevel sl && age % 2 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, a.getX(), a.getY(), a.getZ(), 1, 0, 0, 0, 0);
            }
            if (age >= 12) { // 12 тиков ≈ 10–12 блоков сквозь стены
                a.setNoPhysics(false);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onImpact(ProjectileImpactEvent ev) {
        if (!(ev.getProjectile() instanceof ThrownTrident t) || t.level().isClientSide) return;
        if (!t.getPersistentData().getBoolean("TrialsEcho") || t.getPersistentData().getBoolean("TrialsEchoDone")) return;
        t.getPersistentData().putBoolean("TrialsEchoDone", true);
        ServerLevel sl = (ServerLevel) t.level();
        sl.playSound(null, t.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 1.4f);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, t.getX(), t.getY(), t.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP, t.getX(), t.getY(), t.getZ(), 40, 2.5, 0.5, 2.5, 0.05);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(5.0),
                x -> x != t.getOwner() && x.isAlive())) {
            le.addEffect(new MobEffectInstance(ModEffects.STUN.get(), 80, 0));
            le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            if (le instanceof Mob m) m.setTarget(null);
        }
    }

    // ---- Души: источник боеприпасов для Призрачного лука -------------------------------------

    @SubscribeEvent
    public void onDrops(LivingDropsEvent ev) {
        LivingEntity e = ev.getEntity();
        if (e.level().isClientSide || !(e instanceof Monster)) return;
        if (e.getRandom().nextFloat() < 0.30f) {
            ev.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(e.level(), e.getX(), e.getY(), e.getZ(),
                    new ItemStack(ModItems.SOUL_ESSENCE.get())));
        }
    }
}

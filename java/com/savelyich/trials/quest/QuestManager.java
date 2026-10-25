package com.savelyich.trials.quest;

import com.savelyich.trials.net.Net;
import com.savelyich.trials.net.SyncQuestsPacket;
import com.savelyich.trials.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.PacketDistributor;

/** Хранит прогресс игрока в persistent data (переживает смерть) и синхронизирует с клиентом. */
public final class QuestManager {
    private QuestManager() {}

    private static final String ROOT = "SavelyichTrials";

    private static CompoundTag root(Player p) {
        CompoundTag persisted = p.getPersistentData();
        if (!persisted.contains(Player.PERSISTED_NBT_TAG)) persisted.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag pt = persisted.getCompound(Player.PERSISTED_NBT_TAG);
        if (!pt.contains(ROOT)) pt.put(ROOT, new CompoundTag());
        return pt.getCompound(ROOT);
    }

    public static int[] states(Player p) {
        int[] s = root(p).getIntArray("Q");
        if (s.length != QuestData.COUNT) s = new int[QuestData.COUNT];
        return s;
    }

    private static void save(Player p, int[] s) { root(p).putIntArray("Q", s); }

    public static boolean met(Player p) { return root(p).getBoolean("Met"); }
    public static void setMet(Player p) { root(p).putBoolean("Met", true); }
    public static boolean finaleSeen(Player p) { return root(p).getBoolean("Finale"); }
    public static void setFinaleSeen(Player p) { root(p).putBoolean("Finale", true); }

    public static int activeIndex(Player p) {
        int[] s = states(p);
        for (int i = 0; i < s.length; i++) if (s[i] == QuestData.ACTIVE) return i;
        return -1;
    }

    public static int reportIndex(Player p) {
        int[] s = states(p);
        for (int i = 0; i < s.length; i++) if (s[i] == QuestData.KILLED) return i;
        return -1;
    }

    /** Первый квест, ещё не выданный. -1 если всё выдано. */
    public static int nextIndex(Player p) {
        int[] s = states(p);
        for (int i = 0; i < s.length; i++) if (s[i] == QuestData.LOCKED) return i;
        return -1;
    }

    public static boolean allDone(Player p) {
        for (int v : states(p)) if (v != QuestData.DONE) return false;
        return true;
    }

    // ----- действия -----

    public static void accept(ServerPlayer p, int i) {
        int[] s = states(p);
        if (i < 0 || i >= s.length || s[i] != QuestData.LOCKED || activeIndex(p) >= 0 || reportIndex(p) >= 0) return;
        s[i] = QuestData.ACTIVE;
        save(p, s);
        giveWeapon(p, i);
        ServerLevel overworld = p.server.overworld();
        MapData md = MapData.get(overworld);
        if (md.hasOrigin) com.savelyich.trials.map.BossSpawner.ensureBoss(overworld, md.origin, i);
        p.displayClientMessage(Component.literal("Новое задание: убить «" + QuestData.BOSS_NAMES[i]
                + "» (" + QuestData.ARENA_NAMES[i] + "). Журнал — клавиша J."), false);
        sync(p);
    }

    public static void giveWeapon(ServerPlayer p, int quest) {
        ItemStack w = ModItems.createWeaponStack(QuestData.QUEST_WEAPON[quest]);
        if (!p.getInventory().add(w)) p.drop(w, false);
        if (quest == 13) { // Фонарный Зал: запас света
            ItemStack torches = new ItemStack(Items.TORCH, 64);
            if (!p.getInventory().add(torches)) p.drop(torches, false);
            ItemStack lanterns = new ItemStack(Items.LANTERN, 8);
            if (!p.getInventory().add(lanterns)) p.drop(lanterns, false);
        }
        if (QuestData.QUEST_WEAPON[quest] == 2) { // Призрачный лук — сразу выдаём запас душ
            ItemStack souls = new ItemStack(ModItems.SOUL_ESSENCE.get(), 16);
            if (!p.getInventory().add(souls)) p.drop(souls, false);
        }
    }

    /** Игрок сдаёт задание. */
    public static void report(ServerPlayer p, int i) {
        int[] s = states(p);
        if (i < 0 || i >= s.length || s[i] != QuestData.KILLED) return;
        s[i] = QuestData.DONE;
        save(p, s);
        ItemStack souls = new ItemStack(ModItems.SOUL_ESSENCE.get(), 8);
        if (!p.getInventory().add(souls)) p.drop(souls, false);
        p.giveExperiencePoints(150 + 30 * i);
        if (allDone(p)) {
            ItemStack em = new ItemStack(Items.EMERALD, 16);
            if (!p.getInventory().add(em)) p.drop(em, false);
        }
        p.displayClientMessage(Component.literal("Задание выполнено: «" + QuestData.BOSS_NAMES[i] + "»."), false);
        sync(p);
    }

    /** Босс убит: засчитываем всем игрокам поблизости с активным квестом на этого босса. */
    public static void onBossKilled(ServerLevel level, BlockPos pos, int bossIndex) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 60 * 60) continue;
            int[] s = states(p);
            if (s[bossIndex] == QuestData.ACTIVE) {
                s[bossIndex] = QuestData.KILLED;
                save(p, s);
                p.displayClientMessage(Component.literal(
                        "«" + QuestData.BOSS_NAMES[bossIndex] + "» повержен. Вернитесь к Савельевичу."), false);
                p.displayClientMessage(Component.literal("§6[Савельевич]§r "
                        + QuestData.KILL_COMMENTS[p.getRandom().nextInt(QuestData.KILL_COMMENTS.length)]), false);
                sync(p);
            }
        }
    }

    public static void resetAll(ServerPlayer p) {
        save(p, new int[QuestData.COUNT]);
        root(p).putBoolean("Met", false);
        root(p).putBoolean("Finale", false);
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        MapData md = MapData.get(p.server.overworld());
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new SyncQuestsPacket(states(p), md.origin, md.hasOrigin && md.built, met(p), finaleSeen(p)));
    }
}

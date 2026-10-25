package com.savelyich.trials.events;

import com.savelyich.trials.entity.Bosses3;
import com.savelyich.trials.entity.SavelyichEntity;
import com.savelyich.trials.map.Arenas;
import com.savelyich.trials.map.Brush;
import com.savelyich.trials.map.MapBuilder;
import com.savelyich.trials.quest.ArenaLayout;
import com.savelyich.trials.quest.MapData;
import com.savelyich.trials.quest.QuestData;
import com.savelyich.trials.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSwingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Всё, что связано с миром: автопостройка карты, ритм поршней, названия арен, комментарии Савельевича. */
public class WorldEvents {

    private static final String ARENA_TAG = "TrialsArena";

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer sp)) return;
        ServerLevel ow = sp.server.overworld();
        MapData md = MapData.get(ow);
        if (!md.hasOrigin) {
            BlockPos spawn = ow.getSharedSpawnPos();
            int y = ow.getHeight(Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ()) - 1;
            MapBuilder.start(ow, new BlockPos(spawn.getX(), y, spawn.getZ()));
        } else if (!md.built && !MapBuilder.isRunning()) {
            MapBuilder.start(ow, md.origin); // доделать, если сервер остановили посреди стройки
        }
        QuestManager.sync(sp);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        MapBuilder.tick(ev.getServer());
        ServerLevel ow = ev.getServer().overworld();
        long t = ow.getGameTime();
        MapData md = MapData.get(ow);
        if (!md.hasOrigin || !md.built) return;

        if (t % 20 == 0) pistonRhythm(ow, md.origin, t);

        if (t % 20 == 10) for (ServerPlayer p : ow.players()) arenaTitle(p, md.origin);

        if (t % 1500 == 700) { // Савельевич иногда бурчит рядом с игроком
            for (ServerPlayer p : ow.players()) {
                if (ow.getEntitiesOfClass(SavelyichEntity.class, p.getBoundingBox().inflate(7)).isEmpty()) continue;
                String line = QuestManager.allDone(p) ? QuestData.ALL_DONE_IDLE
                        : QuestData.IDLE[p.getRandom().nextInt(QuestData.IDLE.length)];
                p.displayClientMessage(Component.literal("§6[Савельевич]§r " + line), false);
            }
        }
    }

    /** Поршни «Зала Зеркал» качаются в такт: 1 секунда включены — 1 секунда выключены. */
    private void pistonRhythm(ServerLevel ow, BlockPos origin, long t) {
        BlockPos c = ArenaLayout.center(origin, 4);
        boolean near = false;
        for (ServerPlayer p : ow.players()) if (p.distanceToSqr(c.getX(), c.getY(), c.getZ()) < 45 * 45) { near = true; break; }
        if (!near) return;
        boolean powered = (t / 20) % 2 == 0;
        Brush b = new Brush(ow, c, ArenaLayout.gateSide(origin, 4));
        for (int[] p : Arenas.PISTONS) {
            BlockPos pos = b.at(p[2], 0, p[3]);
            var want = (powered ? Blocks.REDSTONE_BLOCK : Blocks.QUARTZ_BLOCK).defaultBlockState();
            if (ow.getBlockState(pos) != want) ow.setBlock(pos, want, 3);
        }
    }

    private void arenaTitle(ServerPlayer p, BlockPos origin) {
        if (p.level() != p.server.overworld()) return;
        int inside = -1;
        for (int i = 0; i < QuestData.COUNT; i++) {
            BlockPos c = ArenaLayout.center(origin, i);
            if (Math.abs(p.getX() - c.getX()) <= ArenaLayout.HALF && Math.abs(p.getZ() - c.getZ()) <= ArenaLayout.HALF) { inside = i; break; }
        }
        int prev = p.getPersistentData().contains(ARENA_TAG) ? p.getPersistentData().getInt(ARENA_TAG) : -1;
        if (inside != prev) {
            p.getPersistentData().putInt(ARENA_TAG, inside);
            if (inside >= 0) {
                p.displayClientMessage(Component.literal("§e" + QuestData.ARENA_NAMES[inside] + " §7— " + QuestData.BOSS_NAMES[inside]), true);
                p.displayClientMessage(Component.literal("§6[Савельевич]§r " + QuestData.ARENA_ENTER[inside]), false);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        MapData md = MapData.get(p.server.overworld());
        if (!md.hasOrigin || p.level() != p.server.overworld()) return;
        if (p.distanceToSqr(md.origin.getX(), md.origin.getY(), md.origin.getZ()) > 160 * 160) return;
        p.displayClientMessage(Component.literal("§6[Савельевич]§r "
                + QuestData.DEATH_COMMENTS[p.getRandom().nextInt(QuestData.DEATH_COMMENTS.length)]), false);
    }

    /** Взмах игрока → Двойник Луны готовит зеркальный удар. */
    @SubscribeEvent
    public void onSwing(LivingSwingEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        for (Bosses3.MoonDouble m : p.level().getEntitiesOfClass(Bosses3.MoonDouble.class, p.getBoundingBox().inflate(30, 10, 30)))
            m.onSwing(p);
    }
}

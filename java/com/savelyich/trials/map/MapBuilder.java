package com.savelyich.trials.map;

import com.savelyich.trials.entity.SavelyichEntity;
import com.savelyich.trials.quest.ArenaLayout;
import com.savelyich.trials.quest.MapData;
import com.savelyich.trials.quest.QuestData;
import com.savelyich.trials.quest.QuestManager;
import com.savelyich.trials.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.Deque;

/** Строит карту порциями по тикам, чтобы сервер не зависал: хаб, 15 дорог, 15 арен, Савельевич, боссы. */
public final class MapBuilder {
    private MapBuilder() {}

    private static final Deque<Runnable> QUEUE = new ArrayDeque<>();
    private static int total = 0;
    private static boolean running = false;
    private static ServerLevel level;
    private static BlockPos origin;

    public static boolean isRunning() { return running; }

    public static void start(ServerLevel overworld, BlockPos floorPos) {
        if (running) return;
        MapData md = MapData.get(overworld);
        md.setOrigin(floorPos);
        md.setBuilt(false);
        level = overworld;
        origin = floorPos.immutable();
        QUEUE.clear();
        QUEUE.add(() -> buildHub(level, origin));
        for (int i = 0; i < QuestData.COUNT; i++) {
            final int k = i;
            QUEUE.add(() -> buildRoad(level, origin, k));
            QUEUE.add(() -> {
                BlockPos c = ArenaLayout.center(origin, k);
                level.getChunk(c);
                Arenas.BUILDERS[k].accept(new Brush(level, c, ArenaLayout.gateSide(origin, k)));
            });
        }
        QUEUE.add(MapBuilder::finish);
        total = QUEUE.size();
        running = true;
        broadcast("Савельевич зовёт: строю карту (" + total + " этапов). Не уходите далеко от центра.");
    }

    /** Вызывается каждый серверный тик; тратит не более ~25 мс. */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (!running) return;
        long t0 = System.nanoTime();
        int before = QUEUE.size();
        while (!QUEUE.isEmpty() && System.nanoTime() - t0 < 25_000_000L) {
            Runnable r = QUEUE.poll();
            try { r.run(); } catch (Exception e) { com.mojang.logging.LogUtils.getLogger().error("Ошибка при строительстве карты", e); }
        }
        int done = total - QUEUE.size();
        if (QUEUE.size() != before && done % 6 == 0 && !QUEUE.isEmpty())
            broadcast("Строительство карты: " + (100 * done / total) + "%");
    }

    private static void broadcast(String s) {
        if (level == null) return;
        for (ServerPlayer p : level.players()) p.displayClientMessage(Component.literal(s), true);
    }

    private static void finish() {
        BossSpawner.ensureAll(level, origin);
        if (level.getEntitiesOfClass(SavelyichEntity.class, new AABB(origin).inflate(16, 8, 16)).isEmpty()) {
            SavelyichEntity s = ModEntities.SAVELYICH.get().create(level);
            if (s != null) {
                s.moveTo(origin.getX() + 0.5, origin.getY() + 1, origin.getZ() - 7 + 0.5, 0f, 0f);
                level.addFreshEntity(s);
            }
        }
        level.setDefaultSpawnPos(origin.offset(0, 1, 6), 0f);
        MapData md = MapData.get(level);
        md.setBuilt(true);
        running = false;
        broadcast("Карта готова! Савельевич ждёт у кузницы в центре.");
        for (ServerPlayer p : level.players()) {
            p.teleportTo(level, origin.getX() + 0.5, origin.getY() + 1, origin.getZ() + 6.5, 180f, 0f);
            QuestManager.sync(p);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Хаб
    // ---------------------------------------------------------------------------------------
    private static void set(ServerLevel l, BlockPos o, int x, int y, int z, Block b) { l.setBlock(o.offset(x, y, z), b.defaultBlockState(), 2); }

    private static void set(ServerLevel l, BlockPos o, int x, int y, int z, BlockState s) { l.setBlock(o.offset(x, y, z), s, 2); }

    static void buildHub(ServerLevel l, BlockPos o) {
        int R = ArenaLayout.HUB_HALF;
        for (int x = -R - 2; x <= R + 2; x++)
            for (int z = -R - 2; z <= R + 2; z++) {
                int d2 = x * x + z * z;
                if (d2 > (R + 1.5) * (R + 1.5)) continue;
                for (int y = -6; y <= -1; y++) set(l, o, x, y, z, Blocks.COBBLESTONE);
                for (int y = 1; y <= 14; y++) set(l, o, x, y, z, Blocks.AIR);
                Block floor;
                int h = Brush.hash(x, z, 21);
                if (d2 >= (R - 1) * (R - 1)) floor = Blocks.SMOOTH_STONE;
                else if (d2 <= 4) floor = Blocks.POLISHED_ANDESITE;
                else floor = h < 20 ? Blocks.POLISHED_ANDESITE : h < 45 ? Blocks.STONE_BRICKS : h < 55 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS;
                set(l, o, x, 0, z, floor);
            }
        // фонтан
        for (int x = -2; x <= 2; x++)
            for (int z = 0; z <= 4; z++) {
                int dz = z - 2;
                if (Math.max(Math.abs(x), Math.abs(dz)) == 2) { set(l, o, x, 0, z, Blocks.QUARTZ_BLOCK); set(l, o, x, 1, z, Blocks.QUARTZ_SLAB); }
                else set(l, o, x, 0, z, Blocks.WATER);
            }
        set(l, o, 0, 1, 2, Blocks.QUARTZ_PILLAR);
        set(l, o, 0, 2, 2, Blocks.QUARTZ_PILLAR);
        set(l, o, 0, 3, 2, Blocks.SEA_LANTERN);

        // кузница Савельевича (открытый павильон у северного края)
        for (int x = -5; x <= 5; x++)
            for (int z = -11; z <= -5; z++) {
                set(l, o, x, 0, z, Blocks.SPRUCE_PLANKS);
                set(l, o, x, 5, z, Blocks.SPRUCE_SLAB);
            }
        for (int x = -5; x <= 5; x += 10) for (int z = -11; z <= -5; z += 6) for (int y = 1; y <= 4; y++) set(l, o, x, y, z, Blocks.STRIPPED_SPRUCE_LOG);
        for (int x = -5; x <= 5; x++) for (int y = 1; y <= 4; y++) set(l, o, x, y, -11, Blocks.COBBLESTONE);
        for (int y = 1; y <= 4; y++) { set(l, o, -5, y, -8, Blocks.COBBLESTONE); set(l, o, 5, y, -8, Blocks.COBBLESTONE); }
        set(l, o, -3, 1, -10, Blocks.BLAST_FURNACE);
        set(l, o, -1, 1, -10, Blocks.ANVIL);
        set(l, o, 1, 1, -10, Blocks.GRINDSTONE);
        set(l, o, 3, 1, -10, Blocks.SMITHING_TABLE);
        set(l, o, -4, 1, -10, Blocks.BARREL);
        set(l, o, 4, 1, -10, Blocks.CHEST);
        set(l, o, -4, 1, -6, Blocks.CAULDRON);
        set(l, o, 4, 1, -6, Blocks.LAVA_CAULDRON);
        set(l, o, 0, 4, -9, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        set(l, o, -2, 4, -7, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        set(l, o, 2, 4, -7, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        // фонари по периметру
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6 + 0.26;
            int x = (int) Math.round((R - 2) * Math.cos(a)), z = (int) Math.round((R - 2) * Math.sin(a));
            if (z < -4 && Math.abs(x) <= 6) continue; // не загораживать кузницу
            for (int y = 1; y <= 3; y++) set(l, o, x, y, z, Blocks.OAK_FENCE);
            set(l, o, x, 4, z, Blocks.LANTERN);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Дороги
    // ---------------------------------------------------------------------------------------
    static void buildRoad(ServerLevel l, BlockPos o, int i) {
        BlockPos a = ArenaLayout.hubExit(o, i);
        BlockPos b = ArenaLayout.gateOutside(o, i);
        int x0 = a.getX(), z0 = a.getZ(), x1 = b.getX(), z1 = b.getZ();
        int n = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        int y = o.getY();
        for (int k = 0; k <= n; k++) {
            int x = x0 + Math.round((float) (x1 - x0) * k / Math.max(n, 1));
            int z = z0 + Math.round((float) (z1 - z0) * k / Math.max(n, 1));
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = new BlockPos(x + dx, y, z + dz);
                    for (int yy = 1; yy <= 4; yy++) l.setBlock(p.above(yy), Blocks.AIR.defaultBlockState(), 2);
                    for (int yy = 1; yy <= 4; yy++) {
                        BlockState st = l.getBlockState(p.below(yy));
                        if (st.isAir() || st.canBeReplaced() || !st.getFluidState().isEmpty()) l.setBlock(p.below(yy), Blocks.COBBLESTONE.defaultBlockState(), 2);
                    }
                    int h = Brush.hash(p.getX(), p.getZ(), 31);
                    l.setBlock(p, (h < 35 ? Blocks.GRAVEL : h < 70 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS).defaultBlockState(), 2);
                }
            if (k % 9 == 4) { // фонарный столб сбоку
                BlockPos lp = new BlockPos(x + 2, y + 1, z + 2);
                l.setBlock(lp, Blocks.OAK_FENCE.defaultBlockState(), 2);
                l.setBlock(lp.above(), Blocks.OAK_FENCE.defaultBlockState(), 2);
                l.setBlock(lp.above(2), Blocks.LANTERN.defaultBlockState(), 2);
            }
        }
    }
}

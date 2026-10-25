package com.savelyich.trials.map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Рисует в локальных координатах арены: x — вправо, y — вверх (пол арены = 0), z — вглубь к воротам.
 * Ворота всегда на z = +HALF+1, независимо от поворота арены в мире.
 */
public final class Brush {
    public final ServerLevel level;
    public final BlockPos center;
    private final int gx, gz, rx, rz;

    public static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public Brush(ServerLevel level, BlockPos center, Direction gate) {
        this.level = level;
        this.center = center;
        this.gx = gate.getStepX();
        this.gz = gate.getStepZ();
        this.rx = gz;
        this.rz = -gx;
    }

    /** Брошь для хаба: без поворота (ворот нет). */
    public static Brush plain(ServerLevel level, BlockPos origin) { return new Brush(level, origin, Direction.SOUTH); }

    public BlockPos at(int x, int y, int z) {
        return center.offset(x * rx + z * gx, y, x * rz + z * gz);
    }

    public static BlockState s(Block b) { return b.defaultBlockState(); }

    public void set(int x, int y, int z, BlockState st) { level.setBlock(at(x, y, z), st, 2); }

    public void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }

    public BlockState get(int x, int y, int z) { return level.getBlockState(at(x, y, z)); }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState st) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, st);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) { fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState()); }

    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) { fill(x1, y1, z1, x2, y2, z2, AIR); }

    /** Детерминированный «шум» 0..99 для рассыпанных деталей. */
    public static int hash(int x, int z, int salt) {
        int h = x * 374761393 + z * 668265263 + salt * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return Math.abs(h ^ (h >>> 16)) % 100;
    }
}

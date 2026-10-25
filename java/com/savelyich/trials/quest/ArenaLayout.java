package com.savelyich.trials.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Геометрия карты: хаб в центре, 15 арен по кольцу. Всё считается от точки origin (пол хаба). */
public final class ArenaLayout {
    private ArenaLayout() {}

    public static final int RING_RADIUS = 90;
    public static final int HALF = 12;       // арена = (2*HALF+1)^2
    public static final int HUB_HALF = 13;

    /** Центр арены i; Y = Y хаба. Арена 0 — на севере, дальше по часовой стрелке. */
    public static BlockPos center(BlockPos origin, int i) {
        double a = Math.toRadians(i * 24.0);
        int dx = (int) Math.round(RING_RADIUS * Math.sin(a));
        int dz = (int) Math.round(-RING_RADIUS * Math.cos(a));
        return origin.offset(dx, 0, dz);
    }

    /** Сторона арены, обращённая к хабу — там ворота. */
    public static Direction gateSide(BlockPos origin, int i) {
        BlockPos c = center(origin, i);
        int dx = origin.getX() - c.getX();
        int dz = origin.getZ() - c.getZ();
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Блок прямо за воротами (снаружи) — конец дороги. */
    public static BlockPos gateOutside(BlockPos origin, int i) {
        Direction d = gateSide(origin, i);
        return center(origin, i).relative(d, HALF + 2);
    }

    /** Точка у выхода дороги из хаба. */
    public static BlockPos hubExit(BlockPos origin, int i) {
        double a = Math.toRadians(i * 24.0);
        int dx = (int) Math.round((HUB_HALF + 1) * Math.sin(a));
        int dz = (int) Math.round(-(HUB_HALF + 1) * Math.cos(a));
        return origin.offset(dx, 0, dz);
    }

    /** Куда телепортировать игрока внутри арены (внутри ворот). */
    public static BlockPos entrance(BlockPos origin, int i) {
        return center(origin, i).relative(gateSide(origin, i), HALF - 2);
    }
}

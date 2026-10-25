package com.savelyich.trials.map;

import com.savelyich.trials.quest.ArenaLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/** Постройки 15 арен. Координаты локальные (см. Brush). Пол арены — y = 0, ворота на z = +13. */
public final class Arenas {
    private Arenas() {}

    private static final int H = ArenaLayout.HALF;

    /** Где в арене (локально x, z) появляется босс. */
    public static final int[][] SPAWN = {
            {0, 0}, {0, -8}, {0, -2}, {0, 0}, {0, 0}, {3, 3}, {0, 0}, {5, 0},
            {0, -8}, {0, -2}, {0, 0}, {0, -6}, {0, -3}, {0, -3}, {0, -3}};

    @SuppressWarnings("unchecked")
    public static final Consumer<Brush>[] BUILDERS = new Consumer[]{
            Arenas::hallOfSilence, Arenas::mirrorCourt, Arenas::pen, Arenas::dome, Arenas::hallOfMirrors,
            Arenas::sandIsland, Arenas::cauldron, Arenas::forge, Arenas::shadowTheatre, Arenas::rootWell,
            Arenas::glassDome, Arenas::bonfire, Arenas::armoury, Arenas::lanternHall, Arenas::mirrorArena};

    // ---------------------------------------------------------------------------------------
    // Общая оболочка: фундамент, пол, стены с воротами, угловые башенки, (опционально) крыша
    // ---------------------------------------------------------------------------------------
    static void shell(Brush b, Block wall, Block floor, Block roof) {
        b.fill(-H - 1, -6, -H - 1, H + 1, -1, H + 1, Blocks.COBBLESTONE);
        b.clear(-H - 1, 1, -H - 1, H + 1, 18, H + 1);
        b.fill(-H, 0, -H, H, 0, H, floor);
        for (int x = -H - 1; x <= H + 1; x++)
            for (int z = -H - 1; z <= H + 1; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != H + 1) continue;
                b.fill(x, 0, z, x, 8, z, wall);
            }
        gate(b);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                b.fill(sx * (H + 1), 0, sz * (H + 1), sx * (H + 1), 10, sz * (H + 1), Blocks.STONE_BRICKS);
                b.set(sx * (H + 1), 11, sz * (H + 1), Blocks.LANTERN);
            }
        if (roof != null) b.fill(-H - 1, 9, -H - 1, H + 1, 9, H + 1, roof);
    }

    static void gate(Brush b) {
        b.clear(-1, 1, H + 1, 1, 4, H + 1);
        b.fill(-2, 0, H + 1, -2, 6, H + 1, Blocks.STONE_BRICKS);
        b.fill(2, 0, H + 1, 2, 6, H + 1, Blocks.STONE_BRICKS);
        b.fill(-2, 5, H + 1, 2, 5, H + 1, Blocks.STONE_BRICKS);
        b.set(-2, 7, H + 1, Blocks.LANTERN);
        b.set(2, 7, H + 1, Blocks.LANTERN);
        b.fill(-1, 0, H + 1, 1, 0, H + 2, Blocks.SMOOTH_STONE);
    }

    static BlockState hangingLantern(boolean soul) {
        return (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    // 0 ---------------------------------------------------------------- Зал Тишины
    static void hallOfSilence(Brush b) {
        shell(b, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_TILES);
        for (int x = -H; x <= H; x++)
            for (int z = -H; z <= H; z++) {
                if (x * x + z * z > 11.5 * 11.5) continue;
                b.set(x, 0, z, Brush.hash(x, z, 1) < 14 && x * x + z * z > 16 ? Blocks.GRAVEL : Blocks.WHITE_WOOL);
            }
        for (int i = 0; i < 8; i++) { // акустические колонны
            int x = (int) Math.round(8 * Math.cos(i * Math.PI / 4)), z = (int) Math.round(8 * Math.sin(i * Math.PI / 4));
            b.fill(x, 1, z, x, 6, z, Blocks.POLISHED_DEEPSLATE);
            b.set(x, 7, z, Blocks.SCULK_SENSOR);
        }
        for (int i = 0; i < 4; i++) b.set(i < 2 ? -4 : 4, 8, (i & 1) == 0 ? -4 : 4, hangingLantern(true));
        b.set(0, 8, 0, hangingLantern(true));
    }

    // 1 ---------------------------------------------------------------- Зеркальный Двор
    static void mirrorCourt(Brush b) {
        shell(b, Blocks.GLASS, Blocks.POLISHED_DIORITE, null);
        b.fill(-4, -2, -4, 4, 0, 4, Blocks.WATER);
        b.fill(-5, 0, -5, 5, 0, -5, Blocks.QUARTZ_BLOCK);
        b.fill(-5, 0, 5, 5, 0, 5, Blocks.QUARTZ_BLOCK);
        b.fill(-5, 0, -5, -5, 0, 5, Blocks.QUARTZ_BLOCK);
        b.fill(5, 0, -5, 5, 0, 5, Blocks.QUARTZ_BLOCK);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) { // стеклянные зеркальные колонны
                b.fill(sx * 9, 1, sz * 9, sx * 9, 5, sz * 9, Blocks.TINTED_GLASS);
                b.fill(sx * 9, 1, sz * 6, sx * 9, 4, sz * 6, Blocks.GLASS);
            }
        b.fill(-3, 1, -9, 3, 3, -9, Blocks.GLASS);
    }

    // 2 ---------------------------------------------------------------- Загон
    static void pen(Brush b) {
        shell(b, Blocks.STONE_BRICKS, Blocks.COARSE_DIRT, null);
        for (int x = -H; x <= H; x++)
            for (int z = -H; z <= H; z++) {
                int h = Brush.hash(x, z, 2);
                if (h < 20) b.set(x, 0, z, Blocks.GRAVEL);
                else if (h < 30) b.set(x, 0, z, Blocks.DIRT);
            }
        int f = H - 1;
        for (int i = -f; i <= f; i++) {
            b.set(i, 1, -f, Blocks.OAK_FENCE);
            b.set(-f, 1, i, Blocks.OAK_FENCE);
            b.set(f, 1, i, Blocks.OAK_FENCE);
            if (Math.abs(i) > 1) b.set(i, 1, f, Blocks.OAK_FENCE);
        }
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) { // кучи камня по углам
                int cx = sx * 8, cz = sz * 8;
                for (int l = 0; l < 3; l++) b.fill(cx - 2 + l, 1 + l, cz - 2 + l, cx + 2 - l, 1 + l, cz + 2 - l, Blocks.COBBLESTONE);
            }
        b.set(-5, 1, 6, Blocks.HAY_BLOCK);
        b.set(5, 1, 6, Blocks.HAY_BLOCK);
        b.set(5, 2, 6, Blocks.HAY_BLOCK);
    }

    // 3 ---------------------------------------------------------------- Купол
    static void dome(Brush b) {
        b.fill(-H - 1, -6, -H - 1, H + 1, -1, H + 1, Blocks.COBBLESTONE);
        b.clear(-H - 1, 1, -H - 1, H + 1, 18, H + 1);
        for (int x = -12; x <= 12; x++)
            for (int z = -12; z <= 12; z++) {
                if (x * x + z * z <= 144) b.set(x, 0, z, Blocks.STONE_BRICKS);
            }
        for (int x = -12; x <= 12; x++)
            for (int y = 0; y <= 13; y++)
                for (int z = -12; z <= 13; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d < 11.0 || d > 12.3) continue;
                    if (y == 0) { b.set(x, 0, z, Blocks.STONE_BRICKS); continue; }
                    if (Math.abs(x) <= 1 && z > 0 && y <= 3) continue; // ворота
                    b.set(x, y, z, Blocks.GLASS);
                }
        b.fill(-1, 0, 11, 1, 0, 14, Blocks.SMOOTH_STONE);
        // узкие проходы для «рассеивания» роя
        int[][] walls = {{-6, -9, -6, -2}, {6, -9, 6, -2}, {-3, 3, 3, 3}, {-9, 0, -9, 7}, {9, 0, 9, 7}, {-1, -6, 1, -6}};
        for (int[] w : walls) b.fill(w[0], 1, w[1], w[2], 5, w[3], Blocks.STONE_BRICKS);
        for (int[] w : walls) b.set(w[0], 6, w[1], Blocks.LANTERN);
        b.set(0, 0, 0, Blocks.SEA_LANTERN);
        b.set(0, 0, -9, Blocks.SEA_LANTERN);
    }

    // 4 ---------------------------------------------------------------- Зал Зеркал
    /** {x поршня, z поршня, x блока питания, z блока питания}, поршни смотрят вверх. */
    public static final int[][] PISTONS = {
            {9, 0, 10, 0}, {-9, 0, -10, 0}, {0, 9, 0, 10}, {0, -9, 0, -10},
            {6, 6, 7, 6}, {-6, 6, -7, 6}, {6, -6, 7, -6}, {-6, -6, -7, -6}};

    static void hallOfMirrors(Brush b) {
        shell(b, Blocks.QUARTZ_BRICKS, Blocks.QUARTZ_BLOCK, null);
        for (int x = -H; x <= H; x++)
            for (int z = -H; z <= H; z++)
                if (((x + z) & 1) != 0) b.set(x, 0, z, Blocks.POLISHED_BLACKSTONE);
        BlockState piston = Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP);
        for (int[] p : PISTONS) b.set(p[0], 0, p[1], piston);
        for (int i = -10; i <= 10; i += 5) { // «зеркала» вдоль стен
            b.fill(i, 2, -H, i, 5, -H, Blocks.GLASS);
            b.fill(-H, 2, i, -H, 5, i, Blocks.GLASS);
            b.fill(H, 2, i, H, 5, i, Blocks.GLASS);
        }
        b.set(0, 8, 0, hangingLantern(false));
    }

    // 5 ---------------------------------------------------------------- Арена-Остров
    public static final int[][] PADS = {{-7, -7}, {7, -7}, {-7, 6}, {7, 6}, {0, -1}, {0, 8}};

    static void sandIsland(Brush b) {
        shell(b, Blocks.STONE_BRICKS, Blocks.SAND, null);
        b.fill(-H, -3, -H, H, -1, H, Blocks.SAND);
        b.fill(-H, 0, -H, H, 0, H, Blocks.SAND);
        for (int[] p : PADS) {
            b.fill(p[0] - 1, -3, p[1] - 1, p[0] + 1, 0, p[1] + 1, Blocks.SMOOTH_STONE);
            b.fill(p[0] - 1, 0, p[1] - 1, p[0] + 1, 0, p[1] + 1, Blocks.STONE_BRICKS);
        }
        b.fill(-1, 0, H, 1, 0, H, Blocks.SMOOTH_STONE); // у ворот — твёрдый вход
        b.fill(-1, -3, H, 1, -1, H, Blocks.SMOOTH_STONE);
    }

    // 6 ---------------------------------------------------------------- Котёл
    static void cauldron(Brush b) {
        shell(b, Blocks.BLACKSTONE, Blocks.STONE_BRICKS, null);
        for (int x = -H; x <= H; x++)
            for (int z = -H; z <= H; z++)
                if (x * x + z * z <= 49) b.set(x, 0, z, Blocks.BLUE_ICE);
        for (int i = 0; i < 12; i++) {
            int x = (int) Math.round(10.5 * Math.cos(i * Math.PI / 6)), z = (int) Math.round(10.5 * Math.sin(i * Math.PI / 6));
            b.set(x, 0, z, Blocks.STONE_BRICKS);
            b.set(x, 1, z, Blocks.CAMPFIRE);
        }
    }

    // 7 ---------------------------------------------------------------- Кузня
    static void forge(Brush b) {
        shell(b, Blocks.BRICKS, Blocks.SMOOTH_STONE, null);
        int[][] iron = {{-8, -3}, {-8, 3}, {8, -3}, {8, 3}, {-3, -9}, {3, 9}, {-4, 8}};
        for (int[] p : iron) { b.set(p[0], 1, p[1], Blocks.IRON_BLOCK); if (Brush.hash(p[0], p[1], 7) < 50) b.set(p[0], 2, p[1], Blocks.IRON_BLOCK); }
        int[][] anvils = {{-5, -8}, {5, -8}, {-10, 0}, {10, 0}};
        for (int[] p : anvils) b.set(p[0], 1, p[1], Blocks.ANVIL);
        b.fill(0, 1, 0, 0, 6, 0, Blocks.COPPER_BLOCK); // громоотвод-столб
        b.set(0, 7, 0, Blocks.LIGHTNING_ROD);
        b.set(-10, 1, -4, Blocks.BLAST_FURNACE);
        b.set(10, 1, -4, Blocks.BLAST_FURNACE);
        b.set(-10, 1, 4, Blocks.GRINDSTONE);
        b.set(10, 1, 4, Blocks.SMITHING_TABLE);
    }

    // 8 ---------------------------------------------------------------- Театр Теней
    static void shadowTheatre(Brush b) {
        shell(b, Blocks.BLACK_WOOL, Blocks.SMOOTH_STONE, Blocks.DEEPSLATE_TILES);
        b.fill(-10, 1, -11, 10, 1, -6, Blocks.DARK_OAK_PLANKS); // сцена
        for (int x = -11; x <= 11; x += 2) b.fill(x, 1, -H, x, 8, -H, Blocks.RED_WOOL); // кулисы
        // источники света под разными углами
        int[][] pillars = {{-9, -2}, {9, -2}, {-9, 5}, {9, 5}, {-4, 9}, {4, 9}, {0, 4}, {-5, -4}, {5, -4}};
        for (int i = 0; i < pillars.length; i++) {
            int[] p = pillars[i];
            int h = 2 + (i % 4);
            b.fill(p[0], 1, p[1], p[0], h, p[1], Blocks.POLISHED_BLACKSTONE);
            b.set(p[0], h + 1, p[1], Blocks.SEA_LANTERN);
        }
        for (int i = -9; i <= 9; i += 6) { b.set(i, 7, H, Blocks.GLOWSTONE); b.set(i, 7, -H, Blocks.GLOWSTONE); }
    }

    // 9 ---------------------------------------------------------------- Корневой Колодец
    public static final int[][] ROOT_PLATFORMS = {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};

    static void rootWell(Brush b) {
        shell(b, Blocks.MOSSY_STONE_BRICKS, Blocks.COARSE_DIRT, null);
        for (int x = -H; x <= H; x++)
            for (int z = -H; z <= H; z++) {
                int h = Brush.hash(x, z, 9);
                if (h < 35) b.set(x, 0, z, Blocks.ROOTED_DIRT);
                else if (h < 50) b.set(x, 0, z, Blocks.PODZOL);
            }
        for (int[] p : ROOT_PLATFORMS) {
            b.fill(p[0] - 1, 0, p[1] - 1, p[0] + 1, 1, p[1] + 1, Blocks.STONE_BRICKS);
            b.set(p[0], 2, p[1], Blocks.MOSS_CARPET);
        }
        for (int i = 0; i < 14; i++) {
            int x = -H + Brush.hash(i, 3, 4) * 24 / 100, z = -H + Brush.hash(i, 5, 6) * 24 / 100;
            b.set(x, 1, z, Blocks.HANGING_ROOTS.defaultBlockState());
        }
    }

    // 10 --------------------------------------------------------------- Стеклянный Купол
    public static final int[][] POOLS = {{-8, -2, -4, 2}, {4, -2, 8, 2}};

    static void glassDome(Brush b) {
        shell(b, Blocks.GLASS, Blocks.SMOOTH_SANDSTONE, null);
        for (int[] p : POOLS) {
            b.fill(p[0], -2, p[1], p[2], -1, p[3], Blocks.WATER);
            b.fill(p[0], 0, p[1], p[2], 0, p[3], Blocks.WATER);
        }
        for (int i = -9; i <= 9; i += 6) {
            b.fill(i, 1, -9, i, 6, -9, Blocks.QUARTZ_PILLAR);
            b.fill(i, 1, 9, i, 6, 9, Blocks.QUARTZ_PILLAR);
        }
        b.fill(-9, 7, -9, 9, 7, -9, Blocks.GLASS_PANE);
    }

    // 11 --------------------------------------------------------------- Костёр
    static void bonfire(Brush b) {
        shell(b, Blocks.MUD_BRICKS, Blocks.MUD, null);
        for (int x = -H; x <= H; x++)
            for (int z = -H; z <= H; z++)
                if (Brush.hash(x, z, 11) < 25) b.set(x, 0, z, Blocks.PACKED_MUD);
        b.fill(-3, -2, -3, 3, 0, 3, Blocks.WATER);
        for (int i = 0; i < 16; i++) {
            int x = (int) Math.round(10 * Math.cos(i * Math.PI / 8)), z = (int) Math.round(10 * Math.sin(i * Math.PI / 8));
            b.set(x, 0, z, Blocks.MUD_BRICKS);
            b.set(x, 1, z, Blocks.CAMPFIRE);
        }
    }

    // 12 --------------------------------------------------------------- Арсенал
    static void armoury(Brush b) {
        shell(b, Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_PLANKS);
        for (int i = -9; i <= 9; i += 6) b.set(i, 8, 0, hangingLantern(false));
        for (int i = -6; i <= 6; i += 6) { b.set(i, 8, -6, hangingLantern(false)); b.set(i, 8, 6, hangingLantern(false)); }
        Item[] guns = {Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.STONE_AXE, Items.BOW, Items.TRIDENT, Items.IRON_AXE,
                Items.GOLDEN_SWORD, Items.CROSSBOW, Items.WOODEN_SWORD, Items.DIAMOND_AXE};
        int[][] spots = {{-10, -6}, {-10, -2}, {-10, 2}, {-10, 6}, {10, -6}, {10, -2}, {10, 2}, {10, 6}, {-3, -11}, {3, -11}};
        ServerLevel lvl = b.level;
        for (int i = 0; i < spots.length; i++) {
            b.set(spots[i][0], 1, spots[i][1], Blocks.POLISHED_ANDESITE);
            BlockPos p = b.at(spots[i][0], 2, spots[i][1]);
            ArmorStand st = EntityType.ARMOR_STAND.create(lvl);
            if (st == null) continue;
            double dx = b.center.getX() - p.getX(), dz = b.center.getZ() - p.getZ();
            float yaw = (float) (Mth.atan2(-dx, dz) * 180.0 / Math.PI);
            st.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0);
            st.setShowArms(true);
            st.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(guns[i]));
            lvl.addFreshEntity(st);
        }
    }

    // 13 --------------------------------------------------------------- Фонарный Зал
    static void lanternHall(Brush b) {
        shell(b, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS);
        for (int x = -8; x <= 8; x += 4)
            for (int z = -8; z <= 8; z += 4) {
                if (x == 0 && z == 0) continue;
                b.set(x, 1, z, Blocks.STONE_BRICKS);
                b.set(x, 2, z, Blocks.TORCH);
            }
        for (int x = -6; x <= 6; x += 6)
            for (int z = -6; z <= 6; z += 6) b.set(x, 8, z, hangingLantern(false));
        b.set(0, 8, 0, hangingLantern(false));
    }

    // 14 --------------------------------------------------------------- Зеркальная Арена
    static void mirrorArena(Brush b) {
        shell(b, Blocks.QUARTZ_BRICKS, Blocks.POLISHED_DIORITE, null);
        for (int x = -H; x <= H; x++)
            for (int z = -H; z <= H; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                if (x == 0 || z == 0) b.set(x, 0, z, Blocks.WHITE_CONCRETE);
                else if (d == 4 || d == 8) b.set(x, 0, z, Blocks.LIGHT_BLUE_CONCRETE);
                else if (d == 12) b.set(x, 0, z, Blocks.GOLD_BLOCK);
            }
        b.set(0, 0, 0, Blocks.SEA_LANTERN);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                b.fill(sx * 6, 1, sz * 6, sx * 6, 3, sz * 6, Blocks.QUARTZ_PILLAR);
                b.set(sx * 6, 4, sz * 6, Blocks.SEA_LANTERN);
            }
    }
}

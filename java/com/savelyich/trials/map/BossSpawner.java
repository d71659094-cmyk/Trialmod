package com.savelyich.trials.map;

import com.savelyich.trials.entity.BaseBoss;
import com.savelyich.trials.quest.ArenaLayout;
import com.savelyich.trials.quest.QuestData;
import com.savelyich.trials.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;

public final class BossSpawner {
    private BossSpawner() {}

    public static AABB arenaBox(BlockPos origin, int i) {
        BlockPos c = ArenaLayout.center(origin, i);
        int h = ArenaLayout.HALF + 1;
        return new AABB(c.getX() - h, c.getY() - 9, c.getZ() - h, c.getX() + h + 1, c.getY() + 18, c.getZ() + h + 1);
    }

    public static boolean bossAlive(ServerLevel level, BlockPos origin, int i) {
        return !level.getEntitiesOfClass(BaseBoss.class, arenaBox(origin, i), b -> b.getBossIndex() == i && b.isAlive()).isEmpty();
    }

    /** Создаёт босса арены i, если его там нет. Возвращает true, если пришлось спавнить. */
    public static boolean ensureBoss(ServerLevel level, BlockPos origin, int i) {
        if (i < 0 || i >= QuestData.COUNT) return false;
        BlockPos c = ArenaLayout.center(origin, i);
        level.getChunk(c); // принудительно прогружаем чанк арены
        if (bossAlive(level, origin, i)) return false;
        Brush b = new Brush(level, c, ArenaLayout.gateSide(origin, i));
        BlockPos at = b.at(Arenas.SPAWN[i][0], 1, Arenas.SPAWN[i][1]);
        BaseBoss boss = ModEntities.BOSSES.get(i).get().create(level);
        if (boss == null) return false;
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        boss.setHome(c);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.COMMAND, null, null);
        level.addFreshEntity(boss);
        return true;
    }

    public static int ensureAll(ServerLevel level, BlockPos origin) {
        int n = 0;
        for (int i = 0; i < QuestData.COUNT; i++) if (ensureBoss(level, origin, i)) n++;
        return n;
    }
}

package com.savelyich.trials.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Сохраняет в мире, где стоит карта и достроена ли она. */
public class MapData extends SavedData {
    private static final String NAME = "savelyich_trials_map";

    public BlockPos origin = BlockPos.ZERO;
    public boolean hasOrigin = false;
    public boolean built = false;
    public boolean building = false;

    public static MapData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(MapData::load, MapData::new, NAME);
    }

    public static MapData load(CompoundTag tag) {
        MapData d = new MapData();
        d.hasOrigin = tag.getBoolean("HasOrigin");
        d.built = tag.getBoolean("Built");
        if (d.hasOrigin) d.origin = NbtUtils.readBlockPos(tag.getCompound("Origin"));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("HasOrigin", hasOrigin);
        tag.putBoolean("Built", built);
        tag.put("Origin", NbtUtils.writeBlockPos(origin));
        return tag;
    }

    public void setOrigin(BlockPos pos) {
        this.origin = pos.immutable();
        this.hasOrigin = true;
        setDirty();
    }

    public void setBuilt(boolean b) {
        this.built = b;
        setDirty();
    }
}

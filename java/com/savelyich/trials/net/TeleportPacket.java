package com.savelyich.trials.net;

import com.savelyich.trials.quest.ArenaLayout;
import com.savelyich.trials.quest.MapData;
import com.savelyich.trials.quest.QuestData;
import com.savelyich.trials.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Кнопка «К арене» в журнале: переносит к воротам арены (index) либо в хаб (index = -1). Только для открытых заданий. */
public class TeleportPacket {
    public final int index;

    public TeleportPacket(int index) { this.index = index; }

    public static void encode(TeleportPacket p, FriendlyByteBuf buf) { buf.writeVarInt(p.index); }

    public static TeleportPacket decode(FriendlyByteBuf buf) { return new TeleportPacket(buf.readVarInt()); }

    public static void handle(TeleportPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            MapData md = MapData.get(sp.server.overworld());
            if (!md.hasOrigin || !md.built || sp.level() != sp.server.overworld()) return;
            BlockPos dest;
            if (p.index < 0) {
                dest = md.origin.offset(0, 1, 6);
            } else {
                if (p.index >= QuestData.COUNT) return;
                int st = QuestManager.states(sp)[p.index];
                if (st != QuestData.ACTIVE) return;
                dest = ArenaLayout.gateOutside(md.origin, p.index).above();
            }
            sp.teleportTo(sp.server.overworld(), dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, sp.getYRot(), 0);
        });
        ctx.get().setPacketHandled(true);
    }
}

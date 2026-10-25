package com.savelyich.trials.net;

import com.savelyich.trials.client.ClientQuestState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncQuestsPacket {
    public final int[] states;
    public final BlockPos origin;
    public final boolean mapReady;
    public final boolean met;
    public final boolean finale;

    public SyncQuestsPacket(int[] states, BlockPos origin, boolean mapReady, boolean met, boolean finale) {
        this.states = states;
        this.origin = origin;
        this.mapReady = mapReady;
        this.met = met;
        this.finale = finale;
    }

    public static void encode(SyncQuestsPacket p, FriendlyByteBuf buf) {
        buf.writeVarIntArray(p.states);
        buf.writeBlockPos(p.origin);
        buf.writeBoolean(p.mapReady);
        buf.writeBoolean(p.met);
        buf.writeBoolean(p.finale);
    }

    public static SyncQuestsPacket decode(FriendlyByteBuf buf) {
        return new SyncQuestsPacket(buf.readVarIntArray(), buf.readBlockPos(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(SyncQuestsPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientQuestState.update(p.states, p.origin, p.mapReady, p.met, p.finale)));
        ctx.get().setPacketHandled(true);
    }
}

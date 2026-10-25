package com.savelyich.trials.net;

import com.savelyich.trials.client.ClientQuestState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сервер просит клиент открыть диалог с Савельевичем. */
public class OpenDialogPacket {
    public OpenDialogPacket() {}

    public static void encode(OpenDialogPacket p, FriendlyByteBuf buf) {}

    public static OpenDialogPacket decode(FriendlyByteBuf buf) { return new OpenDialogPacket(); }

    public static void handle(OpenDialogPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> ClientQuestState::openDialog));
        ctx.get().setPacketHandled(true);
    }
}

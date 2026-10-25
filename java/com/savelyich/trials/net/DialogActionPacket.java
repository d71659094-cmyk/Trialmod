package com.savelyich.trials.net;

import com.savelyich.trials.quest.QuestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Действие игрока в диалоге. 0 — знакомство, 1 — взять задание, 2 — сдать задание, 3 — финал просмотрен. */
public class DialogActionPacket {
    public static final int MEET = 0, ACCEPT = 1, REPORT = 2, FINALE = 3;

    public final int action;
    public final int index;

    public DialogActionPacket(int action, int index) {
        this.action = action;
        this.index = index;
    }

    public static void encode(DialogActionPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.action);
        buf.writeVarInt(p.index);
    }

    public static DialogActionPacket decode(FriendlyByteBuf buf) {
        return new DialogActionPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(DialogActionPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            // Защита от подделки: игрок должен стоять рядом с Савельевичем.
            boolean near = !sp.level().getEntitiesOfClass(
                    com.savelyich.trials.entity.SavelyichEntity.class,
                    sp.getBoundingBox().inflate(8)).isEmpty();
            if (!near) return;
            switch (p.action) {
                case MEET -> { QuestManager.setMet(sp); QuestManager.sync(sp); }
                case ACCEPT -> QuestManager.accept(sp, p.index);
                case REPORT -> QuestManager.report(sp, p.index);
                case FINALE -> { QuestManager.setFinaleSeen(sp); QuestManager.sync(sp); }
                default -> { }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

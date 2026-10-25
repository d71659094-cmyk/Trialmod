package com.savelyich.trials.net;

import com.savelyich.trials.TrialsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private Net() {}

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TrialsMod.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SyncQuestsPacket.class, SyncQuestsPacket::encode, SyncQuestsPacket::decode, SyncQuestsPacket::handle);
        CHANNEL.registerMessage(id++, OpenDialogPacket.class, OpenDialogPacket::encode, OpenDialogPacket::decode, OpenDialogPacket::handle);
        CHANNEL.registerMessage(id++, DialogActionPacket.class, DialogActionPacket::encode, DialogActionPacket::decode, DialogActionPacket::handle);
        CHANNEL.registerMessage(id++, TeleportPacket.class, TeleportPacket::encode, TeleportPacket::decode, TeleportPacket::handle);
    }
}

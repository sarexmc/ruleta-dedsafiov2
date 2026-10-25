package com.dedsafio.ruleta.network;

import com.dedsafio.ruleta.RuletaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetHandler {
    private NetHandler() {}

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RuletaMod.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, StartPacket.class, StartPacket::encode, StartPacket::decode, StartPacket::handle);
        CHANNEL.registerMessage(id++, StopPacket.class, StopPacket::encode, StopPacket::decode, StopPacket::handle);
    }
}

package com.dedsafio.ruleta.network;

import com.dedsafio.ruleta.client.ClientRuletaState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StopPacket {
    private final ResourceLocation dim;
    private final BlockPos pos;

    public StopPacket(ResourceLocation dim, BlockPos pos) {
        this.dim = dim;
        this.pos = pos;
    }

    public static void encode(StopPacket p, FriendlyByteBuf buf) {
        buf.writeResourceLocation(p.dim);
        buf.writeBlockPos(p.pos);
    }

    public static StopPacket decode(FriendlyByteBuf buf) {
        return new StopPacket(buf.readResourceLocation(), buf.readBlockPos());
    }

    public static void handle(StopPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientRuletaState.stop(p.dim, p.pos)));
        ctx.get().setPacketHandled(true);
    }
}

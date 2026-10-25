package com.dedsafio.ruleta.network;

import com.dedsafio.ruleta.client.ClientRuletaState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StartPacket {
    private final ResourceLocation dim;
    private final BlockPos pos;
    private final int elapsed;
    private final long seed;

    public StartPacket(ResourceLocation dim, BlockPos pos, int elapsed, long seed) {
        this.dim = dim;
        this.pos = pos;
        this.elapsed = elapsed;
        this.seed = seed;
    }

    public static void encode(StartPacket p, FriendlyByteBuf buf) {
        buf.writeResourceLocation(p.dim);
        buf.writeBlockPos(p.pos);
        buf.writeVarInt(p.elapsed);
        buf.writeLong(p.seed);
    }

    public static StartPacket decode(FriendlyByteBuf buf) {
        return new StartPacket(buf.readResourceLocation(), buf.readBlockPos(), buf.readVarInt(), buf.readLong());
    }

    public static void handle(StartPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientRuletaState.start(p.dim, p.pos, p.elapsed, p.seed)));
        ctx.get().setPacketHandled(true);
    }
}

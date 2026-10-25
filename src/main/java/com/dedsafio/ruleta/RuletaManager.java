package com.dedsafio.ruleta;

import com.dedsafio.ruleta.network.NetHandler;
import com.dedsafio.ruleta.network.StartPacket;
import com.dedsafio.ruleta.network.StopPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Logica de servidor: una sola secuencia por zona, temporizador, resultado y sincronizacion. */
@Mod.EventBusSubscriber(modid = RuletaMod.MODID)
public final class RuletaManager {
    private RuletaManager() {}

    private static final class Seq {
        final ResourceKey<Level> dim;
        final BlockPos pos;
        final long seed;
        int ticks;
        boolean announced;

        Seq(ResourceKey<Level> dim, BlockPos pos, long seed) {
            this.dim = dim;
            this.pos = pos;
            this.seed = seed;
        }
    }

    private static final List<Seq> ACTIVE = new ArrayList<>();

    /** @return false si ya hay una ruleta activa cerca (evita duplicados). */
    public static boolean start(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        for (Seq s : ACTIVE) {
            if (s.dim.equals(dim) && s.pos.distSqr(pos) < 24.0 * 24.0) return false;
        }
        long seed = level.getRandom().nextLong();
        Seq seq = new Seq(dim, pos.immutable(), seed);
        ACTIVE.add(seq);
        StartPacket pkt = new StartPacket(dim.location(), seq.pos, 0, seed);
        NetHandler.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                new PacketDistributor.TargetPoint(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 192.0 * 192.0, dim)), pkt);
        return true;
    }

    /** Acorta la secuencia: cierra la ruleta y restaura la luz. */
    public static int abortAll() {
        int n = 0;
        for (Seq s : ACTIVE) {
            if (s.ticks < Timeline.CLOSE_START) {
                s.ticks = Timeline.CLOSE_START;
                s.announced = true;
                n++;
                NetHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new StopPacket(s.dim.location(), s.pos));
            }
        }
        return n;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        Iterator<Seq> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Seq s = it.next();
            s.ticks++;
            if (!s.announced && s.ticks >= Timeline.SPIN_END) {
                s.announced = true;
                ServerLevel level = e.getServer().getLevel(s.dim);
                if (level != null) announce(level, s);
            }
            if (s.ticks >= Timeline.TOTAL) it.remove();
        }
    }

    /** No se avisa al jugador de que sector salio; solo se lanza el evento para quien quiera usarlo en el servidor. */
    private static void announce(ServerLevel level, Seq s) {
        MinecraftForge.EVENT_BUS.post(new RuletaFinishedEvent(level, s.pos, Timeline.win(s.seed)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) resend(sp);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) resend(sp);
    }

    private static void resend(ServerPlayer sp) {
        for (Seq s : ACTIVE) {
            if (s.dim.equals(sp.level().dimension())) {
                NetHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new StartPacket(s.dim.location(), s.pos, s.ticks, s.seed));
            }
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        ACTIVE.clear();
    }
}

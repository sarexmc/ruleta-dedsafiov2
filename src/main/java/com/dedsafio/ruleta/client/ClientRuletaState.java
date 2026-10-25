package com.dedsafio.ruleta.client;

import com.dedsafio.ruleta.ModSounds;
import com.dedsafio.ruleta.RuletaMod;
import com.dedsafio.ruleta.Timeline;
import com.dedsafio.ruleta.block.RuletaAltarBlock;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Estado y logica de cliente: reloj de la secuencia, sonidos y particulas. */
@Mod.EventBusSubscriber(modid = RuletaMod.MODID, value = Dist.CLIENT)
public final class ClientRuletaState {
    private ClientRuletaState() {}

    public static final int STAR_COUNT = 650;

    public static final class Seq {
        public final ResourceLocation dim;
        public final BlockPos pos;
        public final long seed;
        public final float target;
        public int ticks;
        public int prev;
        public final float[] starDir = new float[STAR_COUNT * 3];
        public final float[] starSize = new float[STAR_COUNT];
        public final float[] starPhase = new float[STAR_COUNT];
        public final int[] starColor = new int[STAR_COUNT];
        public final float[] nebDir = new float[18];
        public final float[] nebPhase = new float[6];

        Seq(ResourceLocation dim, BlockPos pos, int elapsed, long seed) {
            this.dim = dim;
            this.pos = pos;
            this.seed = seed;
            this.ticks = elapsed;
            this.prev = elapsed;
            this.target = Timeline.targetAngle(seed);
            Random r = new Random(seed * 31L + 7L);
            for (int i = 0; i < STAR_COUNT; i++) {
                float y = 0.06f + (float) Math.pow(r.nextFloat(), 0.8f) * 0.94f;
                float ang = r.nextFloat() * Timeline.TWO_PI;
                float h = (float) Math.sqrt(1f - y * y);
                starDir[i * 3] = h * (float) Math.cos(ang);
                starDir[i * 3 + 1] = y;
                starDir[i * 3 + 2] = h * (float) Math.sin(ang);
                float q = r.nextFloat();
                starSize[i] = q < 0.82f ? 0.05f + r.nextFloat() * 0.10f : (q < 0.97f ? 0.16f + r.nextFloat() * 0.14f : 0.32f + r.nextFloat() * 0.22f);
                starPhase[i] = r.nextFloat() * 20f;
                starColor[i] = r.nextInt(8);
            }
            for (int i = 0; i < 6; i++) {
                float y = 0.15f + r.nextFloat() * 0.55f;
                float ang = r.nextFloat() * Timeline.TWO_PI;
                float h = (float) Math.sqrt(1f - y * y);
                nebDir[i * 3] = h * (float) Math.cos(ang);
                nebDir[i * 3 + 1] = y;
                nebDir[i * 3 + 2] = h * (float) Math.sin(ang);
                nebPhase[i] = r.nextFloat() * 6f;
            }
        }

        public float cx() { return pos.getX() + 0.5f; }
        public float cz() { return pos.getZ() + 0.5f; }
        public float groundY() { return pos.getY() + 0.03f; }
    }

    public static final List<Seq> ACTIVE = new ArrayList<>();

    public static void start(ResourceLocation dim, BlockPos pos, int elapsed, long seed) {
        ACTIVE.removeIf(s -> s.pos.equals(pos) && s.dim.equals(dim));
        ACTIVE.add(new Seq(dim, pos, elapsed, seed));
    }

    public static void stop(ResourceLocation dim, BlockPos pos) {
        for (Seq s : ACTIVE) {
            if (s.pos.equals(pos) && s.dim.equals(dim) && s.ticks < Timeline.CLOSE_START) {
                s.ticks = Timeline.CLOSE_START;
                s.prev = Timeline.CLOSE_START;
            }
        }
    }

    public static boolean isAltar(ClientLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof RuletaAltarBlock;
    }

    public static float glowY(ClientLevel level, Seq s) {
        return s.pos.getY() + (isAltar(level, s.pos) ? 2.15f : 1.2f);
    }

    private static CloudStatus savedClouds = null;

    /** Las nubes del juego se dibujan encima de la oscuridad: se ocultan mientras dura la secuencia. */
    private static void updateClouds(Minecraft mc, boolean off) {
        if (off) {
            if (savedClouds == null && mc.options.cloudStatus().get() != CloudStatus.OFF) {
                savedClouds = mc.options.cloudStatus().get();
                mc.options.cloudStatus().set(CloudStatus.OFF);
            }
        } else if (savedClouds != null) {
            mc.options.cloudStatus().set(savedClouds);
            savedClouds = null;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ACTIVE.clear();
        updateClouds(Minecraft.getInstance(), false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE.clear();
            updateClouds(mc, false);
            return;
        }
        if (ACTIVE.isEmpty()) {
            updateClouds(mc, false);
            return;
        }
        if (mc.isPaused()) return;
        ResourceLocation cur = mc.level.dimension().location();
        Iterator<Seq> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Seq s = it.next();
            if (!s.dim.equals(cur)) {
                it.remove();
                continue;
            }
            s.prev = s.ticks;
            s.ticks++;
            if (s.ticks >= Timeline.TOTAL) {
                it.remove();
                continue;
            }
            cues(mc.level, s);
            particles(mc.level, s);
        }
        boolean dark = false;
        for (Seq q : ACTIVE) if (Timeline.darkness(q.ticks) > 0.3f) dark = true;
        updateClouds(mc, dark);
    }

    private static boolean cross(Seq s, int t) {
        return s.prev < t && s.ticks >= t;
    }

    private static void snd(ClientLevel level, Seq s, SoundEvent ev, float vol, float pitch) {
        level.playLocalSound(s.cx(), s.pos.getY() + 1.0, s.cz(), ev, SoundSource.AMBIENT, vol, pitch, false);
    }

    private static void cues(ClientLevel level, Seq s) {
        if (cross(s, 1)) snd(level, s, ModSounds.USE.get(), 2.0f, 1.0f);
        if (cross(s, Timeline.OPEN_START)) snd(level, s, ModSounds.BOOM.get(), 4.0f, 1.0f);
        if (cross(s, Timeline.SPIN_START)) snd(level, s, ModSounds.BED.get(), 3.0f, 1.0f);
        if (cross(s, Timeline.CLOSE_START)) snd(level, s, ModSounds.THUMP.get(), 4.0f, 1.0f);
        if (cross(s, Timeline.FINALE_AT)) snd(level, s, ModSounds.FINALE.get(), 3.5f, 1.0f);
    }

    private static void particles(ClientLevel level, Seq s) {
        RandomSource rnd = level.random;
        int t = s.ticks;
        double cx = s.cx(), cz = s.cz();
        double gy = glowY(level, s);
        double gnd = s.groundY() + 0.1;

        if (cross(s, Timeline.ORB_BOOST)) {
            for (int i = 0; i < 90; i++) {
                double dx = rnd.nextGaussian(), dy = rnd.nextGaussian(), dz = rnd.nextGaussian();
                double len = Math.max(0.001, Math.sqrt(dx * dx + dy * dy + dz * dz));
                double sp = 0.15 + rnd.nextDouble() * 0.55;
                level.addParticle(ParticleTypes.END_ROD, cx, gy, cz, dx / len * sp, dy / len * sp, dz / len * sp);
            }
        }
        if (t > Timeline.ORB_BOOST && t < Timeline.ORB_BOOST + 25) {
            for (int i = 0; i < 4; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                double sp = 0.1 + rnd.nextDouble() * 0.25;
                level.addParticle(ParticleTypes.END_ROD, cx, gy, cz, Math.cos(a) * sp, (rnd.nextDouble() - 0.3) * 0.2, Math.sin(a) * sp);
            }
        }
        if (t >= Timeline.OPEN_START && t < Timeline.CLOSE_START + 30) {
            float maxR = Timeline.R * Timeline.maxRadiusFactor(t);
            if (maxR > 0.5f) {
                float rot = Timeline.rotation(t, s.target);
                int n = t < Timeline.SPIN_START ? 3 : 7;
                for (int i = 0; i < n; i++) {
                    float r = (float) Math.sqrt(rnd.nextFloat()) * maxR;
                    float ang = rnd.nextFloat() * Timeline.TWO_PI;
                    float local = ((ang - rot) % Timeline.TWO_PI + Timeline.TWO_PI) % Timeline.TWO_PI;
                    int sec = Math.min(Timeline.SECTORS - 1, (int) (local / Timeline.SECTOR_ANGLE));
                    float[] c = Timeline.COLORS[sec];
                    double x = cx + r * Math.cos(ang), z = cz + r * Math.sin(ang);
                    level.addParticle(new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 1.3f),
                            x, gnd, z, 0, 0.03 + rnd.nextDouble() * 0.06, 0);
                    if (rnd.nextInt(4) == 0) {
                        level.addParticle(ParticleTypes.END_ROD, x, gnd + 0.2, z, 0, 0.02 + rnd.nextDouble() * 0.04, 0);
                    }
                }
            }
        }
        if (t >= Timeline.ORB_BOOST && t < Timeline.CORE_FADE_START) {
            for (int i = 0; i < 2; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, rr = 0.2 + rnd.nextDouble() * 1.4;
                float[] c = rnd.nextBoolean() ? new float[]{0.2f, 0.95f, 0.95f} : new float[]{1f, 0.9f, 0.3f};
                level.addParticle(new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 0.8f),
                        cx + Math.cos(a) * rr, gy + (rnd.nextDouble() - 0.4) * 1.2, cz + Math.sin(a) * rr,
                        (rnd.nextDouble() - 0.5) * 0.04, 0.01 + rnd.nextDouble() * 0.04, (rnd.nextDouble() - 0.5) * 0.04);
            }
        }
        if (t >= Timeline.RING_START && t < Timeline.RING_START + Timeline.RING_DUR) {
            float u = (t - Timeline.RING_START) / (float) Timeline.RING_DUR;
            float rad = Timeline.ringRadius(u);
            for (int i = 0; i < 8; i++) {
                float a = rnd.nextFloat() * Timeline.TWO_PI;
                float[] c = RuletaRenderer.hsv(rnd.nextFloat());
                level.addParticle(new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 1.2f),
                        cx + rad * Math.cos(a), gnd, cz + rad * Math.sin(a), 0, 0.03, 0);
            }
        }
    }
}

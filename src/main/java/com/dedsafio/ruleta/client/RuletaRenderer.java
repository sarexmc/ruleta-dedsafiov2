package com.dedsafio.ruleta.client;

import com.dedsafio.ruleta.RuletaMod;
import com.dedsafio.ruleta.Timeline;
import com.dedsafio.ruleta.block.RuletaAltarBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Dibujo de la secuencia: cielo oscuro con estrellas/nebulosa, destello VFX, disco que gira, hub arcoiris y anillo final. */
@Mod.EventBusSubscriber(modid = RuletaMod.MODID, value = Dist.CLIENT)
public final class RuletaRenderer {
    private RuletaRenderer() {}

    private static final int RINGS = 18;
    private static final int SUB = 8;
    private static final float[][] NEB = {{0.45f, 0.15f, 0.9f}, {0.15f, 0.3f, 1f}, {0.1f, 0.7f, 0.85f}, {0.85f, 0.2f, 0.7f}, {0.3f, 0.2f, 0.9f}, {0.1f, 0.5f, 0.9f}};

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ClientRuletaState.ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack ps = e.getPoseStack();
        float pt = e.getPartialTick();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        Matrix4f m = ps.last().pose();
        Vector3f look = cam.getLookVector();
        Vector3f up = cam.getUpVector();
        Vector3f left = cam.getLeftVector();
        Vector3f right = new Vector3f(left).negate();

        float dark = 0f;
        for (ClientRuletaState.Seq s : ClientRuletaState.ACTIVE) {
            float t = s.ticks + pt;
            double dx = cp.x - s.cx(), dz = cp.z - s.cz();
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            float prox = Timeline.clamp01(1f - (dist - 48f) / 48f);
            dark = Math.max(dark, Timeline.darkness(t) * prox);
        }
        if (dark > 0.002f) {
            VertexConsumer vc = buf.getBuffer(RuletaRenderTypes.OVERLAY);
            screenQuad(vc, m, look, up, left, 0f, 0f, 0.012f, 0.985f * dark);
            buf.endBatch(RuletaRenderTypes.OVERLAY);
        }

        if (dark > 0.05f) {
            for (ClientRuletaState.Seq s : ClientRuletaState.ACTIVE) {
                BlockState st = level.getBlockState(s.pos);
                if (st.getBlock() instanceof RuletaAltarBlock) {
                    ps.pushPose();
                    ps.translate(s.pos.getX() - cp.x, s.pos.getY() - cp.y, s.pos.getZ() - cp.z);
                    mc.getBlockRenderer().renderSingleBlock(st, ps, buf, 0xF000F0, OverlayTexture.NO_OVERLAY);
                    ps.popPose();
                    buf.endBatch();
                }
            }
        }

        for (ClientRuletaState.Seq s : ClientRuletaState.ACTIVE) {
            float t = s.ticks + pt;
            if (t > Timeline.TOTAL) continue;
            float ox = (float) (s.cx() - cp.x);
            float oy = (float) (s.groundY() - cp.y);
            float oz = (float) (s.cz() - cp.z);
            float gy = (float) (ClientRuletaState.glowY(level, s) - cp.y);
            float d = Timeline.darkness(t);
            boolean altar = ClientRuletaState.isAltar(level, s.pos);

            // cielo: nebulosa muy tenue + campo de estrellas (puntos pequenos, algunas con destello de 4 puntas)
            if (d > 0.3f) {
                VertexConsumer g = buf.getBuffer(RuletaRenderTypes.GLOW);
                for (int i = 0; i < 6; i++) {
                    float[] c = NEB[i];
                    float pul = 0.7f + 0.3f * (float) Math.sin(t * 0.03f + s.nebPhase[i]);
                    softGlow(g, m, ox + s.nebDir[i * 3] * 70f, gy + s.nebDir[i * 3 + 1] * 70f, oz + s.nebDir[i * 3 + 2] * 70f,
                            right, up, 26f, 0.07f * d * pul, c[0], c[1], c[2], 0f, t, 5, 12);
                }
                for (int i = 0; i < ClientRuletaState.STAR_COUNT; i++) {
                    float tw = 0.45f + 0.55f * (0.5f + 0.5f * (float) Math.sin(t * 0.2f + s.starPhase[i]));
                    float a = tw * d;
                    float[] c = starColor(s.starColor[i]);
                    float sx = ox + s.starDir[i * 3] * 70f;
                    float sy = gy + s.starDir[i * 3 + 1] * 70f;
                    float sz = oz + s.starDir[i * 3 + 2] * 70f;
                    float sz0 = s.starSize[i];
                    starPoint(g, m, sx, sy, sz, right, up, sz0 * 2.4f, c, a);
                    if (sz0 > 0.15f) {
                        starPoint(g, m, sx, sy, sz, right, up, sz0 * 6f, c, a * 0.18f);
                        spikes(g, m, sx, sy, sz, right, up, sz0 * (sz0 > 0.3f ? 16f : 8f), a * 0.85f, t, 0.045f, c);
                    }
                }
                buf.endBatch(RuletaRenderTypes.GLOW);
            }

            float openHub = Timeline.smooth((t - Timeline.OPEN_START) / 30f)
                    * (1f - Timeline.smooth((t - Timeline.CLOSE_START - 30f) / 30f));

            if (Timeline.maxRadiusFactor(t) > 0.002f) {
                float rot = Timeline.rotation(t, s.target);
                // capa 1: nebulosa texturizada, translucida de dia y saturada de noche
                VertexConsumer tex = buf.getBuffer(RuletaRenderTypes.DISC_TEX);
                disc(tex, m, s, t, rot, ox, oy, oz, d, false);
                buf.endBatch(RuletaRenderTypes.DISC_TEX);
                // capa 2: bordes dorados, anillos y centro arcoiris
                VertexConsumer solid = buf.getBuffer(RuletaRenderTypes.DISC);
                trims(solid, m, t, rot, ox, oy + 0.006f, oz, false);
                hub(solid, m, ox, oy + 0.012f, oz, t, openHub, 0.9f);
                buf.endBatch(RuletaRenderTypes.DISC);
                // capa 3: venas brillantes de la nebulosa (aditivo)
                VertexConsumer vein = buf.getBuffer(RuletaRenderTypes.GLOW_TEX);
                disc(vein, m, s, t, rot, ox, oy + 0.03f, oz, d, true);
                buf.endBatch(RuletaRenderTypes.GLOW_TEX);
                VertexConsumer add = buf.getBuffer(RuletaRenderTypes.GLOW);
                trims(add, m, t, rot, ox, oy + 0.035f, oz, true);
                hub(add, m, ox, oy + 0.04f, oz, t, openHub, 0.5f);
                pointer(add, m, t, ox, oy + 0.05f, oz);
                buf.endBatch(RuletaRenderTypes.GLOW);
            }

            float u = Timeline.ringProgress(t);
            if (u > 0f && u < 1.2f) {
                VertexConsumer add = buf.getBuffer(RuletaRenderTypes.GLOW);
                rainbowRing(add, m, ox, oy + 0.05f, oz, u, t, 1.0f);
                rainbowRing(add, m, ox, oy + 0.06f, oz, u - 0.14f, t, 0.55f);
                buf.endBatch(RuletaRenderTypes.GLOW);
            }

            if (altar) {
                VertexConsumer add = buf.getBuffer(RuletaRenderTypes.GLOW);
                altarGlow(add, m, (float) (s.pos.getX() + 0.5 - cp.x), (float) (s.pos.getY() - cp.y), (float) (s.pos.getZ() + 0.5 - cp.z), t, d);
                buf.endBatch(RuletaRenderTypes.GLOW);
            }

            // VFX central: orbe de luz (bloom suave) con destellos de 4 puntas
            float flash = Timeline.flash(t);
            float core = Timeline.core(t);
            float inten = Math.max(flash, core * 0.85f);
            if (inten > 0.01f) {
                VertexConsumer add = buf.getBuffer(RuletaRenderTypes.GLOW);
                float pulse = 1f + 0.1f * (float) Math.sin(t * 0.4f);
                float radius = 0.55f + 1.9f * flash + 0.55f * core * pulse;
                softGlow(add, m, ox, gy, oz, right, up, radius * 1.8f, inten * 0.35f, 1f, 0.95f, 0.9f, 0.35f, t, 8, 28);
                softGlow(add, m, ox, gy, oz, right, up, radius, inten, 1f, 1f, 1f, 0.2f, t, 8, 28);
                softGlow(add, m, ox, gy, oz, right, up, radius * 0.4f, Math.min(1f, inten * 1.3f), 1f, 1f, 1f, 0f, t, 4, 20);
                spikes(add, m, ox, gy, oz, right, up, radius * 2.4f, inten * 0.7f, t, 0.025f, new float[]{1f, 1f, 1f});
                buf.endBatch(RuletaRenderTypes.GLOW);
            }
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (ClientRuletaState.ACTIVE.isEmpty()) return;
        Vec3 cp = e.getCamera().getPosition();
        float amp = 0f, tt = 0f;
        for (ClientRuletaState.Seq s : ClientRuletaState.ACTIVE) {
            float t = s.ticks + (float) e.getPartialTick();
            double dx = cp.x - s.cx(), dz = cp.z - s.cz();
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            float a = Timeline.shake(t) * Timeline.clamp01(1f - (dist - 16f) / 32f);
            if (a > amp) {
                amp = a;
                tt = t;
            }
        }
        if (amp <= 0f) return;
        float k = 1.3f * amp;
        e.setYaw(e.getYaw() + (float) Math.sin(tt * 2.9f + 2f) * k);
        e.setPitch(e.getPitch() + (float) Math.sin(tt * 2.3f + 1f) * k);
        e.setRoll(e.getRoll() + (float) Math.sin(tt * 1.7f) * k * 0.7f);
    }

    // ------------------------------------------------------------------ utilidades

    private static void screenQuad(VertexConsumer vc, Matrix4f m, Vector3f look, Vector3f up, Vector3f left,
                                   float r, float g, float b, float a) {
        float d = 0.25f, S = 4f;
        float cx = look.x * d, cy = look.y * d, cz = look.z * d;
        v(vc, m, cx + left.x * S + up.x * S, cy + left.y * S + up.y * S, cz + left.z * S + up.z * S, r, g, b, a);
        v(vc, m, cx - left.x * S + up.x * S, cy - left.y * S + up.y * S, cz - left.z * S + up.z * S, r, g, b, a);
        v(vc, m, cx - left.x * S - up.x * S, cy - left.y * S - up.y * S, cz - left.z * S - up.z * S, r, g, b, a);
        v(vc, m, cx + left.x * S - up.x * S, cy + left.y * S - up.y * S, cz + left.z * S - up.z * S, r, g, b, a);
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).color(clamp(r), clamp(g), clamp(b), clamp(a)).endVertex();
    }

    private static float clamp(float x) { return x < 0f ? 0f : (x > 1f ? 1f : x); }

    // ------------------------------------------------------------------ disco

    private static void tv(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v,
                           float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).uv(u, v).color(clamp(r), clamp(g), clamp(b), clamp(a)).endVertex();
    }

    /** Sectores texturizados. Todos giran con el mismo angulo 'rot'; la textura va pegada al disco (rota con el). */
    private static void disc(VertexConsumer vc, Matrix4f m, ClientRuletaState.Seq s, float t, float rot,
                             float ox, float oy, float oz, float dark, boolean glow) {
        float S = Timeline.SECTOR_ANGLE;
        for (int i = 0; i < Timeline.SECTORS; i++) {
            float rf = Timeline.radiusFactor(i, t);
            float wf = Timeline.widthFactor(i, t);
            if (rf <= 0.002f || wf <= 0.002f) continue;
            float rMax = Timeline.R * rf;
            float half = (S / 2f - Timeline.GAP) * wf;
            float mid = i * S + S / 2f;
            float[] col = Timeline.COLORS[i];
            for (int k = 0; k < RINGS; k++) {
                float r0 = rMax * k / RINGS, r1 = rMax * (k + 1) / RINGS;
                for (int j = 0; j < SUB; j++) {
                    float a0 = mid - half + 2f * half * j / SUB;
                    float a1 = mid - half + 2f * half * (j + 1) / SUB;
                    dv(vc, m, ox, oy, oz, r0, a0, rot, col, t, rMax, mid, half, dark, glow);
                    dv(vc, m, ox, oy, oz, r0, a1, rot, col, t, rMax, mid, half, dark, glow);
                    dv(vc, m, ox, oy, oz, r1, a1, rot, col, t, rMax, mid, half, dark, glow);
                    dv(vc, m, ox, oy, oz, r1, a0, rot, col, t, rMax, mid, half, dark, glow);
                }
            }
        }
    }

    private static void dv(VertexConsumer vc, Matrix4f m, float ox, float oy, float oz, float r, float a, float rot,
                           float[] col, float t, float rMax, float mid, float half, float dark, boolean glow) {
        float wa = a + rot;
        float x = ox + r * (float) Math.cos(wa);
        float z = oz + r * (float) Math.sin(wa);
        float lx = r * (float) Math.cos(a), lz = r * (float) Math.sin(a);
        float u = lx * 0.11f + t * 0.0016f, v = lz * 0.11f - t * 0.0011f;
        float lat = Math.abs(a - mid) / Math.max(half, 0.0001f);
        float ef = r / Math.max(rMax, 0.0001f);
        float cg = Math.max(0f, 1f - r / (Timeline.R * 0.4f));
        float edge = ef > 0.85f ? 1f - (ef - 0.85f) / 0.15f * 0.55f : 1f;
        if (glow) {
            float k = (0.30f + 0.55f * dark) * (1f - 0.7f * lat * lat * lat);
            tv(vc, m, x, oy, z, u, v, col[0] * k * 1.3f, col[1] * k * 1.3f, col[2] * k * 1.3f, 0.6f * edge);
        } else {
            float b = 0.95f * (1f - 0.55f * lat * lat * lat);
            float alpha = (0.52f + 0.42f * dark) * edge;
            tv(vc, m, x, oy, z, u, v, col[0] * b + 0.35f * cg, col[1] * b + 0.35f * cg, col[2] * b + 0.35f * cg, alpha);
        }
    }

    /** Borde dorado exterior de cada sector y anillo interior alrededor del centro. */
    private static void trims(VertexConsumer vc, Matrix4f m, float t, float rot, float ox, float oy, float oz, boolean glow) {
        float S = Timeline.SECTOR_ANGLE;
        for (int i = 0; i < Timeline.SECTORS; i++) {
            float rf = Timeline.radiusFactor(i, t);
            float wf = Timeline.widthFactor(i, t);
            if (rf <= 0.002f || wf <= 0.002f) continue;
            float rMax = Timeline.R * rf;
            float half = (S / 2f - Timeline.GAP) * wf;
            float mid = i * S + S / 2f;
            float rd = Timeline.HUB_R + 0.22f;
            for (int j = 0; j < SUB; j++) {
                float a0 = mid - half + 2f * half * j / SUB;
                float a1 = mid - half + 2f * half * (j + 1) / SUB;
                float al = glow ? 0.55f : 0.95f;
                band(vc, m, ox, oy, oz, rMax - 0.1f, rMax, a0, a1, rot, 1f, 0.93f, 0.62f, al);
                if (rMax > rd + 0.3f) band(vc, m, ox, oy, oz, Timeline.HUB_R + 0.1f, rd, a0, a1, rot, 1f, 0.9f, 0.55f, al * 0.85f);
            }
        }
    }

    private static void band(VertexConsumer vc, Matrix4f m, float ox, float oy, float oz, float r0, float r1,
                             float a0, float a1, float rot, float cr, float cg, float cb, float al) {
        float w0 = a0 + rot, w1 = a1 + rot;
        v(vc, m, ox + r0 * (float) Math.cos(w0), oy, oz + r0 * (float) Math.sin(w0), cr, cg, cb, al);
        v(vc, m, ox + r0 * (float) Math.cos(w1), oy, oz + r0 * (float) Math.sin(w1), cr, cg, cb, al);
        v(vc, m, ox + r1 * (float) Math.cos(w1), oy, oz + r1 * (float) Math.sin(w1), cr, cg, cb, al);
        v(vc, m, ox + r1 * (float) Math.cos(w0), oy, oz + r1 * (float) Math.sin(w0), cr, cg, cb, al);
    }

    /** Centro arcoiris que gira en sentido contrario al disco. */
    private static void hub(VertexConsumer vc, Matrix4f m, float ox, float oy, float oz, float t, float open, float alpha) {
        if (open <= 0.01f) return;
        float rad = Timeline.HUB_R * open;
        int N = 40;
        for (int k = 0; k < 3; k++) {
            float r0 = rad * k / 3f, r1 = rad * (k + 1) / 3f;
            float w0 = 1f - 0.45f * k / 3f, w1 = 1f - 0.45f * (k + 1) / 3f;
            for (int j = 0; j < N; j++) {
                float a0 = Timeline.TWO_PI * j / N, a1 = Timeline.TWO_PI * (j + 1) / N;
                float[] c0 = hsv((float) j / N - t * 0.02f), c1 = hsv((float) (j + 1) / N - t * 0.02f);
                hv(vc, m, ox, oy, oz, r0, a0, c0, w0, alpha);
                hv(vc, m, ox, oy, oz, r0, a1, c1, w0, alpha);
                hv(vc, m, ox, oy, oz, r1, a1, c1, w1, alpha);
                hv(vc, m, ox, oy, oz, r1, a0, c0, w1, alpha);
            }
        }
    }

    private static void hv(VertexConsumer vc, Matrix4f m, float ox, float oy, float oz, float r, float a, float[] c, float w, float alpha) {
        float wh = Math.max(0f, w - 0.55f) * 1.4f;
        v(vc, m, ox + r * (float) Math.cos(a), oy, oz + r * (float) Math.sin(a), c[0] * w + wh * 0.5f, c[1] * w + wh * 0.5f, c[2] * w + wh * 0.5f, alpha);
    }

    private static void pointer(VertexConsumer vc, Matrix4f m, float t, float ox, float oy, float oz) {
        float rf = Timeline.maxRadiusFactor(t);
        if (rf < 0.3f) return;
        float a = Timeline.clamp01((rf - 0.3f) * 3f);
        float rr = Timeline.R * rf;
        v(vc, m, ox, oy, oz - (rr - 0.45f), 1f, 1f, 1f, a);
        v(vc, m, ox - 0.35f, oy, oz - (rr + 0.4f), 1f, 0.95f, 0.7f, a * 0.6f);
        v(vc, m, ox, oy, oz - (rr + 0.12f), 1f, 1f, 1f, a * 0.8f);
        v(vc, m, ox + 0.35f, oy, oz - (rr + 0.4f), 1f, 0.95f, 0.7f, a * 0.6f);
    }

    // ------------------------------------------------------------------ altar

    /** Lineas de neon cian del altar (emisivas, con luz que sube). */
    private static void altarGlow(VertexConsumer vc, Matrix4f m, float bx, float by, float bz, float t, float dark) {
        float k = 0.4f + 0.6f * dark;
        int[][] faces = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        float[][] spans = {{0.2f, 1.0f}, {1.08f, 1.95f}};
        int SEG = 6;
        for (int[] f : faces) {
            float nx = f[0], nz = f[1];
            float tx = -nz, tz = nx;
            for (int side = -1; side <= 1; side += 2) {
                float l0 = side * 0.185f, l1 = side * 0.22f;
                for (float[] sp : spans) {
                    for (int sg = 0; sg < SEG; sg++) {
                        float y0 = sp[0] + (sp[1] - sp[0]) * sg / SEG, y1 = sp[0] + (sp[1] - sp[0]) * (sg + 1) / SEG;
                        float a0 = k * (0.4f + 0.6f * (0.5f + 0.5f * (float) Math.sin(t * 0.2f - y0 * 4f)));
                        float a1 = k * (0.4f + 0.6f * (0.5f + 0.5f * (float) Math.sin(t * 0.2f - y1 * 4f)));
                        float px0 = bx + nx * 0.262f + tx * l0, pz0 = bz + nz * 0.262f + tz * l0;
                        float px1 = bx + nx * 0.262f + tx * l1, pz1 = bz + nz * 0.262f + tz * l1;
                        v(vc, m, px0, by + y0, pz0, 0.15f, 0.95f, 0.9f, a0);
                        v(vc, m, px1, by + y0, pz1, 0.15f, 0.95f, 0.9f, a0);
                        v(vc, m, px1, by + y1, pz1, 0.15f, 0.95f, 0.9f, a1);
                        v(vc, m, px0, by + y1, pz0, 0.15f, 0.95f, 0.9f, a1);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ anillo / VFX

    private static void rainbowRing(VertexConsumer vc, Matrix4f m, float ox, float oy, float oz, float u, float t, float strength) {
        if (u <= 0f || u >= 1.2f) return;
        float uu = Timeline.clamp01(u);
        float rad = Timeline.ringRadius(uu);
        float thick = 0.95f * (1f - uu * 0.5f);
        float fadeIn = Timeline.clamp01(uu / 0.12f);
        float alpha = (float) Math.pow(1f - uu, 1.1) * fadeIn * strength;
        if (alpha <= 0.002f) return;
        int N = 90;
        float[] rs = {rad - thick, rad - thick * 0.5f, rad, rad + thick * 0.5f, rad + thick};
        float[] al = {0f, 0.55f, 1f, 0.55f, 0f};
        for (int j = 0; j < N; j++) {
            float a0 = Timeline.TWO_PI * j / N, a1 = Timeline.TWO_PI * (j + 1) / N;
            float[] c0 = pastel(((float) j / N + t * 0.012f) % 1f), c1 = pastel(((float) (j + 1) / N + t * 0.012f) % 1f);
            float sw0 = 0.6f + 0.4f * (float) Math.sin(a0 * 2f - t * 0.12f), sw1 = 0.6f + 0.4f * (float) Math.sin(a1 * 2f - t * 0.12f);
            for (int q = 0; q < 4; q++) {
                v(vc, m, ox + rs[q] * (float) Math.cos(a0), oy, oz + rs[q] * (float) Math.sin(a0), c0[0], c0[1], c0[2], alpha * al[q] * sw0);
                v(vc, m, ox + rs[q + 1] * (float) Math.cos(a0), oy, oz + rs[q + 1] * (float) Math.sin(a0), c0[0], c0[1], c0[2], alpha * al[q + 1] * sw0);
                v(vc, m, ox + rs[q + 1] * (float) Math.cos(a1), oy, oz + rs[q + 1] * (float) Math.sin(a1), c1[0], c1[1], c1[2], alpha * al[q + 1] * sw1);
                v(vc, m, ox + rs[q] * (float) Math.cos(a1), oy, oz + rs[q] * (float) Math.sin(a1), c1[0], c1[1], c1[2], alpha * al[q] * sw1);
            }
        }
    }

    private static float[] pastel(float h) {
        float[] c = hsv(h);
        return new float[]{0.55f + 0.45f * c[0], 0.55f + 0.45f * c[1], 0.55f + 0.45f * c[2]};
    }

    private static void ringStrip(VertexConsumer vc, Matrix4f m, float ox, float oy, float oz,
                                  float rIn, float aIn, float rOut, float aOut, float a0, float a1, float[] c0, float[] c1) {
        v(vc, m, ox + rIn * (float) Math.cos(a0), oy, oz + rIn * (float) Math.sin(a0), c0[0], c0[1], c0[2], aIn);
        v(vc, m, ox + rOut * (float) Math.cos(a0), oy, oz + rOut * (float) Math.sin(a0), c0[0], c0[1], c0[2], aOut);
        v(vc, m, ox + rOut * (float) Math.cos(a1), oy, oz + rOut * (float) Math.sin(a1), c1[0], c1[1], c1[2], aOut);
        v(vc, m, ox + rIn * (float) Math.cos(a1), oy, oz + rIn * (float) Math.sin(a1), c1[0], c1[1], c1[2], aIn);
    }

    /** Resplandor suave (curva gaussiana, sin borde duro). rainbow 0..1 mezcla el color base con un halo arcoiris giratorio. */
    private static void softGlow(VertexConsumer vc, Matrix4f m, float x, float y, float z, Vector3f right, Vector3f up,
                                 float radius, float inten, float br, float bg, float bb, float rainbow, float t, int K, int N) {
        for (int k = 0; k < K; k++) {
            float f0 = (float) k / K, f1 = (float) (k + 1) / K;
            float a0 = inten * (float) Math.exp(-Math.pow(f0 * 3.0, 2)), a1 = inten * (float) Math.exp(-Math.pow(f1 * 3.0, 2));
            for (int j = 0; j < N; j++) {
                float t0 = Timeline.TWO_PI * j / N, t1 = Timeline.TWO_PI * (j + 1) / N;
                gv(vc, m, x, y, z, right, up, radius * f0, t0, a0, br, bg, bb, rainbow, f0, t);
                gv(vc, m, x, y, z, right, up, radius * f1, t0, a1, br, bg, bb, rainbow, f1, t);
                gv(vc, m, x, y, z, right, up, radius * f1, t1, a1, br, bg, bb, rainbow, f1, t);
                gv(vc, m, x, y, z, right, up, radius * f0, t1, a0, br, bg, bb, rainbow, f0, t);
            }
        }
    }

    private static void gv(VertexConsumer vc, Matrix4f m, float x, float y, float z, Vector3f right, Vector3f up,
                           float rad, float th, float alpha, float br, float bg, float bb, float rainbow, float f, float t) {
        float c = (float) Math.cos(th), s = (float) Math.sin(th);
        float[] h = hsv(th / Timeline.TWO_PI + t * 0.02f);
        float mix = rainbow * Timeline.smooth(f * 1.4f);
        v(vc, m, x + (right.x * c + up.x * s) * rad, y + (right.y * c + up.y * s) * rad, z + (right.z * c + up.z * s) * rad,
                br + (h[0] - br) * mix, bg + (h[1] - bg) * mix, bb + (h[2] - bb) * mix, alpha);
    }

    private static void starPoint(VertexConsumer vc, Matrix4f m, float x, float y, float z, Vector3f right, Vector3f up,
                                  float size, float[] c, float a) {
        int N = 5;
        for (int j = 0; j < N; j++) {
            float t0 = Timeline.TWO_PI * j / N, t1 = Timeline.TWO_PI * (j + 1) / N;
            v(vc, m, x, y, z, c[0], c[1], c[2], a);
            v(vc, m, x + (right.x * (float) Math.cos(t0) + up.x * (float) Math.sin(t0)) * size, y + (right.y * (float) Math.cos(t0) + up.y * (float) Math.sin(t0)) * size,
                    z + (right.z * (float) Math.cos(t0) + up.z * (float) Math.sin(t0)) * size, c[0], c[1], c[2], 0f);
            v(vc, m, x + (right.x * (float) Math.cos(t1) + up.x * (float) Math.sin(t1)) * size, y + (right.y * (float) Math.cos(t1) + up.y * (float) Math.sin(t1)) * size,
                    z + (right.z * (float) Math.cos(t1) + up.z * (float) Math.sin(t1)) * size, c[0], c[1], c[2], 0f);
            v(vc, m, x, y, z, c[0], c[1], c[2], a);
        }
    }

    private static void spikes(VertexConsumer vc, Matrix4f m, float x, float y, float z, Vector3f right, Vector3f up,
                               float len, float inten, float t, float widthFactor, float[] col) {
        for (int k = 0; k < 4; k++) {
            float ang = t * 0.01f + k * (float) (Math.PI / 4.0);
            float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
            float dx = right.x * c + up.x * s, dy = right.y * c + up.y * s, dz = right.z * c + up.z * s;
            float px = -right.x * s + up.x * c, py = -right.y * s + up.y * c, pz = -right.z * s + up.z * c;
            float L = (k % 2 == 0) ? len : len * 0.6f;
            float w = L * widthFactor;
            v(vc, m, x + dx * L, y + dy * L, z + dz * L, col[0], col[1], col[2], 0f);
            v(vc, m, x + px * w, y + py * w, z + pz * w, col[0], col[1], col[2], inten);
            v(vc, m, x - dx * L, y - dy * L, z - dz * L, col[0], col[1], col[2], 0f);
            v(vc, m, x - px * w, y - py * w, z - pz * w, col[0], col[1], col[2], inten);
        }
    }

    public static float[] hsv(float h) {
        h = (h % 1f + 1f) % 1f;
        float x = h * 6f;
        int i = (int) x;
        float f = x - i;
        float q = 1f - f;
        switch (i % 6) {
            case 0: return new float[]{1f, f, 0f};
            case 1: return new float[]{q, 1f, 0f};
            case 2: return new float[]{0f, 1f, f};
            case 3: return new float[]{0f, q, 1f};
            case 4: return new float[]{f, 0f, 1f};
            default: return new float[]{1f, 0f, q};
        }
    }

    private static float[] starColor(int i) {
        switch (i) {
            case 0: case 1: return new float[]{1f, 1f, 1f};
            case 2: return new float[]{1f, 0.45f, 0.95f};
            case 3: return new float[]{0.75f, 0.4f, 1f};
            case 4: return new float[]{0.45f, 0.65f, 1f};
            case 5: return new float[]{1f, 0.8f, 0.9f};
            case 6: return new float[]{0.8f, 0.9f, 1f};
            default: return new float[]{1f, 0.95f, 0.75f};
        }
    }
}

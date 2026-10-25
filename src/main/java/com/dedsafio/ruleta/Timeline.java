package com.dedsafio.ruleta;

import java.util.Random;

/** Linea de tiempo compartida (servidor y cliente). Todo en ticks (20 = 1 s). */
public final class Timeline {
    private Timeline() {}

    public static final int SECTORS = 7;
    public static final float TWO_PI = (float) (Math.PI * 2.0);
    public static final float SECTOR_ANGLE = TWO_PI / SECTORS;
    public static final float GAP = 0.02f;
    public static final float R = 5.5f;
    public static final float HUB_R = 0.9f;
    public static final float POINTER_ANGLE = (float) (-Math.PI / 2.0);
    public static final int TURNS = 7;

    // Linea de tiempo calcada del video: ping (t=0) -> golpe grave + apertura diurna -> zumbido -> oscuridad con estrellas
    // -> cierre con golpe -> orbe + anillo -> vuelve el dia.
    public static final int OPEN_START = 12;
    public static final int OPEN_DUR = 45;
    public static final int OPEN_STAGGER = 3;
    public static final int SPIN_START = OPEN_START + 20;
    public static final int ACCEL = 45;
    public static final int CRUISE = 55;
    public static final int DECEL = 100;
    public static final int SPIN_END = SPIN_START + ACCEL + CRUISE + DECEL;
    public static final int DARK_START = OPEN_START + 50;
    public static final int DARK_IN = 30;
    public static final int FLASH_START = DARK_START + DARK_IN + 2;
    public static final int CLOSE_START = SPIN_END + 10;
    public static final int CLOSE_DUR = 40;
    public static final int ORB_BOOST = CLOSE_START + 15;
    public static final int RING_START = CLOSE_START + 20;
    public static final int RING_DUR = 95;
    public static final int RESTORE_START = CLOSE_START + 45;
    public static final int RESTORE_DUR = 40;
    public static final int CORE_FADE_START = CLOSE_START + 110;
    public static final int CORE_FADE_DUR = 40;
    public static final int FINALE_AT = CLOSE_START + 40;
    public static final int TOTAL = CORE_FADE_START + CORE_FADE_DUR + 5;

    /** Orden horario visto desde arriba. */
    public static final float[][] COLORS = {
            {0.15f, 0.95f, 0.25f}, // verde
            {0.10f, 0.85f, 0.95f}, // cian
            {0.15f, 0.20f, 1.00f}, // azul
            {0.70f, 0.20f, 0.95f}, // morado
            {0.95f, 0.10f, 0.25f}, // rojo
            {1.00f, 0.55f, 0.10f}, // naranja
            {1.00f, 0.90f, 0.10f}  // amarillo
    };
    public static final String[] NAMES = {"Verde", "Cian", "Azul", "Morado", "Rojo", "Naranja", "Amarillo"};

    public static float clamp01(float x) { return x < 0f ? 0f : (x > 1f ? 1f : x); }
    public static float smooth(float x) { x = clamp01(x); return x * x * (3f - 2f * x); }
    public static float easeOutCubic(float x) { x = clamp01(x); float i = 1f - x; return 1f - i * i * i; }
    public static float easeInOut(float x) { return smooth(x); }

    public static float radiusFactor(int i, float t) {
        float op = clamp01((t - OPEN_START - i * OPEN_STAGGER) / OPEN_DUR);
        float cl = clamp01((t - CLOSE_START - (SECTORS - 1 - i) * OPEN_STAGGER) / (float) CLOSE_DUR);
        return easeOutCubic(op) * (1f - easeInOut(cl));
    }

    public static float widthFactor(int i, float t) {
        float op = clamp01((t - OPEN_START - i * OPEN_STAGGER) / (OPEN_DUR * 0.6f));
        float cl = clamp01((t - CLOSE_START - (SECTORS - 1 - i) * OPEN_STAGGER) / (float) CLOSE_DUR);
        cl = clamp01((cl - 0.45f) / 0.55f);
        return smooth(op) * (1f - smooth(cl));
    }

    public static float maxRadiusFactor(float t) {
        float m = 0f;
        for (int i = 0; i < SECTORS; i++) m = Math.max(m, radiusFactor(i, t));
        return m;
    }

    public static float darkness(float t) {
        if (t <= DARK_START) return 0f;
        if (t < RESTORE_START) return smooth((t - DARK_START) / DARK_IN);
        return 1f - smooth((t - RESTORE_START) / RESTORE_DUR);
    }

    private static float pulse(float u, float rise, float fall) {
        if (u < 0f) return 0f;
        if (u < rise) return u / rise;
        float f = 1f - (u - rise) / fall;
        return f <= 0f ? 0f : f * f;
    }

    /** Destello suave al llegar la oscuridad (0.55) y gran destello del orbe al cerrar (1.0). */
    public static float flash(float t) {
        return Math.max(0.55f * pulse(t - FLASH_START, 4f, 30f), pulse(t - ORB_BOOST, 5f, 45f));
    }

    public static float core(float t) {
        float base = 0.35f * smooth((t - FLASH_START) / 30f);
        float boost = smooth((t - ORB_BOOST) / 15f);
        float out = 1f - smooth((t - CORE_FADE_START) / CORE_FADE_DUR);
        return Math.max(base, boost) * out;
    }

    public static float shake(float t) {
        float a = Math.min(1f, 0.8f * pulse(t - OPEN_START, 3f, 22f) + 0f);
        float b = 0.5f * pulse(t - FLASH_START, 3f, 18f);
        float c = 0.7f * pulse(t - CLOSE_START, 3f, 20f);
        return Math.max(a, Math.max(b, c));
    }

    public static float ringProgress(float t) { return (t - RING_START) / RING_DUR; }
    public static float ringRadius(float u) { return 0.5f + 5.5f * easeOutCubic(u); }

    public static int win(long seed) { return new Random(seed).nextInt(SECTORS); }

    /** Angulo total (rad) que recorre el disco: TURNS vueltas + ajuste para parar con el puntero en el sector ganador. */
    public static float targetAngle(long seed) {
        int w = win(seed);
        Random r = new Random(seed ^ 0x9E3779B9L);
        r.nextInt(SECTORS);
        float off = (r.nextFloat() - 0.5f) * 0.6f;
        float local = (w + 0.5f + off) * SECTOR_ANGLE;
        float base = POINTER_ANGLE - local;
        base = ((base % TWO_PI) + TWO_PI) % TWO_PI;
        return TURNS * TWO_PI + base;
    }

    /** Angulo real del disco en el instante t (continuo, aceleracion/desaceleracion suaves). */
    public static float rotation(float t, float target) {
        float s = t - SPIN_START;
        if (s <= 0f) return 0f;
        float wmax = target / (ACCEL / 2f + CRUISE + DECEL / 2f);
        if (s < ACCEL) {
            float u = s / ACCEL;
            return wmax * ACCEL * (u * u * u - u * u * u * u / 2f);
        }
        float a1 = wmax * ACCEL * 0.5f;
        s -= ACCEL;
        if (s < CRUISE) return a1 + wmax * s;
        float a2 = a1 + wmax * CRUISE;
        s -= CRUISE;
        if (s < DECEL) {
            float u = s / DECEL;
            return a2 + wmax * DECEL * (u - (u * u * u - u * u * u * u / 2f));
        }
        return target;
    }

    public static int sectorAtPointer(float rot) {
        float local = POINTER_ANGLE - rot;
        local = ((local % TWO_PI) + TWO_PI) % TWO_PI;
        return Math.min(SECTORS - 1, (int) (local / SECTOR_ANGLE));
    }
}

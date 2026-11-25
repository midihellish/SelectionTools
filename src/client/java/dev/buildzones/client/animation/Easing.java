package dev.buildzones.client.animation;

/** Curvas de tempo. Entrada e saída entre 0 e 1. */
public final class Easing {

    private Easing() {
    }

    private static double clamp01(double t) {
        return Math.max(0.0, Math.min(1.0, t));
    }

    /** Entrada: começa rápido e desacelera. */
    public static double easeOutCubic(double t) {
        double u = 1.0 - clamp01(t);
        return 1.0 - u * u * u;
    }

    /** Saída: começa devagar e acelera. */
    public static double easeInCubic(double t) {
        double u = clamp01(t);
        return u * u * u;
    }
}

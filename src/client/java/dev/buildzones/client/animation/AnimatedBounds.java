package dev.buildzones.client.animation;

import net.minecraft.util.math.Box;

/**
 * Caixa interpolada só para apresentação. Não altera as coordenadas reais da seleção.
 * A interpolação usa o tempo decorrido (dt), então a velocidade é igual em qualquer FPS.
 */
public final class AnimatedBounds {
    /** Taxa de convergência por segundo: ~95% do caminho em 130 ms. */
    private static final double RATE = 22.0;

    private double[] current;

    public void reset() {
        current = null;
    }

    public boolean seeded() {
        return current != null;
    }

    /** Define o ponto de partida, por exemplo a caixa do canto A, para a expansão começar dali. */
    public void seed(Box start) {
        current = toArray(start);
    }

    /** Aproxima a caixa do alvo. Na primeira chamada, aparece direto no alvo. */
    public Box approach(Box target, double deltaSeconds) {
        double[] goal = toArray(target);
        if (current == null) {
            current = goal;
            return target;
        }
        double factor = 1.0 - Math.exp(-RATE * Math.max(0.0, deltaSeconds));
        for (int i = 0; i < 6; i++) {
            current[i] += (goal[i] - current[i]) * factor;
        }
        return toBox(current);
    }

    private static double[] toArray(Box b) {
        return new double[]{b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ};
    }

    private static Box toBox(double[] v) {
        return new Box(v[0], v[1], v[2], v[3], v[4], v[5]);
    }
}

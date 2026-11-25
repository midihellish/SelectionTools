package dev.buildzones.client.axis;

import dev.buildzones.client.selection.SelectionAxis;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Escolhe o eixo do scroll pelo componente dominante do vetor de visão.
 *
 * Histerese: o eixo atual só troca quando outro eixo supera o atual pela
 * margem. Sem isso, a câmera perto de uma diagonal alterna o eixo a cada frame.
 */
public final class AxisResolver {
    /** Histerese da escolha por face, em blocos ao longo do raio. */
    private static final double FACE_MARGIN = 0.15;

    private final double margin;

    public AxisResolver(double margin) {
        this.margin = margin;
    }

    public SelectionAxis resolve(Vec3d look, SelectionAxis previous) {
        SelectionAxis best = SelectionAxis.X;
        double bestValue = Math.abs(look.x);
        double y = Math.abs(look.y);
        if (y > bestValue) {
            best = SelectionAxis.Y;
            bestValue = y;
        }
        double z = Math.abs(look.z);
        if (z > bestValue) {
            best = SelectionAxis.Z;
            bestValue = z;
        }

        if (previous == null || best == previous) {
            return best;
        }
        double previousValue = Math.abs(previous.component(look));
        return bestValue > previousValue + margin ? best : previous;
    }

    /**
     * Eixo da face da caixa que o olhar atinge. De fora, é a face de entrada; de dentro, a face à frente.
     * Se o raio não toca a caixa, cai no eixo dominante do olhar. Histerese em blocos de distância ao longo
     * do raio: perto da quina entre duas faces, o eixo atual só troca se o outro passar à frente por mais
     * que {@code FACE_MARGIN}.
     *
     * @param look direção do olhar, normalizada
     */
    public SelectionAxis resolveFace(Vec3d eye, Vec3d look, Box box, SelectionAxis previous) {
        double[] origin = {eye.x, eye.y, eye.z};
        double[] dir = {look.x, look.y, look.z};
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};
        double[] near = new double[3];
        double[] far = new double[3];
        for (int i = 0; i < 3; i++) {
            if (Math.abs(dir[i]) < 1.0E-9) {
                if (origin[i] < min[i] || origin[i] > max[i]) {
                    return resolve(look, previous);
                }
                near[i] = Double.NEGATIVE_INFINITY;
                far[i] = Double.POSITIVE_INFINITY;
            } else {
                double t1 = (min[i] - origin[i]) / dir[i];
                double t2 = (max[i] - origin[i]) / dir[i];
                near[i] = Math.min(t1, t2);
                far[i] = Math.max(t1, t2);
            }
        }
        double enter = Math.max(near[0], Math.max(near[1], near[2]));
        double exit = Math.min(far[0], Math.min(far[1], far[2]));
        if (enter > exit || exit < 0) {
            return resolve(look, previous);
        }
        boolean inside = enter < 0;
        double[] t = inside ? far : near;
        int best = 0;
        for (int i = 1; i < 3; i++) {
            if (inside ? t[i] < t[best] : t[i] > t[best]) {
                best = i;
            }
        }
        if (previous != null && previous.ordinal() != best) {
            double gap = inside ? t[previous.ordinal()] - t[best] : t[best] - t[previous.ordinal()];
            if (gap < FACE_MARGIN) {
                return previous;
            }
        }
        return SelectionAxis.values()[best];
    }
}

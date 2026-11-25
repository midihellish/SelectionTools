package dev.buildzones.client.axis;

import dev.buildzones.client.selection.SelectionAxis;
import net.minecraft.util.math.Vec3d;

/**
 * Escolhe o eixo do scroll pelo componente dominante do vetor de visão.
 *
 * Histerese: o eixo atual só troca quando outro eixo supera o atual pela
 * margem. Sem isso, a câmera perto de uma diagonal alterna o eixo a cada frame.
 */
public final class AxisResolver {
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
}

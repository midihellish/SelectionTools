package dev.buildzones.client.axis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildzones.client.selection.SelectionAxis;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

class AxisResolverTest {
    private final AxisResolver resolver = new AxisResolver(0.15);

    @Test
    void dominantComponentWins() {
        assertEquals(SelectionAxis.Y, resolver.resolve(new Vec3d(0.1, 0.95, 0.2), null));
        assertEquals(SelectionAxis.X, resolver.resolve(new Vec3d(-0.9, 0.1, 0.3), null));
        assertEquals(SelectionAxis.Z, resolver.resolve(new Vec3d(0.2, -0.1, 0.97), null));
    }

    @Test
    void nearDiagonalKeepsPreviousAxis() {
        // Câmera quase na diagonal: X levemente maior que Y, mas dentro da margem.
        Vec3d look = new Vec3d(0.72, 0.69, 0.0);
        assertEquals(SelectionAxis.Y, resolver.resolve(look, SelectionAxis.Y));
    }

    @Test
    void clearlyDominantAxisTakesOver() {
        Vec3d look = new Vec3d(0.95, 0.2, 0.0);
        assertEquals(SelectionAxis.X, resolver.resolve(look, SelectionAxis.Y));
    }

    @Test
    void smallOscillationsDoNotFlipAxisRepeatedly() {
        SelectionAxis current = SelectionAxis.Z;
        double[] xs = {0.70, 0.72, 0.69, 0.71, 0.68, 0.73};
        for (double x : xs) {
            current = resolver.resolve(new Vec3d(x, 0.0, 0.70), current);
            assertEquals(SelectionAxis.Z, current);
        }
    }
}

package dev.buildzones.client.axis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildzones.client.selection.SelectionAxis;
import net.minecraft.util.math.Box;
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

    private static final Box BOX = new Box(0, 0, 0, 4, 4, 4);

    @Test
    void faceFromOutsideIsTheEntryFace() {
        // De cima, olhando para baixo: entra pela face de cima (Y), mesmo com o olhar inclinado.
        assertEquals(SelectionAxis.Y,
                resolver.resolveFace(new Vec3d(2, 10, 2), new Vec3d(0.3, -0.95, 0.0).normalize(), BOX, null));
        // De lado, olhando ao longo de X: entra pela face de X.
        assertEquals(SelectionAxis.X,
                resolver.resolveFace(new Vec3d(-6, 2, 2), new Vec3d(1, -0.1, 0.05).normalize(), BOX, null));
    }

    @Test
    void faceDiffersFromDominantAxisWhenGrazingTheBox() {
        // O olhar é dominantemente X, mas o raio só encosta na caixa pela face de cima.
        Vec3d eye = new Vec3d(-1.0, 6, 2);
        Vec3d look = new Vec3d(0.9, -0.45, 0.0).normalize();
        assertEquals(SelectionAxis.X, resolver.resolve(look, null));
        assertEquals(SelectionAxis.Y, resolver.resolveFace(eye, look, BOX, null));
    }

    @Test
    void faceFromInsideIsTheFaceAhead() {
        assertEquals(SelectionAxis.Z,
                resolver.resolveFace(new Vec3d(2, 2, 2), new Vec3d(0.1, 0.1, 1).normalize(), BOX, null));
    }

    @Test
    void rayThatMissesTheBoxFallsBackToDominantAxis() {
        assertEquals(SelectionAxis.X,
                resolver.resolveFace(new Vec3d(-6, 20, 2), new Vec3d(1, 0, 0), BOX, null));
    }

    @Test
    void nearAnEdgeKeepsPreviousFace() {
        // Quase na quina entre a face de cima e a da frente: mantém a que já estava.
        Vec3d eye = new Vec3d(-6, 10, 2);
        Vec3d look = new Vec3d(6.0, -6.05, 0).normalize();
        assertEquals(SelectionAxis.X, resolver.resolveFace(eye, look, BOX, SelectionAxis.X));
        assertEquals(SelectionAxis.Y, resolver.resolveFace(eye, look, BOX, SelectionAxis.Y));
    }
}

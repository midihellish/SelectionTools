package dev.buildzones.client.radial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadialLayoutTest {
    private static final double INNER = 36.0;

    @Test
    void centerDeadZoneSelectsNothing() {
        assertEquals(-1, RadialLayout.sliceAt(0, 0, INNER));
        assertEquals(-1, RadialLayout.sliceAt(10, -10, INNER));
    }

    @Test
    void slicesStartAtTopAndGoClockwise() {
        assertEquals(0, RadialLayout.sliceAt(0, -80, INNER), "topo");
        assertEquals(1, RadialLayout.sliceAt(80, 0, INNER), "direita");
        assertEquals(2, RadialLayout.sliceAt(0, 80, INNER), "baixo");
        assertEquals(3, RadialLayout.sliceAt(-80, 0, INNER), "esquerda");
    }

    @Test
    void nearDiagonalsFallInTheSliceOnEachSide() {
        // A divisa entre fatias fica em -135, -45, 45 e 135 graus. Pontos a 5 graus de cada lado não dependem de arredondamento.
        assertEquals(0, RadialLayout.sliceAt(cos(-50), sin(-50), INNER));
        assertEquals(1, RadialLayout.sliceAt(cos(-40), sin(-40), INNER));
        assertEquals(3, RadialLayout.sliceAt(cos(-140), sin(-140), INNER));
    }

    private static double cos(double deg) {
        return Math.cos(Math.toRadians(deg)) * 80;
    }

    private static double sin(double deg) {
        return Math.sin(Math.toRadians(deg)) * 80;
    }

    @Test
    void everyDirectionMapsToAValidSlice() {
        for (int deg = -180; deg < 180; deg += 5) {
            double rad = Math.toRadians(deg);
            int slice = RadialLayout.sliceAt(Math.cos(rad) * 80, Math.sin(rad) * 80, INNER);
            assertTrue(slice >= 0 && slice < RadialLayout.SLICES, "grau " + deg);
        }
    }

    @Test
    void centerAngleOfEachSliceFallsInsideIt() {
        for (int slice = 0; slice < RadialLayout.SLICES; slice++) {
            double rad = Math.toRadians(RadialLayout.centerDegrees(slice));
            assertEquals(slice, RadialLayout.sliceAt(Math.cos(rad) * 80, Math.sin(rad) * 80, INNER));
        }
    }
}

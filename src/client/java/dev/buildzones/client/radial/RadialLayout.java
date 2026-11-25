package dev.buildzones.client.radial;

/**
 * Geometria pura do menu radial, em coordenadas de tela (y cresce para baixo).
 * Fatia 0 centrada no topo, depois sentido horário. Cada fatia ocupa 90 graus.
 */
public final class RadialLayout {
    public static final int SLICES = 4;

    private static final double SLICE_DEGREES = 360.0 / SLICES;

    private RadialLayout() {
    }

    /**
     * Fatia sob o ponto (dx, dy) relativo ao centro do menu.
     *
     * @return índice da fatia, ou -1 dentro do raio interno (centro morto)
     */
    public static int sliceAt(double dx, double dy, double innerRadius) {
        if (Math.hypot(dx, dy) < innerRadius) {
            return -1;
        }
        double degrees = Math.toDegrees(Math.atan2(dy, dx));
        // A fatia 0 começa em -135 graus (topo-esquerda) e cada fatia avança 90 graus no sentido horário.
        double shifted = degrees + 135.0;
        if (shifted < 0) {
            shifted += 360.0;
        }
        int slice = (int) (shifted / SLICE_DEGREES);
        return Math.min(SLICES - 1, slice);
    }

    /** Ângulo central da fatia, em graus no mesmo sistema de {@link #sliceAt}. */
    public static double centerDegrees(int slice) {
        return -90.0 + SLICE_DEGREES * slice;
    }
}

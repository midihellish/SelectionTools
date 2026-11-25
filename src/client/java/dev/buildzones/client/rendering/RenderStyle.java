package dev.buildzones.client.rendering;

/**
 * Paleta e intensidades do renderer. Nenhum número de cor ou alpha aparece
 * fora desta classe. Cores em RGB; alpha de 0 a 255.
 */
public final class RenderStyle {
    /** Região confirmada: ciano. */
    public static final int REGION_RGB = 0x53DDF2;
    /** Região em prévia: turquesa claro. */
    public static final int PREVIEW_RGB = 0x8FFFF0;
    /** Eixo ativo: âmbar suave, para não se confundir com as caixas. */
    public static final int AXIS_RGB = 0xFFC66D;
    /** Marcador do canto A. */
    public static final int ANCHOR_RGB = 0xFFFFFF;
    /** Marcador do canto B. */
    public static final int OPPOSITE_RGB = 0x00E5D0;
    /** Fantasma do pincel de área: azul claro, diferente da região para não confundir. */
    public static final int AREA_GHOST_RGB = 0x9BE7FF;
    /** Contorno de remoção: bloco que não pertence ao alvo. */
    public static final int AREA_REMOVE_RGB = 0xFF5555;

    public static final int FILL_ALPHA = 34;
    public static final int EDGE_ALPHA = 210;
    public static final int PREVIEW_FILL_ALPHA = 58;
    public static final int PREVIEW_EDGE_ALPHA = 255;
    public static final int AREA_GHOST_FILL_ALPHA = 60;
    public static final int AREA_GHOST_EDGE_ALPHA = 170;
    public static final int AREA_REMOVE_EDGE_ALPHA = 230;
    /** Alpha extra durante o pulso de confirmação. */
    public static final int PULSE_BOOST = 90;

    public static final double MARKER_SIZE = 0.10;
    public static final double ANCHOR_MARKER_SIZE = 0.22;

    private RenderStyle() {
    }

    /** Combina RGB e alpha em ARGB. */
    public static int argb(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }
}

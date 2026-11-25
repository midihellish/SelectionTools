package dev.buildzones.client.radial;

import net.minecraft.client.gui.DrawContext;

/**
 * Pixel art de cada fatia, em grade de 8 × 8. Cada caractere é um pixel: {@code #} acende, {@code .} fica vazio.
 * Desenhado com retângulos, sem textura, para não depender de arquivos de imagem.
 */
public final class RadialIcons {
    /** Lado de cada pixel na tela, em pixels de interface. */
    public static final int PIXEL = 2;
    /** Lado total do ícone na tela. */
    public static final int SIZE = 8 * PIXEL;

    private static final String[] BUILD = {
            "........",
            "...##...",
            "...##...",
            ".##.##..",
            ".##.##..",
            "........",
            "........",
            "........",
    };
    private static final String[] HOLLOW = {
            "........",
            ".######.",
            ".#....#.",
            ".#....#.",
            ".#....#.",
            ".#....#.",
            ".######.",
            "........",
    };
    private static final String[] EDGES = {
            "........",
            "#......#",
            ".#....#.",
            "..#..#..",
            "..#..#..",
            ".#....#.",
            "#......#",
            "........",
    };
    private static final String[] FILL = {
            "........",
            ".######.",
            ".######.",
            ".######.",
            ".######.",
            ".######.",
            "........",
            "........",
    };

    private RadialIcons() {
    }

    /** Desenha o ícone da opção centrado em (cx, cy). */
    public static void draw(DrawContext context, RadialOption option, int cx, int cy, int argb) {
        String[] rows = switch (option) {
            case BUILD -> BUILD;
            case HOLLOW -> HOLLOW;
            case EDGES -> EDGES;
            case FILL -> FILL;
        };
        int x0 = cx - SIZE / 2;
        int y0 = cy - SIZE / 2;
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                if (rows[row].charAt(col) == '#') {
                    int x = x0 + col * PIXEL;
                    int y = y0 + row * PIXEL;
                    context.fill(x, y, x + PIXEL, y + PIXEL, argb);
                }
            }
        }
    }
}

package dev.buildzones.client.selection;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;

/**
 * Volume inclusivo entre dois cantos. Pura: não usa render nem MinecraftClient.
 *
 * Os blocos dos dois cantos pertencem à seleção, então a dimensão mínima é 1.
 */
public record SelectionBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public static SelectionBounds between(BlockPos a, BlockPos b) {
        return new SelectionBounds(
                Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public int sizeX() {
        return maxX - minX + 1;
    }

    public int sizeY() {
        return maxY - minY + 1;
    }

    public int sizeZ() {
        return maxZ - minZ + 1;
    }

    public int size(SelectionAxis axis) {
        return switch (axis) {
            case X -> sizeX();
            case Y -> sizeY();
            case Z -> sizeZ();
        };
    }

    /** Usa long para que o produto de três dimensões não estoure. */
    public long volume() {
        return (long) sizeX() * sizeY() * sizeZ();
    }

    /** Caixa em coordenadas de mundo: mínimo no menor bloco e máximo na face externa do último. */
    public Box toBox() {
        return new Box(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
    }
}

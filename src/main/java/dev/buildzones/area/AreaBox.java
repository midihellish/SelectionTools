package dev.buildzones.area;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.util.math.BlockPos;

/**
 * Volume inclusivo entre dois cantos, em coordenadas de bloco. Compartilhado por
 * cliente e servidor. Não conhece input nem render.
 */
public record AreaBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public static AreaBox between(BlockPos a, BlockPos b) {
        return new AreaBox(
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

    /** Usa long para que o produto de três dimensões não estoure. */
    public long volume() {
        return (long) sizeX() * sizeY() * sizeZ();
    }

    public boolean contains(BlockPos p) {
        return p.getX() >= minX && p.getX() <= maxX
                && p.getY() >= minY && p.getY() <= maxY
                && p.getZ() >= minZ && p.getZ() <= maxZ;
    }

    /** Dimensões dentro do limite. Usado para recusar regiões absurdas vindas da rede. */
    public boolean fitsDimension(int max) {
        return sizeX() <= max && sizeY() <= max && sizeZ() <= max;
    }

    /** Percorre todas as células do volume, uma vez cada, em X, depois Z, depois Y. */
    public Iterator<BlockPos> cells() {
        return new CellIterator(this);
    }

    private static final class CellIterator implements Iterator<BlockPos> {
        private final AreaBox box;
        private int x;
        private int y;
        private int z;
        private boolean done;

        CellIterator(AreaBox box) {
            this.box = box;
            this.x = box.minX;
            this.y = box.minY;
            this.z = box.minZ;
        }

        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public BlockPos next() {
            if (done) {
                throw new NoSuchElementException();
            }
            BlockPos current = new BlockPos(x, y, z);
            if (++x > box.maxX) {
                x = box.minX;
                if (++z > box.maxZ) {
                    z = box.minZ;
                    if (++y > box.maxY) {
                        done = true;
                    }
                }
            }
            return current;
        }
    }
}

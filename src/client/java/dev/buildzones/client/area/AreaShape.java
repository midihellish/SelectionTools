package dev.buildzones.client.area;

import dev.buildzones.area.AreaBox;
import net.minecraft.util.math.BlockPos;

/**
 * Forma do pincel de área. O servidor só conhece o volume; a forma vale no cliente, que decide
 * quais células viram fantasma. Por isso o pincel nunca oferece células de dentro do oco.
 */
public enum AreaShape {
    /** Volume inteiro. */
    FILL,
    /** Só a casca: células em alguma face da caixa. */
    HOLLOW,
    /** Só as arestas: células em pelo menos duas faces (cantos incluídos). */
    EDGES;

    /** Verdadeiro se a célula, dentro da caixa, pertence à forma. */
    public boolean contains(AreaBox box, BlockPos pos) {
        if (!box.contains(pos)) {
            return false;
        }
        if (this == FILL) {
            return true;
        }
        int faces = 0;
        if (pos.getX() == box.minX() || pos.getX() == box.maxX()) {
            faces++;
        }
        if (pos.getY() == box.minY() || pos.getY() == box.maxY()) {
            faces++;
        }
        if (pos.getZ() == box.minZ() || pos.getZ() == box.maxZ()) {
            faces++;
        }
        return this == HOLLOW ? faces >= 1 : faces >= 2;
    }
}

package dev.buildzones.client.area;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildzones.area.AreaBox;
import java.util.Iterator;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class AreaShapeTest {
    private static final AreaBox CUBE = new AreaBox(0, 0, 0, 2, 2, 2);

    private static long count(AreaShape shape, AreaBox box) {
        long n = 0;
        Iterator<BlockPos> cells = box.cells();
        while (cells.hasNext()) {
            if (shape.contains(box, cells.next())) {
                n++;
            }
        }
        return n;
    }

    @Test
    void fillCoversTheWholeVolume() {
        assertEquals(27, count(AreaShape.FILL, CUBE));
    }

    @Test
    void hollowLeavesOnlyTheInteriorOut() {
        // 3x3x3 menos o centro.
        assertEquals(26, count(AreaShape.HOLLOW, CUBE));
        assertEquals(false, AreaShape.HOLLOW.contains(CUBE, new BlockPos(1, 1, 1)));
    }

    @Test
    void edgesAreCellsOnAtLeastTwoFaces() {
        // 12 arestas de um cubo 3x3x3: 8 cantos + 12 meios de aresta.
        assertEquals(20, count(AreaShape.EDGES, CUBE));
        assertEquals(false, AreaShape.EDGES.contains(CUBE, new BlockPos(1, 0, 1)), "meio de face não é aresta");
        assertEquals(true, AreaShape.EDGES.contains(CUBE, new BlockPos(1, 2, 0)));
    }

    @Test
    void flatBoxIsAllFaceForHollowAndAllEdgeForEdges() {
        AreaBox flat = new AreaBox(0, 5, 0, 3, 5, 3);
        assertEquals(16, count(AreaShape.FILL, flat));
        assertEquals(16, count(AreaShape.HOLLOW, flat));
        assertEquals(12, count(AreaShape.EDGES, flat), "a borda externa de uma placa 4x4");
    }

    @Test
    void cellsOutsideTheBoxNeverBelong() {
        assertEquals(false, AreaShape.FILL.contains(CUBE, new BlockPos(3, 0, 0)));
    }
}

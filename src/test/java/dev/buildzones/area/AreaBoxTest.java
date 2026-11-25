package dev.buildzones.area;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class AreaBoxTest {

    @Test
    void betweenNormalizesCornersInAnyOrder() {
        AreaBox box = AreaBox.between(new BlockPos(5, 10, -3), new BlockPos(2, 12, 4));
        assertEquals(new AreaBox(2, 10, -3, 5, 12, 4), box);
        assertEquals(4, box.sizeX());
        assertEquals(3, box.sizeY());
        assertEquals(8, box.sizeZ());
    }

    @Test
    void volumeCountsBothEndsInclusively() {
        AreaBox box = AreaBox.between(new BlockPos(0, 0, 0), new BlockPos(15, 15, 15));
        assertEquals(4096L, box.volume());
    }

    @Test
    void cellsVisitEveryPositionExactlyOnce() {
        AreaBox box = AreaBox.between(new BlockPos(-2, 60, 1), new BlockPos(1, 62, 3));
        Set<BlockPos> seen = new HashSet<>();
        int count = 0;
        Iterator<BlockPos> it = box.cells();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            assertTrue(box.contains(pos), "fora da caixa: " + pos);
            seen.add(pos);
            count++;
        }
        assertEquals(box.volume(), count);
        assertEquals(box.volume(), seen.size());
    }

    @Test
    void cellsOfSingleBlockYieldOneCell() {
        AreaBox box = AreaBox.between(new BlockPos(7, 7, 7), new BlockPos(7, 7, 7));
        Iterator<BlockPos> it = box.cells();
        assertTrue(it.hasNext());
        assertEquals(new BlockPos(7, 7, 7), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void containsIsInclusiveOnBothEdges() {
        AreaBox box = AreaBox.between(new BlockPos(0, 0, 0), new BlockPos(3, 3, 3));
        assertTrue(box.contains(new BlockPos(0, 0, 0)));
        assertTrue(box.contains(new BlockPos(3, 3, 3)));
        assertFalse(box.contains(new BlockPos(4, 0, 0)));
        assertFalse(box.contains(new BlockPos(0, -1, 0)));
    }

    @Test
    void fitsDimensionRejectsOversizedAxis() {
        AreaBox ok = AreaBox.between(new BlockPos(0, 0, 0), new BlockPos(511, 511, 511));
        AreaBox tooBig = AreaBox.between(new BlockPos(0, 0, 0), new BlockPos(512, 0, 0));
        assertTrue(ok.fitsDimension(512));
        assertFalse(tooBig.fitsDimension(512));
    }
}

package dev.buildzones.client.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class SelectionBoundsTest {

    @Test
    void minAndMaxAreCorrectEvenWhenBIsBeforeA() {
        SelectionBounds b = SelectionBounds.between(new BlockPos(120, 80, 115), new BlockPos(100, 64, 100));
        assertEquals(100, b.minX());
        assertEquals(64, b.minY());
        assertEquals(100, b.minZ());
        assertEquals(120, b.maxX());
        assertEquals(80, b.maxY());
        assertEquals(115, b.maxZ());
    }

    @Test
    void dimensionsAreInclusive() {
        SelectionBounds same = SelectionBounds.between(new BlockPos(5, 5, 5), new BlockPos(5, 5, 5));
        assertEquals(1, same.sizeX());
        assertEquals(1, same.sizeY());
        assertEquals(1, same.sizeZ());
        assertEquals(1L, same.volume());
    }

    @Test
    void planExampleMatchesDocumentedVolume() {
        // Exemplo do plano: 21 × 17 × 16 = 5.712 blocos.
        SelectionBounds b = SelectionBounds.between(new BlockPos(100, 64, 100), new BlockPos(120, 80, 115));
        assertEquals(21, b.sizeX());
        assertEquals(17, b.sizeY());
        assertEquals(16, b.sizeZ());
        assertEquals(5712L, b.volume());
    }

    @Test
    void volumeDoesNotOverflowInt() {
        SelectionBounds big = new SelectionBounds(0, 0, 0, 2047, 2047, 2047);
        assertEquals(2048L * 2048L * 2048L, big.volume());
    }

    @Test
    void renderBoxCoversLastBlockFully() {
        SelectionBounds b = SelectionBounds.between(new BlockPos(0, 0, 0), new BlockPos(1, 1, 1));
        Box box = b.toBox();
        assertEquals(0.0, box.minX);
        assertEquals(2.0, box.maxX);
        assertEquals(2.0, box.maxY);
        assertEquals(2.0, box.maxZ);
    }
}

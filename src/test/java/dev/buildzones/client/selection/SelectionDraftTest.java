package dev.buildzones.client.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class SelectionDraftTest {
    private static final int MIN = -64;
    private static final int MAX = 319;

    private static SelectionDraft draftFrom(BlockPos a, BlockPos b) {
        SelectionDraft d = new SelectionDraft(a);
        d.setOpposite(b);
        return d;
    }

    @Test
    void scrollUpGrowsAndScrollDownShrinks() {
        SelectionDraft d = draftFrom(new BlockPos(100, 64, 100), new BlockPos(105, 64, 100));
        assertTrue(d.resize(SelectionAxis.X, 2, 1, -1000, 1000));
        assertEquals(107, d.opposite().getX());
        assertTrue(d.resize(SelectionAxis.X, -3, 1, -1000, 1000));
        assertEquals(104, d.opposite().getX());
    }

    @Test
    void anchorNeverMoves() {
        SelectionDraft d = draftFrom(new BlockPos(10, 20, 30), new BlockPos(14, 25, 33));
        d.resize(SelectionAxis.Y, 5, 1, MIN, MAX);
        d.resize(SelectionAxis.X, -10, 1, -1000, 1000);
        assertEquals(new BlockPos(10, 20, 30), d.anchor());
    }

    @Test
    void onlyChosenAxisChanges() {
        SelectionDraft d = draftFrom(new BlockPos(0, 0, 0), new BlockPos(2, 2, 2));
        d.resize(SelectionAxis.Y, 4, 1, MIN, MAX);
        assertEquals(2, d.opposite().getX());
        assertEquals(6, d.opposite().getY());
        assertEquals(2, d.opposite().getZ());
    }

    @Test
    void shrinkingStopsAtOneBlockAndNeverCrossesAnchor() {
        SelectionDraft d = draftFrom(new BlockPos(50, 70, 50), new BlockPos(52, 70, 50));
        d.resize(SelectionAxis.X, -100, 1, -1000, 1000);
        assertEquals(50, d.opposite().getX());
        assertEquals(1, d.bounds().sizeX());
        assertFalse(d.resize(SelectionAxis.X, -1, 1, -1000, 1000));
    }

    @Test
    void growthDirectionSurvivesReturningToMinimum() {
        // A e B coincidem: a direção vem do sinal da câmera no primeiro ajuste (-1 aqui).
        SelectionDraft d = new SelectionDraft(new BlockPos(0, 0, 0));
        d.setOpposite(new BlockPos(0, 0, 0));
        d.resize(SelectionAxis.X, 1, -1, -1000, 1000);
        assertEquals(-1, d.opposite().getX());
        // Volta ao mínimo...
        d.resize(SelectionAxis.X, -1, -1, -1000, 1000);
        assertEquals(0, d.opposite().getX());
        // ...e cresce de novo no mesmo sentido, mesmo com a câmera olhando para o outro lado.
        d.resize(SelectionAxis.X, 1, 1, -1000, 1000);
        assertEquals(-1, d.opposite().getX());
    }

    @Test
    void worldHeightLimitClampsYWithoutMovingAnchor() {
        SelectionDraft d = draftFrom(new BlockPos(0, 316, 0), new BlockPos(0, 317, 0));
        assertTrue(d.resize(SelectionAxis.Y, 10, 1, MIN, MAX));
        assertEquals(319, d.opposite().getY());
        assertEquals(316, d.anchor().getY());
        assertFalse(d.resize(SelectionAxis.Y, 1, 1, MIN, MAX));
    }

    @Test
    void scrollAccumulatesFractionalDeltas() {
        SelectionDraft d = new SelectionDraft(new BlockPos(0, 0, 0));
        assertEquals(0, d.consumeScroll(0.4));
        assertEquals(0, d.consumeScroll(0.4));
        assertEquals(1, d.consumeScroll(0.4));
        assertEquals(-1, d.consumeScroll(-2.0));
    }
}

package dev.buildzones.client.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class SelectionDraftTest {
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 319;
    private static final int OPEN = 1_000_000;

    private static SelectionDraft draftAt(BlockPos anchor) {
        SelectionDraft draft = new SelectionDraft(anchor);
        draft.setOpposite(anchor, MIN_Y, MAX_Y);
        return draft;
    }

    @Test
    void scrollUpGrowsAndScrollDownShrinks() {
        SelectionDraft draft = draftAt(new BlockPos(0, 64, 0));
        assertTrue(draft.resize(SelectionAxis.Y, 1, 1, MIN_Y, MAX_Y));
        assertEquals(65, draft.opposite().getY());
        assertTrue(draft.resize(SelectionAxis.Y, -1, 1, MIN_Y, MAX_Y));
        assertEquals(64, draft.opposite().getY());
    }

    @Test
    void anchorNeverMoves() {
        SelectionDraft draft = draftAt(new BlockPos(0, 64, 0));
        draft.resize(SelectionAxis.Y, 5, 1, MIN_Y, MAX_Y);
        draft.resize(SelectionAxis.X, -3, 1, -OPEN, OPEN);
        assertEquals(new BlockPos(0, 64, 0), draft.anchor());
    }

    @Test
    void onlyChosenAxisChanges() {
        SelectionDraft draft = draftAt(new BlockPos(0, 64, 0));
        draft.resize(SelectionAxis.X, 2, 1, -OPEN, OPEN);
        assertEquals(64, draft.opposite().getY());
        assertEquals(0, draft.opposite().getZ());
        assertTrue(draft.opposite().getX() > 0);
    }

    @Test
    void shrinkingStopsAtOneBlockAndNeverCrossesAnchor() {
        SelectionDraft draft = draftAt(new BlockPos(0, 64, 0));
        draft.resize(SelectionAxis.Y, 2, 1, MIN_Y, MAX_Y);
        draft.resize(SelectionAxis.Y, -10, 1, MIN_Y, MAX_Y);
        assertEquals(64, draft.opposite().getY());
        assertFalse(draft.resize(SelectionAxis.Y, -1, 1, MIN_Y, MAX_Y), "já no mínimo");
    }

    @Test
    void growthDirectionSurvivesReturningToMinimum() {
        SelectionDraft draft = draftAt(new BlockPos(0, 64, 0));
        draft.resize(SelectionAxis.Y, 1, -1, MIN_Y, MAX_Y);
        assertEquals(63, draft.opposite().getY(), "primeira expansão olha para baixo");
        draft.resize(SelectionAxis.Y, -1, -1, MIN_Y, MAX_Y);
        draft.resize(SelectionAxis.Y, 1, 1, MIN_Y, MAX_Y);
        assertEquals(63, draft.opposite().getY(), "a direção guardada vale mesmo com a câmera virada");
    }

    @Test
    void worldHeightLimitClampsYWithoutMovingAnchor() {
        SelectionDraft draft = draftAt(new BlockPos(0, 64, 0));
        draft.resize(SelectionAxis.Y, 500, 1, MIN_Y, MAX_Y);
        assertEquals(MAX_Y, draft.opposite().getY());
        assertEquals(64, draft.anchor().getY());
    }

    @Test
    void scrollAccumulatesFractionalDeltas() {
        SelectionDraft draft = draftAt(new BlockPos(0, 64, 0));
        assertEquals(0, draft.consumeScroll(0.5));
        assertEquals(1, draft.consumeScroll(0.5));
    }

    @Test
    void aimMovesOppositeWithTheScrollOffsetKept() {
        SelectionDraft draft = draftAt(new BlockPos(0, 64, 0));
        draft.resize(SelectionAxis.Y, 1, 1, MIN_Y, MAX_Y);
        draft.setOpposite(new BlockPos(5, 70, 5), MIN_Y, MAX_Y);
        assertEquals(new BlockPos(5, 71, 5), draft.opposite());
    }
}

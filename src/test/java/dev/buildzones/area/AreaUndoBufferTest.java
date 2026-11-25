package dev.buildzones.area;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AreaUndoBufferTest {

    @Test
    void popReturnsMostRecentBatchFirst() {
        AreaUndoBuffer<String> buffer = new AreaUndoBuffer<>(100);
        buffer.push(List.of("a1", "a2"));
        buffer.push(List.of("b1"));
        assertEquals(List.of("b1"), buffer.pop().orElseThrow());
        assertEquals(List.of("a1", "a2"), buffer.pop().orElseThrow());
        assertTrue(buffer.isEmpty());
        assertTrue(buffer.pop().isEmpty());
    }

    @Test
    void evictsOldestBatchesWhenCapIsReached() {
        AreaUndoBuffer<Integer> buffer = new AreaUndoBuffer<>(5);
        buffer.push(List.of(1, 2));
        buffer.push(List.of(3, 4));
        buffer.push(List.of(5, 6, 7));
        // 2+2+3 = 7 > 5: sai o lote mais antigo, depois fica 2+3 = 5.
        assertEquals(5L, buffer.totalCells());
        assertEquals(List.of(5, 6, 7), buffer.pop().orElseThrow());
        assertEquals(List.of(3, 4), buffer.pop().orElseThrow());
        assertTrue(buffer.pop().isEmpty());
    }

    @Test
    void refusesSingleBatchLargerThanWholeCap() {
        AreaUndoBuffer<Integer> buffer = new AreaUndoBuffer<>(3);
        buffer.push(List.of(1));
        assertFalse(buffer.push(List.of(1, 2, 3, 4)));
        // O lote recusado não expulsa o que já estava guardado.
        assertEquals(1L, buffer.totalCells());
    }

    @Test
    void emptyBatchIsIgnoredWithoutCountingAsUndo() {
        AreaUndoBuffer<Integer> buffer = new AreaUndoBuffer<>(10);
        assertTrue(buffer.push(List.of()));
        assertTrue(buffer.isEmpty());
    }

    @Test
    void clearDropsEverything() {
        AreaUndoBuffer<Integer> buffer = new AreaUndoBuffer<>(10);
        buffer.push(List.of(1, 2));
        buffer.clear();
        assertTrue(buffer.isEmpty());
        assertEquals(0L, buffer.totalCells());
    }
}

package dev.buildzones.area;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class AreaJobRunnerTest {

    @Test
    void sliceNeverExceedsBudgetAndFinishesOnLastSlice() {
        AreaBox box = AreaBox.between(new BlockPos(0, 0, 0), new BlockPos(15, 15, 15)); // 4096 células
        List<BlockPos> visited = new ArrayList<>();
        AreaJobRunner<BlockPos> runner = new AreaJobRunner<>(box.cells(), visited::add);

        int first = runner.runSlice(1000);
        assertEquals(1000, first);
        assertFalse(runner.finished());

        int second = runner.runSlice(3000);
        assertEquals(3000, second);
        assertFalse(runner.finished());

        // Sobram 96 células: a última fatia tem menos que o orçamento e termina o trabalho.
        int last = runner.runSlice(3000);
        assertEquals(96, last);
        assertTrue(runner.finished());
        assertEquals(0, runner.runSlice(10));
        assertEquals(4096, visited.size());
    }

    @Test
    void everyCellIsProcessedExactlyOnceAcrossSlices() {
        AreaBox box = AreaBox.between(new BlockPos(3, 70, -2), new BlockPos(6, 72, 1));
        List<BlockPos> visited = new ArrayList<>();
        AreaJobRunner<BlockPos> runner = new AreaJobRunner<>(box.cells(), visited::add);
        while (!runner.finished()) {
            runner.runSlice(5);
        }
        assertEquals(box.volume(), visited.size());
        assertEquals(visited.size(), new java.util.HashSet<>(visited).size());
    }

    @Test
    void emptyJobIsFinishedImmediately() {
        AreaJobRunner<BlockPos> runner = new AreaJobRunner<>(List.<BlockPos>of().iterator(), p -> { });
        assertTrue(runner.finished());
        assertEquals(0, runner.runSlice(100));
    }
}

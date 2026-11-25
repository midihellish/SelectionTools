package dev.buildzones.client.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildzones.client.axis.AxisResolver;
import dev.buildzones.client.storage.SelectionStorage;
import java.nio.file.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SelectionStateMachineTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final Vec3d LOOK_Y = new Vec3d(0.0, 1.0, 0.0);

    @TempDir
    Path tempDir;

    private SelectionManager manager;
    private SelectionStateMachine machine;

    @BeforeEach
    void setUp() {
        manager = new SelectionManager(new SelectionStorage(tempDir));
        manager.setContext("sp-test", "Teste", OVERWORLD);
        machine = new SelectionStateMachine(manager, new AxisResolver(0.15));
    }

    private void drawBox(BlockPos a, BlockPos b) {
        machine.beginDraft(a);
        machine.setSecondCorner(b, LOOK_Y);
        machine.confirm(0x53DDF2);
    }

    @Test
    void firstAndSecondCornerOpenPreview() {
        assertEquals(SelectionState.IDLE, machine.state());
        machine.beginDraft(new BlockPos(0, 64, 0));
        assertEquals(SelectionState.WAITING_SECOND_CORNER, machine.state());
        machine.setSecondCorner(new BlockPos(3, 64, 3), LOOK_Y);
        assertEquals(SelectionState.PREVIEW, machine.state());
    }

    @Test
    void scrollOnlyWorksInPreview() {
        machine.beginDraft(new BlockPos(0, 64, 0));
        assertEquals(SelectionStateMachine.Feedback.IGNORED, machine.resize(1, LOOK_Y, -64, 319));
    }

    @Test
    void confirmSavesRegionAndReturnsToIdle() {
        machine.beginDraft(new BlockPos(0, 64, 0));
        machine.setSecondCorner(new BlockPos(2, 64, 2), LOOK_Y);
        assertTrue(machine.confirm(0x53DDF2).isPresent());
        assertEquals(SelectionState.IDLE, machine.state());
        assertEquals(1, manager.regions().size());
    }

    @Test
    void escDiscardsOnlyTheDraft() {
        drawBox(new BlockPos(0, 64, 0), new BlockPos(1, 64, 1));
        machine.beginDraft(new BlockPos(10, 64, 10));
        assertEquals(SelectionStateMachine.Feedback.CANCELLED, machine.cancel());
        assertEquals(SelectionState.IDLE, machine.state());
        assertEquals(1, manager.regions().size());
    }

    @Test
    void undoRemovesOnlyTheLastRegionAndKeepsDraft() {
        drawBox(new BlockPos(0, 64, 0), new BlockPos(1, 64, 1));
        drawBox(new BlockPos(5, 64, 5), new BlockPos(6, 64, 6));
        machine.beginDraft(new BlockPos(20, 64, 20));
        machine.setSecondCorner(new BlockPos(21, 64, 21), LOOK_Y);

        assertTrue(machine.undo().isPresent());
        assertEquals(1, manager.regions().size());
        assertEquals(new BlockPos(0, 64, 0), manager.regions().get(0).anchor());
        assertEquals(SelectionState.PREVIEW, machine.state());
    }

    @Test
    void clearRemovesRegionsAndCancelsDraft() {
        drawBox(new BlockPos(0, 64, 0), new BlockPos(1, 64, 1));
        drawBox(new BlockPos(5, 64, 5), new BlockPos(6, 64, 6));
        machine.beginDraft(new BlockPos(9, 64, 9));
        assertEquals(2, machine.clearAll().size());
        assertTrue(manager.regions().isEmpty());
        assertEquals(SelectionState.IDLE, machine.state());
    }

    @Test
    void regionsFromOtherWorldsAreNotTouched() {
        drawBox(new BlockPos(0, 64, 0), new BlockPos(1, 64, 1));
        manager.setContext("sp-outro", "Outro", OVERWORLD);
        assertTrue(manager.regions().isEmpty());
        manager.clear();
        manager.setContext("sp-test", "Teste", OVERWORLD);
        assertEquals(1, manager.regions().size(), "limpar outro mundo não pode apagar este");
    }

    @Test
    void confirmedRegionsSurviveReload() {
        drawBox(new BlockPos(0, 64, 0), new BlockPos(3, 70, 3));
        drawBox(new BlockPos(10, 64, 10), new BlockPos(12, 64, 12));
        drawBox(new BlockPos(-4, 60, -4), new BlockPos(-2, 61, -2));

        SelectionManager reopened = new SelectionManager(new SelectionStorage(tempDir));
        reopened.setContext("sp-test", "Teste", OVERWORLD);
        assertEquals(3, reopened.regions().size());
        assertFalse(reopened.regions().isEmpty());
    }

    @Test
    void confirmWithoutContextIsRefused() {
        SelectionManager noContext = new SelectionManager(new SelectionStorage(tempDir));
        SelectionStateMachine m = new SelectionStateMachine(noContext, new AxisResolver(0.15));
        m.beginDraft(new BlockPos(0, 0, 0));
        m.setSecondCorner(new BlockPos(1, 0, 1), LOOK_Y);
        assertFalse(m.confirm(0).isPresent());
        assertEquals(SelectionState.PREVIEW, m.state());
    }
}

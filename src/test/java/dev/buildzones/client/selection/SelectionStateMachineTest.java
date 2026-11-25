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
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 319;

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

    /** Fluxo completo de três cliques: armar, canto A, canto B fixado, salvar. */
    private void drawBox(BlockPos a, BlockPos b) {
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(a);
        machine.aim(b, MIN_Y, MAX_Y);
        machine.lockSecondCorner();
        machine.confirm(0x53DDF2);
    }

    @Test
    void armingMovesFromIdleToArmed() {
        assertEquals(SelectionState.IDLE, machine.state());
        assertEquals(SelectionStateMachine.Feedback.ARMED, machine.arm(dev.buildzones.client.area.AreaShape.FILL));
        assertEquals(SelectionState.ARMED, machine.state());
    }

    @Test
    void clickIsIgnoredUntilArmed() {
        assertEquals(SelectionStateMachine.Feedback.IGNORED, machine.beginDraft(new BlockPos(0, 64, 0)));
        assertEquals(SelectionState.IDLE, machine.state());
    }

    @Test
    void armingWhileBusyIsIgnored() {
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        assertEquals(SelectionStateMachine.Feedback.IGNORED, machine.arm(dev.buildzones.client.area.AreaShape.FILL));
        machine.beginDraft(new BlockPos(0, 64, 0));
        assertEquals(SelectionStateMachine.Feedback.IGNORED, machine.arm(dev.buildzones.client.area.AreaShape.FILL));
        assertEquals(SelectionState.WAITING_SECOND_CORNER, machine.state());
    }

    @Test
    void firstClickMarksAnchorAndSecondLocksTheOppositeCorner() {
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(new BlockPos(0, 64, 0));
        machine.aim(new BlockPos(3, 64, 3), MIN_Y, MAX_Y);
        assertEquals(SelectionStateMachineTest.feedbackPreview(), machine.lockSecondCorner());
        assertEquals(SelectionState.PREVIEW, machine.state());
        machine.aim(new BlockPos(9, 64, 9), MIN_Y, MAX_Y);
        assertEquals(new BlockPos(3, 64, 3), machine.draft().opposite());
    }

    private static SelectionStateMachine.Feedback feedbackPreview() {
        return SelectionStateMachine.Feedback.PREVIEW_STARTED;
    }

    @Test
    void aimMovesOppositeCornerWhileAnchorStays() {
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(new BlockPos(0, 64, 0));
        machine.aim(new BlockPos(3, 64, 3), MIN_Y, MAX_Y);
        machine.aim(new BlockPos(5, 70, -2), MIN_Y, MAX_Y);
        assertEquals(new BlockPos(0, 64, 0), machine.draft().anchor());
        assertEquals(new BlockPos(5, 70, -2), machine.draft().opposite());
    }

    @Test
    void aimIsClampedToWorldHeight() {
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(new BlockPos(0, 64, 0));
        machine.aim(new BlockPos(0, 1000, 0), MIN_Y, MAX_Y);
        assertEquals(MAX_Y, machine.draft().opposite().getY());
    }

    @Test
    void scrollIsIgnoredBeforeTheCornerIsLocked() {
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(new BlockPos(0, 64, 0));
        assertEquals(SelectionStateMachine.Feedback.IGNORED, machine.resize(1, LOOK_Y, MIN_Y, MAX_Y));
        machine.aim(new BlockPos(0, 64, 0), MIN_Y, MAX_Y);
        assertEquals(SelectionStateMachine.Feedback.IGNORED, machine.resize(1, LOOK_Y, MIN_Y, MAX_Y));
    }

    @Test
    void scrollResizesOnlyInThePreview() {
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(new BlockPos(0, 64, 0));
        machine.aim(new BlockPos(0, 64, 0), MIN_Y, MAX_Y);
        machine.lockSecondCorner();
        machine.updateAxis(LOOK_Y);
        assertEquals(SelectionStateMachine.Feedback.RESIZED, machine.resize(1, LOOK_Y, MIN_Y, MAX_Y));
        assertEquals(65, machine.draft().opposite().getY());
    }

    @Test
    void lockedCornerIgnoresAimButKeepsScrollOffset() {
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(new BlockPos(0, 64, 0));
        machine.aim(new BlockPos(0, 64, 0), MIN_Y, MAX_Y);
        machine.lockSecondCorner();
        machine.updateAxis(LOOK_Y);
        machine.resize(2, LOOK_Y, MIN_Y, MAX_Y);
        machine.aim(new BlockPos(9, 90, 9), MIN_Y, MAX_Y);
        assertEquals(new BlockPos(0, 66, 0), machine.draft().opposite());
    }

    @Test
    void secondClickAfterLockSavesRegionAndReturnsToIdle() {
        drawBox(new BlockPos(0, 64, 0), new BlockPos(2, 64, 2));
        assertEquals(SelectionState.IDLE, machine.state());
        assertEquals(1, manager.regions().size());
        assertEquals(new BlockPos(2, 64, 2), manager.regions().get(0).opposite());
    }

    @Test
    void confirmIsIgnoredBeforeTheCornerIsLocked() {
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(new BlockPos(0, 64, 0));
        assertFalse(machine.confirm(0).isPresent());
        assertEquals(SelectionState.WAITING_SECOND_CORNER, machine.state());
        assertTrue(manager.regions().isEmpty());
    }

    @Test
    void confirmSavesRegionAtTheLockedPointEvenInTheAir() {
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(new BlockPos(0, 64, 0));
        machine.aim(new BlockPos(4, 90, 2), MIN_Y, MAX_Y);
        machine.lockSecondCorner();
        assertTrue(machine.confirm(0x53DDF2).isPresent());
        var bounds = manager.regions().get(0).bounds();
        assertEquals(5, bounds.sizeX());
        assertEquals(27, bounds.sizeY());
        assertEquals(3, bounds.sizeZ());
    }

    @Test
    void cancelDiscardsArmedChoiceOrDraftButNeverRegions() {
        drawBox(new BlockPos(0, 64, 0), new BlockPos(1, 64, 1));
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        assertEquals(SelectionStateMachine.Feedback.CANCELLED, machine.cancel());
        assertEquals(SelectionState.IDLE, machine.state());

        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(new BlockPos(10, 64, 10));
        machine.lockSecondCorner();
        assertEquals(SelectionStateMachine.Feedback.CANCELLED, machine.cancel());
        assertEquals(SelectionState.IDLE, machine.state());
        assertEquals(1, manager.regions().size());
    }

    @Test
    void cancelWhenIdleReportsNothingToCancel() {
        assertEquals(SelectionStateMachine.Feedback.NOTHING_TO_CANCEL, machine.cancel());
    }

    @Test
    void undoRemovesOnlyTheLastRegionAndKeepsDraft() {
        drawBox(new BlockPos(0, 64, 0), new BlockPos(1, 64, 1));
        drawBox(new BlockPos(5, 64, 5), new BlockPos(6, 64, 6));
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
        machine.beginDraft(new BlockPos(20, 64, 20));
        machine.aim(new BlockPos(21, 64, 21), MIN_Y, MAX_Y);

        assertTrue(machine.undo().isPresent());
        assertEquals(1, manager.regions().size());
        assertEquals(new BlockPos(0, 64, 0), manager.regions().get(0).anchor());
        assertEquals(SelectionState.WAITING_SECOND_CORNER, machine.state());
    }

    @Test
    void clearRemovesRegionsAndCancelsDraft() {
        drawBox(new BlockPos(0, 64, 0), new BlockPos(1, 64, 1));
        drawBox(new BlockPos(5, 64, 5), new BlockPos(6, 64, 6));
        machine.arm(dev.buildzones.client.area.AreaShape.FILL);
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
    }

    @Test
    void confirmWithoutContextIsRefused() {
        SelectionManager noContext = new SelectionManager(new SelectionStorage(tempDir));
        SelectionStateMachine m = new SelectionStateMachine(noContext, new AxisResolver(0.15));
        m.arm(dev.buildzones.client.area.AreaShape.FILL);
        m.beginDraft(new BlockPos(0, 0, 0));
        m.aim(new BlockPos(1, 0, 1), MIN_Y, MAX_Y);
        m.lockSecondCorner();
        assertFalse(m.confirm(0).isPresent());
        assertEquals(SelectionState.PREVIEW, m.state());
    }
}

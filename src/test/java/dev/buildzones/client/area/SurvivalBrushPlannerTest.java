package dev.buildzones.client.area;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildzones.area.AreaBox;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

class SurvivalBrushPlannerTest {

    @Test
    void planOnlyKeepsCellsThatDifferFromTarget() {
        AreaBox box = AreaBox.between(new BlockPos(0, 0, 0), new BlockPos(2, 0, 0));
        SurvivalBrushPlanner.Plan plan = SurvivalBrushPlanner.plan(box, new BlockPos(1, 0, 0), 8, 2, 100,
                pos -> switch (pos.getX()) {
                    case 0 -> SurvivalBrushPlanner.Cell.EMPTY;
                    case 1 -> SurvivalBrushPlanner.Cell.MATCHES;
                    default -> SurvivalBrushPlanner.Cell.WRONG;
                });
        assertEquals(1, plan.missing().size());
        assertEquals(new BlockPos(0, 0, 0), plan.missing().get(0));
        assertEquals(1, plan.wrong().size());
        assertEquals(new BlockPos(2, 0, 0), plan.wrong().get(0));
    }

    @Test
    void planStaysInsideRangeAroundPlayer() {
        AreaBox box = AreaBox.between(new BlockPos(0, 0, 0), new BlockPos(40, 10, 40));
        SurvivalBrushPlanner.Plan plan = SurvivalBrushPlanner.plan(box, new BlockPos(20, 5, 20), 3, 1, 10_000,
                pos -> SurvivalBrushPlanner.Cell.EMPTY);
        // (2·3+1)² na horizontal × (2·1+1) na vertical = 49 × 3
        assertEquals(49 * 3, plan.missing().size());
        for (BlockPos p : plan.missing()) {
            assertTrue(Math.abs(p.getX() - 20) <= 3 && Math.abs(p.getZ() - 20) <= 3 && Math.abs(p.getY() - 5) <= 1);
        }
    }

    @Test
    void planRespectsMaxCells() {
        AreaBox box = AreaBox.between(new BlockPos(0, 0, 0), new BlockPos(20, 0, 20));
        SurvivalBrushPlanner.Plan plan = SurvivalBrushPlanner.plan(box, new BlockPos(10, 0, 10), 10, 0, 25,
                pos -> SurvivalBrushPlanner.Cell.EMPTY);
        assertEquals(25, plan.missing().size());
    }

    @Test
    void rayPicksFirstMissingCellAheadOfPlayer() {
        Set<BlockPos> missing = Set.of(new BlockPos(3, 1, 0));
        Vec3d origin = new Vec3d(0.5, 1.5, 0.5);
        Vec3d dir = new Vec3d(1.0, 0.0, 0.0).normalize();
        var hit = SurvivalBrushPlanner.firstOnRay(missing, pos -> false, origin, dir, 6.0);
        assertEquals(new BlockPos(3, 1, 0), hit.orElseThrow());
    }

    @Test
    void rayStopsAtOccupiedCellBeforeMissingOne() {
        // Um bloco sólido entre o olhar e o fantasma bloqueia a colocação.
        Set<BlockPos> missing = Set.of(new BlockPos(4, 1, 0));
        Vec3d origin = new Vec3d(0.5, 1.5, 0.5);
        Vec3d dir = new Vec3d(1.0, 0.0, 0.0).normalize();
        var hit = SurvivalBrushPlanner.firstOnRay(missing, pos -> pos.getX() == 2, origin, dir, 6.0);
        assertFalse(hit.isPresent());
    }

    @Test
    void rayRefusesCellOutsideReach() {
        Set<BlockPos> missing = Set.of(new BlockPos(9, 1, 0));
        Vec3d origin = new Vec3d(0.5, 1.5, 0.5);
        Vec3d dir = new Vec3d(1.0, 0.0, 0.0).normalize();
        assertFalse(SurvivalBrushPlanner.firstOnRay(missing, pos -> false, origin, dir, 6.0).isPresent());
    }

    @Test
    void rayIgnoresLookingAwayFromCell() {
        Set<BlockPos> missing = Set.of(new BlockPos(3, 1, 0));
        Vec3d origin = new Vec3d(0.5, 1.5, 0.5);
        Vec3d dir = new Vec3d(-1.0, 0.0, 0.0);
        assertFalse(SurvivalBrushPlanner.firstOnRay(missing, pos -> false, origin, dir, 6.0).isPresent());
    }
}

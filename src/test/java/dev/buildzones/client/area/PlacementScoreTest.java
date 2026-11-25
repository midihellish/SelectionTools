package dev.buildzones.client.area;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

class PlacementScoreTest {
    private static final Vec3d EYE = new Vec3d(0.5, 0.5, 0.5);
    private static final Vec3d LOOK_X = new Vec3d(1, 0, 0);
    private static final double BONUS = 0.35;

    @Test
    void nearerCellWinsEvenWhenTheFarOneIsOnTheAim() {
        double near = PlacementScore.of(EYE, LOOK_X, new BlockPos(0, 0, 3), BONUS);
        double farOnAim = PlacementScore.of(EYE, LOOK_X, new BlockPos(5, 0, 0), BONUS);
        assertTrue(near < farOnAim);
    }

    @Test
    void aimBreaksTiesBetweenEquallyNearCells() {
        double onAim = PlacementScore.of(EYE, LOOK_X, new BlockPos(3, 0, 0), BONUS);
        double behind = PlacementScore.of(EYE, LOOK_X, new BlockPos(-3, 0, 0), BONUS);
        assertTrue(onAim < behind);
    }

    @Test
    void aimCanOvercomeASmallDistanceGap() {
        double slightlyFarOnAim = PlacementScore.of(EYE, LOOK_X, new BlockPos(3, 0, 0), BONUS);
        double slightlyNearOffAim = PlacementScore.of(EYE, LOOK_X, new BlockPos(0, 0, 2), BONUS);
        assertTrue(slightlyFarOnAim < slightlyNearOffAim);
    }

    @Test
    void cellsBehindTheViewGetNoDiscount() {
        double behind = PlacementScore.of(EYE, LOOK_X, new BlockPos(-3, 0, 0), BONUS);
        assertEquals(3.0, behind, 1.0E-9);
    }
}

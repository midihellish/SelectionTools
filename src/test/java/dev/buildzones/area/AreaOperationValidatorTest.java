package dev.buildzones.area;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AreaOperationValidatorTest {

    private static AreaOperationValidator.BulkFacts bulk(boolean creative, boolean jobRunning, boolean known,
                                                         boolean dimension, long volume, boolean needsBlock,
                                                         boolean holding) {
        return new AreaOperationValidator.BulkFacts(creative, jobRunning, known, dimension, volume,
                needsBlock, holding);
    }

    private static AreaOperationValidator.BulkFacts okBulk() {
        return bulk(true, false, true, true, 4096L, true, true);
    }

    @Test
    void bulkAcceptsValidCreativeRequest() {
        assertEquals(Optional.empty(), AreaOperationValidator.checkBulk(okBulk()));
    }

    @Test
    void bulkRefusesSurvivalBeforeAnythingElse() {
        // Mesmo com registro inválido, o modo vem primeiro: a resposta certa é "só criativo".
        var facts = bulk(false, true, false, false, Long.MAX_VALUE, true, false);
        assertEquals(Optional.of(AreaResult.NOT_CREATIVE), AreaOperationValidator.checkBulk(facts));
    }

    @Test
    void bulkRefusesWhileAnotherJobRuns() {
        var facts = bulk(true, true, true, true, 10L, false, false);
        assertEquals(Optional.of(AreaResult.BUSY), AreaOperationValidator.checkBulk(facts));
    }

    @Test
    void bulkRefusesUnknownRegionOrOtherDimension() {
        assertEquals(Optional.of(AreaResult.REGION_UNKNOWN),
                AreaOperationValidator.checkBulk(bulk(true, false, false, true, 10L, false, false)));
        assertEquals(Optional.of(AreaResult.REGION_UNKNOWN),
                AreaOperationValidator.checkBulk(bulk(true, false, true, false, 10L, false, false)));
    }

    @Test
    void bulkRefusesVolumeAboveLimitAndAcceptsExactlyTheLimit() {
        assertEquals(Optional.of(AreaResult.TOO_LARGE),
                AreaOperationValidator.checkBulk(bulk(true, false, true, true, AreaLimits.MAX_OP_VOLUME + 1,
                        false, false)));
        assertTrue(AreaOperationValidator.checkBulk(bulk(true, false, true, true, AreaLimits.MAX_OP_VOLUME,
                false, false)).isEmpty());
    }

    @Test
    void fillNeedsHeldBlock() {
        assertEquals(Optional.of(AreaResult.NOT_BLOCK_ITEM),
                AreaOperationValidator.checkBulk(bulk(true, false, true, true, 10L, true, false)));
        // CLEAR não precisa de bloco na mão.
        assertTrue(AreaOperationValidator.checkBulk(bulk(true, false, true, true, 10L, false, false)).isEmpty());
    }

    @Test
    void undoNeedsCreativeIdleAndABatch() {
        assertEquals(Optional.of(AreaResult.NOT_CREATIVE), AreaOperationValidator.checkUndo(false, false, true));
        assertEquals(Optional.of(AreaResult.BUSY), AreaOperationValidator.checkUndo(true, true, true));
        assertEquals(Optional.of(AreaResult.UNDO_EMPTY), AreaOperationValidator.checkUndo(true, false, false));
        assertTrue(AreaOperationValidator.checkUndo(true, false, true).isEmpty());
    }

    private static AreaOperationValidator.PlaceFacts place(boolean rateOk, boolean known, boolean dimension,
                                                           boolean inside, boolean hasTarget, boolean held,
                                                           boolean allowed, boolean inBuild, boolean chunk,
                                                           double distanceSq) {
        return new AreaOperationValidator.PlaceFacts(rateOk, known, dimension, inside, hasTarget, held,
                allowed, inBuild, chunk, distanceSq, 36.0);
    }

    private static AreaOperationValidator.PlaceFacts goodPlace(double distanceSq) {
        return place(true, true, true, true, true, true, true, true, true, distanceSq);
    }

    @Test
    void placementAcceptsWhenEverythingHolds() {
        assertTrue(AreaOperationValidator.checkPlacement(goodPlace(9.0)).isEmpty());
    }

    @Test
    void placementRateLimitComesFirst() {
        var facts = place(false, false, false, false, false, false, false, false, false, 1000.0);
        assertEquals(Optional.of(AreaResult.RATE_LIMITED), AreaOperationValidator.checkPlacement(facts));
    }

    @Test
    void placementRefusesOutsideRegionAndWrongItem() {
        assertEquals(Optional.of(AreaResult.OUT_OF_REGION),
                AreaOperationValidator.checkPlacement(place(true, true, true, false, true, true, true, true, true, 1.0)));
        assertEquals(Optional.of(AreaResult.WRONG_ITEM),
                AreaOperationValidator.checkPlacement(place(true, true, true, true, true, false, true, true, true, 1.0)));
    }

    @Test
    void placementRefusesMissingTarget() {
        assertEquals(Optional.of(AreaResult.NO_TARGET),
                AreaOperationValidator.checkPlacement(place(true, true, true, true, false, false, true, true, true, 1.0)));
    }

    @Test
    void placementRefusesOutOfReach() {
        assertEquals(Optional.of(AreaResult.OUT_OF_REACH), AreaOperationValidator.checkPlacement(goodPlace(37.0)));
        assertTrue(AreaOperationValidator.checkPlacement(goodPlace(36.0)).isEmpty());
    }

    @Test
    void placementRefusesUnloadedChunkAndNoPermission() {
        assertEquals(Optional.of(AreaResult.UNLOADED),
                AreaOperationValidator.checkPlacement(place(true, true, true, true, true, true, true, true, false, 1.0)));
        assertEquals(Optional.of(AreaResult.NO_PERMISSION),
                AreaOperationValidator.checkPlacement(place(true, true, true, true, true, true, false, true, true, 1.0)));
    }

    @Test
    void breakNeedsNoTargetNorHeldItem() {
        var facts = place(true, true, true, true, false, false, true, true, true, 4.0);
        assertTrue(AreaOperationValidator.checkBreak(facts).isEmpty());
        assertEquals(Optional.of(AreaResult.OUT_OF_REGION),
                AreaOperationValidator.checkBreak(place(true, true, true, false, false, false, true, true, true, 4.0)));
        assertEquals(Optional.of(AreaResult.OUT_OF_REACH),
                AreaOperationValidator.checkBreak(place(true, true, true, true, false, false, true, true, true, 37.0)));
    }

    @Test
    void placeRateAllowsFirstAndRespectsInterval() {
        assertTrue(AreaOperationValidator.placeRateOk(-1L, 1000L));
        assertFalse(AreaOperationValidator.placeRateOk(1000L, 1000L + AreaLimits.PLACE_MIN_INTERVAL_MS - 1));
        assertTrue(AreaOperationValidator.placeRateOk(1000L, 1000L + AreaLimits.PLACE_MIN_INTERVAL_MS));
    }
}

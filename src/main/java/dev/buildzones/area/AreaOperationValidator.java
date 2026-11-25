package dev.buildzones.area;

import java.util.Optional;

/**
 * Regras puras dos pedidos de área. Recebem fatos já lidos do servidor (modo de jogo,
 * registro, limites) e devolvem o motivo da recusa. Vazio significa que pode prosseguir.
 *
 * A ordem das checagens segue o {@code ReplaceBlockValidator}: barato primeiro,
 * e nunca confiar no que o pedido afirma.
 */
public final class AreaOperationValidator {

    private AreaOperationValidator() {
    }

    /** Fatos sobre uma operação em massa, lidos pelo serviço antes de chamar a regra. */
    public record BulkFacts(boolean creative, boolean jobRunning, boolean regionKnown,
                            boolean dimensionMatches, long volume, boolean needsHeldBlock,
                            boolean holdingBlock) {
    }

    /** Ordem: modo, operação ativa, registro, dimensão, volume, bloco na mão. */
    public static Optional<AreaResult> checkBulk(BulkFacts f) {
        if (!f.creative()) {
            return refuse(AreaResult.NOT_CREATIVE);
        }
        if (f.jobRunning()) {
            return refuse(AreaResult.BUSY);
        }
        if (!f.regionKnown() || !f.dimensionMatches()) {
            return refuse(AreaResult.REGION_UNKNOWN);
        }
        if (f.volume() > AreaLimits.MAX_OP_VOLUME) {
            return refuse(AreaResult.TOO_LARGE);
        }
        if (f.needsHeldBlock() && !f.holdingBlock()) {
            return refuse(AreaResult.NOT_BLOCK_ITEM);
        }
        return Optional.empty();
    }

    /** Desfazer em massa: criativo e nenhuma operação em andamento. */
    public static Optional<AreaResult> checkUndo(boolean creative, boolean jobRunning, boolean hasBatch) {
        if (!creative) {
            return refuse(AreaResult.NOT_CREATIVE);
        }
        if (jobRunning) {
            return refuse(AreaResult.BUSY);
        }
        if (!hasBatch) {
            return refuse(AreaResult.UNDO_EMPTY);
        }
        return Optional.empty();
    }

    /**
     * Fatos sobre uma colocação de survival. {@code distanceSq} é a distância do jogador
     * ao centro da célula, ao quadrado.
     */
    public record PlaceFacts(boolean rateOk, boolean regionKnown, boolean dimensionMatches,
                             boolean insideRegion, boolean hasTarget, boolean heldMatchesTarget,
                             boolean allowedToBuild, boolean inBuildLimit, boolean chunkLoaded,
                             double distanceSq, double reachSq) {
    }

    /** Ordem: limite de ritmo, registro, região, alvo, item, permissão, mundo, chunk, alcance. */
    public static Optional<AreaResult> checkPlacement(PlaceFacts f) {
        if (!f.rateOk()) {
            return refuse(AreaResult.RATE_LIMITED);
        }
        if (!f.regionKnown() || !f.dimensionMatches()) {
            return refuse(AreaResult.REGION_UNKNOWN);
        }
        if (!f.insideRegion()) {
            return refuse(AreaResult.OUT_OF_REGION);
        }
        if (!f.hasTarget()) {
            return refuse(AreaResult.NO_TARGET);
        }
        if (!f.heldMatchesTarget()) {
            return refuse(AreaResult.WRONG_ITEM);
        }
        if (!f.allowedToBuild()) {
            return refuse(AreaResult.NO_PERMISSION);
        }
        if (!f.inBuildLimit()) {
            return refuse(AreaResult.OUT_OF_WORLD);
        }
        if (!f.chunkLoaded()) {
            return refuse(AreaResult.UNLOADED);
        }
        if (f.distanceSq() > f.reachSq()) {
            return refuse(AreaResult.OUT_OF_REACH);
        }
        return Optional.empty();
    }

    /**
     * Quebra do pincel: as mesmas checagens da colocação, menos alvo e item na mão, que quebrar não exige.
     */
    public static Optional<AreaResult> checkBreak(PlaceFacts f) {
        return checkPlacement(new PlaceFacts(f.rateOk(), f.regionKnown(), f.dimensionMatches(), f.insideRegion(),
                true, true, f.allowedToBuild(), f.inBuildLimit(), f.chunkLoaded(), f.distanceSq(), f.reachSq()));
    }

    /** Intervalo mínimo entre colocações, em milissegundos. Abaixo disso o pedido é descartado. */
    public static boolean placeRateOk(long lastPlaceMs, long nowMs) {
        return lastPlaceMs < 0 || nowMs - lastPlaceMs >= AreaLimits.PLACE_MIN_INTERVAL_MS;
    }

    private static Optional<AreaResult> refuse(AreaResult result) {
        return Optional.of(result);
    }
}

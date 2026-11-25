package dev.buildzones.area;

/**
 * Resultado de um pedido de área. {@link #key()} é a chave de tradução que o HUD mostra.
 * Chaves de erros comuns com a substituição reaproveitam {@code buildzones.replace.*}.
 */
public enum AreaResult {
    QUEUED("buildzones.area.queued"),
    DONE("buildzones.area.done"),
    PLACED("buildzones.area.placed"),
    UNDO_EMPTY("buildzones.area.undo_empty"),
    NOT_CREATIVE("buildzones.area.not_creative"),
    BUSY("buildzones.area.busy"),
    REGION_UNKNOWN("buildzones.area.region_unknown"),
    TOO_LARGE("buildzones.area.too_large"),
    NO_TARGET("buildzones.area.no_target"),
    WRONG_ITEM("buildzones.area.wrong_item"),
    OUT_OF_REGION("buildzones.area.out_of_region"),
    CELL_OCCUPIED("buildzones.area.cell_occupied"),
    PROTECTED("buildzones.area.protected"),
    RATE_LIMITED("buildzones.area.rate_limited"),
    NOT_BLOCK_ITEM("buildzones.replace.not_block_item"),
    NO_PERMISSION("buildzones.replace.no_permission"),
    OUT_OF_REACH("buildzones.replace.out_of_reach"),
    OUT_OF_WORLD("buildzones.replace.out_of_world"),
    UNLOADED("buildzones.replace.unloaded"),
    INVALID_PLACEMENT("buildzones.replace.invalid_placement"),
    INTERNAL_ERROR("buildzones.replace.internal_error");

    private final String key;

    AreaResult(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}

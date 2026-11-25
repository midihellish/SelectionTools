package dev.buildzones.replacement;

/**
 * Resultado de uma substituição. {@link #key()} é a chave de tradução usada pelo HUD do cliente.
 */
public enum ReplacementResult {
    APPLIED("buildzones.replace.applied"),
    NOT_BLOCK_ITEM("buildzones.replace.not_block_item"),
    NO_PERMISSION("buildzones.replace.no_permission"),
    OUT_OF_REACH("buildzones.replace.out_of_reach"),
    OUT_OF_WORLD("buildzones.replace.out_of_world"),
    UNLOADED("buildzones.replace.unloaded"),
    TARGET_CHANGED("buildzones.replace.target_changed"),
    TARGET_AIR("buildzones.replace.target_air"),
    TARGET_PROTECTED("buildzones.replace.target_protected"),
    SAME_BLOCK("buildzones.replace.same_block"),
    INVALID_PLACEMENT("buildzones.replace.invalid_placement"),
    INTERNAL_ERROR("buildzones.replace.internal_error");

    private final String key;

    ReplacementResult(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public boolean isApplied() {
        return this == APPLIED;
    }
}

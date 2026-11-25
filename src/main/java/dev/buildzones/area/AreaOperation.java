package dev.buildzones.area;

/**
 * Pedidos de operação sobre a área. CLEAR e FILL são em massa e só valem no criativo.
 * UNDO desfaz a última operação em massa do jogador.
 */
public enum AreaOperation {
    CLEAR,
    FILL,
    UNDO
}

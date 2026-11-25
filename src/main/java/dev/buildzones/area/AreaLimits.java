package dev.buildzones.area;

/**
 * Limites compartilhados por cliente e servidor. Ficam em {@code main} porque o
 * servidor precisa deles e o código comum não pode depender do source set client.
 */
public final class AreaLimits {

    /** Tamanho máximo de uma dimensão de região, para evitar caixas gigantes por acidente. */
    public static final int MAX_DIMENSION = 512;

    /** Volume máximo de uma operação em massa (64 × 64 × 64). */
    public static final long MAX_OP_VOLUME = 262_144L;

    /** Total de células guardadas para desfazer, por jogador. */
    public static final long UNDO_MAX_CELLS = 1_048_576L;

    /** Células processadas por tick de servidor em uma operação em massa. */
    public static final int JOB_SLICE_CELLS = 4096;

    /** Intervalo mínimo entre colocações de um mesmo jogador no servidor. Um pouco abaixo do cliente. */
    public static final long PLACE_MIN_INTERVAL_MS = 80L;

    private AreaLimits() {
    }
}

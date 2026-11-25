package dev.buildzones.client.config;

import dev.buildzones.area.AreaLimits;

/**
 * Valores de comportamento em um único lugar. Config em JSON e tela ficam para
 * uma versão futura; a V1 não precisa de biblioteca externa.
 */
public final class BuildZonesConfig {
    /** Tamanho máximo de uma dimensão, para evitar caixas gigantes por acidente. Igual ao do servidor. */
    public static final int MAX_DIMENSION = AreaLimits.MAX_DIMENSION;

    /** Limite absoluto para X e Z, bem acima de qualquer borda real do mundo. */
    public static final int MAX_WORLD_COORD = 30_000_000;

    /** Margem de histerese do eixo, em componente normalizado. */
    public static final double AXIS_HYSTERESIS = 0.15;

    /** Distância máxima de renderização das regiões, em blocos. */
    public static final double RENDER_DISTANCE = 192.0;

    /** Janela para ignorar o mesmo clique repetido pelo vanilla enquanto o botão está segurado. */
    public static final long ATTACK_DEBOUNCE_MS = 120L;

    /** Intervalo mínimo entre pedidos de substituição do botão direito segurado. */
    public static final long REPLACE_COOLDOWN_MS = 250L;

    /** Tempo de exibição de um toast do HUD. */
    public static final long TOAST_DURATION_MS = 2400L;

    /** Intervalo entre colocações do pincel de área enquanto o botão direito estiver segurado. */
    public static final long AREA_REPEAT_MS = 120L;

    /** Raio horizontal, em blocos, dentro do qual os fantasmas do pincel aparecem. */
    public static final int AREA_GHOST_RANGE = 8;

    /** Camada vertical, para cima e para baixo do jogador, em que os fantasmas aparecem. */
    public static final int AREA_GHOST_LAYER = 2;

    /** Teto de fantasmas desenhados ao mesmo tempo, para não poluir nem pesar. */
    public static final int AREA_GHOST_MAX = 1024;

    /** Janela para a segunda pressão confirmar uma operação em massa. */
    public static final long AREA_CONFIRM_WINDOW_MS = 5000L;

    private BuildZonesConfig() {
    }
}

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

    /** Intervalo mínimo entre pedidos de substituição do botão direito segurado. */
    public static final long REPLACE_COOLDOWN_MS = 250L;

    /** Tempo de exibição de um toast do HUD. */
    public static final long TOAST_DURATION_MS = 2400L;

    /** Intervalo mínimo entre ações do pincel com o botão segurado. Age em ticks de 50 ms: 90 vira uma ação a cada 2 ticks (10 por segundo). */
    public static final long AREA_REPEAT_MS = 90L;

    /**
     * Desconto máximo da mira sobre a distância ao escolher onde colocar (0 a 1). Proximidade manda:
     * com 0,35 a célula bem na mira "parece" até 35% mais perto, e fora da frente do olhar não ganha nada.
     */
    public static final double AREA_AIM_BONUS = 0.35;

    /** Tempo em que uma célula já pedida não é escolhida de novo, à espera da atualização do servidor. */
    public static final long AREA_PENDING_MS = 300L;

    /** Janela em que o mesmo bloco não é quebrado de novo, à espera da atualização do servidor. */
    public static final long AREA_SAME_CELL_MS = 200L;

    /** Janela para a segunda pressão confirmar uma operação em massa. */
    public static final long AREA_CONFIRM_WINDOW_MS = 5000L;

    private BuildZonesConfig() {
    }
}

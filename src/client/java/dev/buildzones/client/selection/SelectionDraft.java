package dev.buildzones.client.selection;

import dev.buildzones.client.config.BuildZonesConfig;
import java.util.EnumMap;
import net.minecraft.util.math.BlockPos;

/**
 * Rascunho em edição. Existe só enquanto o jogador define a região.
 *
 * O canto A (anchor) é fixo. O canto B é o ponto da mira somado ao ajuste do scroll,
 * por eixo: a mira move a caixa, e o scroll cresce ou diminui no eixo da câmera.
 * A direção de crescimento de cada eixo é guardada na primeira expansão e continua valendo
 * mesmo quando a dimensão volta ao mínimo.
 */
public final class SelectionDraft {
    private final BlockPos anchor;
    private BlockPos aim;
    private int minY = Integer.MIN_VALUE;
    private int maxY = Integer.MAX_VALUE;
    private final int[] scrollOffset = new int[3];
    private SelectionAxis activeAxis;
    private final EnumMap<SelectionAxis, Integer> growthSigns = new EnumMap<>(SelectionAxis.class);
    private double scrollAccumulator;

    public SelectionDraft(BlockPos anchor) {
        this.anchor = anchor.toImmutable();
    }

    public BlockPos anchor() {
        return anchor;
    }

    /** Canto B: mira mais ajuste do scroll, com Y dentro do mundo. O próprio A antes da primeira mira. */
    public BlockPos opposite() {
        if (aim == null) {
            return anchor;
        }
        int y = Math.max(minY, Math.min(maxY, aim.getY() + scrollOffset[1]));
        return new BlockPos(aim.getX() + scrollOffset[0], y, aim.getZ() + scrollOffset[2]);
    }

    /** Define o ponto da mira e os limites de Y do mundo. Não mexe no ajuste do scroll. */
    public void setOpposite(BlockPos position, int minY, int maxY) {
        this.aim = position.toImmutable();
        this.minY = minY;
        this.maxY = maxY;
    }

    public void setOpposite(BlockPos position) {
        setOpposite(position, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public SelectionAxis activeAxis() {
        return activeAxis;
    }

    public void setActiveAxis(SelectionAxis axis) {
        this.activeAxis = axis;
    }

    public SelectionBounds bounds() {
        return SelectionBounds.between(anchor, opposite());
    }

    /**
     * Acumula o delta do scroll e devolve os passos inteiros já completos.
     * Rodas com granularidade fracionária não geram saltos.
     */
    public int consumeScroll(double delta) {
        scrollAccumulator += delta;
        int steps = (int) scrollAccumulator;
        scrollAccumulator -= steps;
        return steps;
    }

    /**
     * Ajusta o canto B no eixo, em blocos, a partir da dimensão atual, mantendo o A fixo.
     * O ajuste fica guardado como deslocamento sobre a mira, então continua valendo quando a mira se move.
     *
     * @param lookSign  sinal da câmera no eixo; usado só quando A e B coincidem e
     *                  a direção ainda não foi guardada
     * @param minCoord  menor coordenada válida no eixo (limite do mundo)
     * @param maxCoord  maior coordenada válida no eixo (limite do mundo)
     * @return true se o canto B mudou
     */
    public boolean resize(SelectionAxis axis, int steps, int lookSign, int minCoord, int maxCoord) {
        if (aim == null || steps == 0) {
            return false;
        }
        int a = coord(anchor, axis);
        int o = coord(opposite(), axis);
        int sign = growthSign(axis, a, o, lookSign);

        int currentDim = Math.abs(o - a) + 1;
        int newDim = Math.max(1, Math.min(BuildZonesConfig.MAX_DIMENSION, currentDim + steps));
        int target = a + sign * (newDim - 1);
        // O limite do mundo pode cortar a expansão; como A está dentro dele, o corte nunca cruza A.
        target = Math.max(minCoord, Math.min(maxCoord, target));
        int offset = target - coord(aim, axis);
        if (offset == scrollOffset[axis.ordinal()] || target == o) {
            return false;
        }
        scrollOffset[axis.ordinal()] = offset;
        return true;
    }

    private int growthSign(SelectionAxis axis, int a, int o, int lookSign) {
        Integer stored = growthSigns.get(axis);
        if (stored != null) {
            return stored;
        }
        int sign;
        if (o != a) {
            sign = Integer.signum(o - a);
        } else {
            sign = lookSign < 0 ? -1 : 1;
        }
        growthSigns.put(axis, sign);
        return sign;
    }

    static int coord(BlockPos pos, SelectionAxis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }
}

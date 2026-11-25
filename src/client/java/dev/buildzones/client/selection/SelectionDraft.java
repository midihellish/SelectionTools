package dev.buildzones.client.selection;

import dev.buildzones.client.config.BuildZonesConfig;
import java.util.EnumMap;
import net.minecraft.util.math.BlockPos;

/**
 * Rascunho em edição. Existe só enquanto o jogador define a região.
 *
 * O canto A (anchor) é fixo. O canto B (opposite) é o único que se move, e só
 * no eixo ativo. A direção de crescimento de cada eixo é guardada na primeira
 * expansão e continua valendo mesmo quando a dimensão volta ao mínimo.
 */
public final class SelectionDraft {
    private final BlockPos anchor;
    private BlockPos opposite;
    private SelectionAxis activeAxis;
    private final EnumMap<SelectionAxis, Integer> growthSigns = new EnumMap<>(SelectionAxis.class);
    private double scrollAccumulator;

    public SelectionDraft(BlockPos anchor) {
        this.anchor = anchor.toImmutable();
    }

    public BlockPos anchor() {
        return anchor;
    }

    /** Canto B, ou o próprio A enquanto o segundo canto não foi marcado. */
    public BlockPos opposite() {
        return opposite == null ? anchor : opposite;
    }

    public boolean hasOpposite() {
        return opposite != null;
    }

    public void setOpposite(BlockPos position) {
        this.opposite = position.toImmutable();
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
     * Ajusta o canto B no eixo, mantendo o A fixo.
     *
     * @param lookSign  sinal da câmera no eixo; usado só quando A e B coincidem e
     *                  a direção ainda não foi guardada
     * @param minCoord  menor coordenada válida no eixo (limite do mundo)
     * @param maxCoord  maior coordenada válida no eixo (limite do mundo)
     * @return true se o canto B mudou
     */
    public boolean resize(SelectionAxis axis, int steps, int lookSign, int minCoord, int maxCoord) {
        if (!hasOpposite() || steps == 0) {
            return false;
        }
        int a = coord(anchor, axis);
        int o = coord(opposite, axis);
        int sign = growthSign(axis, a, o, lookSign);

        int currentDim = Math.abs(o - a) + 1;
        int newDim = Math.max(1, Math.min(BuildZonesConfig.MAX_DIMENSION, currentDim + steps));
        int target = a + sign * (newDim - 1);
        // O limite do mundo pode cortar a expansão; como A está dentro dele, o corte nunca cruza A.
        target = Math.max(minCoord, Math.min(maxCoord, target));
        if (target == o) {
            return false;
        }
        opposite = withCoord(opposite, axis, target);
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

    private static BlockPos withCoord(BlockPos pos, SelectionAxis axis, int value) {
        return switch (axis) {
            case X -> new BlockPos(value, pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), value, pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), value);
        };
    }
}

package dev.buildzones.client.area;

import dev.buildzones.area.AreaBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Cálculo puro do pincel de área de sobrevivência: quais células ainda diferem do alvo
 * (fantasmas) e qual célula o raio do olhar escolhe. Não lê o mundo: quem chama passa
 * as classificações, o que deixa a regra testável sem bootstrap do Minecraft.
 */
public final class SurvivalBrushPlanner {

    /** Situação de uma célula em relação ao alvo. */
    public enum Cell {
        /** Já tem o bloco do alvo. Não aparece. */
        MATCHES,
        /** Vazia ou substituível. Recebe um fantasma de colocação. */
        EMPTY,
        /** Ocupada por outro bloco. Aparece com contorno de remoção. */
        WRONG
    }

    /** Células a desenhar: fantasmas de colocação e contornos de remoção. */
    public record Plan(List<BlockPos> missing, List<BlockPos> wrong) {
    }

    private SurvivalBrushPlanner() {
    }

    /**
     * Monta o plano limitado a um cubo ao redor do jogador: {@code range} na horizontal,
     * {@code layer} na vertical. Para ao atingir {@code maxCells} no total.
     */
    public static Plan plan(AreaBox box, BlockPos center, int range, int layer, int maxCells,
                            Function<BlockPos, Cell> classify) {
        List<BlockPos> missing = new ArrayList<>();
        List<BlockPos> wrong = new ArrayList<>();
        int minX = Math.max(box.minX(), center.getX() - range);
        int maxX = Math.min(box.maxX(), center.getX() + range);
        int minY = Math.max(box.minY(), center.getY() - layer);
        int maxY = Math.min(box.maxY(), center.getY() + layer);
        int minZ = Math.max(box.minZ(), center.getZ() - range);
        int maxZ = Math.min(box.maxZ(), center.getZ() + range);

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (missing.size() + wrong.size() >= maxCells) {
                        return new Plan(missing, wrong);
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    switch (classify.apply(pos)) {
                        case EMPTY -> missing.add(pos);
                        case WRONG -> wrong.add(pos);
                        case MATCHES -> {
                            // Nada a desenhar.
                        }
                    }
                }
            }
        }
        return new Plan(missing, wrong);
    }

    /**
     * Primeira célula de fantasma atingida pelo raio do olhar, até {@code reach}.
     * O raio para na primeira célula ocupada que não é fantasma: não se coloca atrás de um bloco.
     *
     * @param missing   células que aceitam colocação
     * @param occupied  verdadeiro se a célula bloqueia a visão (sólida, não substituível)
     */
    public static Optional<BlockPos> firstOnRay(Set<BlockPos> missing, Predicate<BlockPos> occupied,
                                                Vec3d origin, Vec3d direction, double reach) {
        double reachSq = reach * reach;
        double step = 0.05;
        BlockPos last = null;
        for (double d = 0.0; d <= reach; d += step) {
            int x = (int) Math.floor(origin.x + direction.x * d);
            int y = (int) Math.floor(origin.y + direction.y * d);
            int z = (int) Math.floor(origin.z + direction.z * d);
            BlockPos cell = new BlockPos(x, y, z);
            if (cell.equals(last)) {
                continue;
            }
            last = cell;
            if (missing.contains(cell)) {
                // Mesmo dentro do alcance do raio, o centro da célula precisa estar no alcance do servidor.
                if (Vec3d.ofCenter(cell).squaredDistanceTo(origin) <= reachSq) {
                    return Optional.of(cell);
                }
                return Optional.empty();
            }
            if (occupied.test(cell)) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}

package dev.buildzones.client.area;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Pontuação de uma célula para o pincel colocar: menor é melhor. Proximidade manda; a mira só dá um
 * desconto sobre a distância. Puro, sem ler o mundo, para ser testável sem bootstrap do Minecraft.
 */
public final class PlacementScore {

    private PlacementScore() {
    }

    /**
     * Distância efetiva do olho ao centro da célula. A célula bem no centro da mira ganha até
     * {@code aimBonus} (fração, 0 a 1) de desconto; fora da frente do olhar, nenhum.
     * Com {@code aimBonus} abaixo de 1, uma célula mais perto nunca perde só pela mira para outra
     * mais longe do que {@code 1 / (1 - aimBonus)} vezes a sua distância.
     *
     * @param look direção do olhar, normalizada
     */
    public static double of(Vec3d eye, Vec3d look, BlockPos cell, double aimBonus) {
        Vec3d toCell = Vec3d.ofCenter(cell).subtract(eye);
        double distance = toCell.length();
        if (distance < 1.0E-6) {
            return 0.0;
        }
        double aim = Math.max(0.0, toCell.dotProduct(look) / distance);
        return distance * (1.0 - aimBonus * aim);
    }
}

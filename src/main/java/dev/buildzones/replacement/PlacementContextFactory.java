package dev.buildzones.replacement;

import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Cria o contexto de colocação apontando para o próprio bloco alvo.
 *
 * Decisão do spike (ver docs/REPLACEMENT_COMPATIBILITY.md): o vanilla desloca
 * a posição de colocação para o vizinho quando o alvo é sólido. Em vez de
 * usar Accessor no campo privado {@code placementPos}, a subclasse sobrescreve
 * {@link ItemPlacementContext#getBlockPos()} e os métodos de permissão.
 */
public final class PlacementContextFactory {

    private PlacementContextFactory() {
    }

    public static ItemPlacementContext create(World world, PlayerEntity player, Hand hand, ItemStack stack,
                                              BlockPos target, Direction side) {
        // O ponto de impacto fica na face do alvo; o lado indica de onde o jogador "clicou".
        Vec3d hitPos = Vec3d.ofCenter(target).add(Vec3d.of(side.getVector()).multiply(0.5));
        BlockHitResult hit = new BlockHitResult(hitPos, side, target, false);
        return new TargetPlacementContext(world, player, hand, stack, hit, target);
    }

    private static final class TargetPlacementContext extends ItemPlacementContext {
        private final BlockPos target;

        TargetPlacementContext(World world, PlayerEntity player, Hand hand, ItemStack stack,
                               BlockHitResult hit, BlockPos target) {
            super(world, player, hand, stack, hit);
            this.target = target.toImmutable();
        }

        @Override
        public BlockPos getBlockPos() {
            return target;
        }

        @Override
        public boolean canPlace() {
            return true;
        }

        @Override
        public boolean canReplaceExisting() {
            return true;
        }
    }
}

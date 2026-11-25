package dev.buildzones.replacement;

import dev.buildzones.network.ReplaceBlockRequest;
import java.util.Optional;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

/**
 * Validações do servidor. Não confia na coordenada enviada: refaz o raycast e
 * confere alcance, carregamento, permissões e o alvo atual.
 */
public final class ReplaceBlockValidator {

    /** Alcance do servidor, um pouco acima do vanilla para tolerar latência. */
    public static final double REACH = 6.0;

    private ReplaceBlockValidator() {
    }

    /** Vazio significa que a solicitação pode ser aplicada. */
    public static Optional<ReplacementResult> validate(ServerPlayerEntity player, ReplaceBlockRequest request) {
        if (player.isSpectator() || player.interactionManager.getGameMode() == GameMode.ADVENTURE) {
            return refuse(ReplacementResult.NO_PERMISSION);
        }

        ItemStack stack = player.getStackInHand(request.hand());
        if (!(stack.getItem() instanceof BlockItem)) {
            return refuse(ReplacementResult.NOT_BLOCK_ITEM);
        }

        World world = player.getWorld();
        BlockPos pos = request.pos();
        if (!world.isInBuildLimit(pos)) {
            return refuse(ReplacementResult.OUT_OF_WORLD);
        }
        if (!world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return refuse(ReplacementResult.UNLOADED);
        }
        if (player.squaredDistanceTo(Vec3d.ofCenter(pos)) > REACH * REACH) {
            return refuse(ReplacementResult.OUT_OF_REACH);
        }

        // O alvo precisa ser o mesmo que o jogador está mirando agora no servidor.
        HitResult hit = player.raycast(REACH, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(pos)) {
            return refuse(ReplacementResult.TARGET_CHANGED);
        }

        if (!world.canPlayerModifyAt(player, pos)) {
            return refuse(ReplacementResult.NO_PERMISSION);
        }

        Optional<ReplacementResult> targetRefusal = ReplacementPolicy.evaluateTarget(world, pos, world.getBlockState(pos));
        if (targetRefusal.isPresent()) {
            return targetRefusal;
        }
        return ReplacementPolicy.evaluateNewBlock(((BlockItem) stack.getItem()).getBlock());
    }

    private static Optional<ReplacementResult> refuse(ReplacementResult result) {
        return Optional.of(result);
    }
}

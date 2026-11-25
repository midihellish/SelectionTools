package dev.buildzones.replacement;

import dev.buildzones.network.ReplaceBlockRequest;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.world.event.GameEvent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executa a substituição no thread do servidor.
 *
 * Ordem das operações: validar tudo, calcular o novo estado, checar colocação e
 * entidades; só então trocar o bloco. Drops e consumo do item acontecem depois
 * da troca bem-sucedida, então uma falha antes do ponto de não retorno não
 * apaga o bloco antigo nem gasta item.
 */
public final class ReplaceBlockService {
    private static final Logger LOGGER = LoggerFactory.getLogger("BuildZones/Replace");

    private ReplaceBlockService() {
    }

    public static ReplacementResult replace(ServerPlayerEntity player, ReplaceBlockRequest request) {
        Optional<ReplacementResult> refusal = ReplaceBlockValidator.validate(player, request);
        if (refusal.isPresent()) {
            return refusal.get();
        }
        try {
            return apply(player, request);
        } catch (RuntimeException e) {
            LOGGER.error("Falha inesperada na substituição de {}", request.pos(), e);
            return ReplacementResult.INTERNAL_ERROR;
        }
    }

    private static ReplacementResult apply(ServerPlayerEntity player, ReplaceBlockRequest request) {
        ServerWorld world = player.getServerWorld();
        BlockPos pos = request.pos();
        Hand hand = request.hand();
        ItemStack stack = player.getStackInHand(hand);
        Block newBlock = ((BlockItem) stack.getItem()).getBlock();

        BlockState oldState = world.getBlockState(pos);
        ItemPlacementContext context = PlacementContextFactory.create(world, player, hand, stack, pos, request.side());
        BlockState newState = newBlock.getPlacementState(context);
        if (newState == null) {
            return ReplacementResult.INVALID_PLACEMENT;
        }
        if (newState.equals(oldState)) {
            return ReplacementResult.SAME_BLOCK;
        }
        if (!newState.canPlaceAt(world, pos)) {
            return ReplacementResult.INVALID_PLACEMENT;
        }
        // Só entidades podem impedir a colocação; a colisão do bloco antigo não entra aqui.
        if (!world.canPlace(newState, pos, ShapeContext.of(player))) {
            return ReplacementResult.INVALID_PLACEMENT;
        }

        // Ponto de não retorno: a troca só acontece com o substituto validado.
        if (!world.setBlockState(pos, newState, Block.NOTIFY_ALL)) {
            return ReplacementResult.INVALID_PLACEMENT;
        }

        if (ReplacementPolicy.dropsOldBlock(player)) {
            // Usa a ferramenta da mão principal para que drops dependentes de ferramenta sigam o vanilla.
            Block.dropStacks(oldState, world, pos, null, player, player.getMainHandStack());
        }

        finishPlacement(player, world, pos, newState, stack);
        return ReplacementResult.APPLIED;
    }

    /**
     * Efeitos depois de um bloco já colocado: hook, som, evento de jogo e consumo do item.
     * Compartilhado com a colocação do pincel de área, para que survival se comporte igual.
     * Chamar só depois de {@code setBlockState} ter dado certo.
     */
    public static void finishPlacement(ServerPlayerEntity player, ServerWorld world, BlockPos pos,
                                       BlockState newState, ItemStack stack) {
        newState.getBlock().onPlaced(world, pos, newState, player, stack);
        BlockSoundGroup sound = newState.getSoundGroup();
        world.playSound((PlayerEntity) null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        world.emitGameEvent(player, GameEvent.BLOCK_PLACE, pos);

        if (ReplacementPolicy.consumesItem(player)) {
            stack.decrement(1);
        }
    }
}

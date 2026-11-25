package dev.buildzones.replacement;

import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * Regras centralizadas da substituição: o que é protegido, o que é recusado e
 * a política de drops e consumo. Nada disso deve ficar espalhado pelos callbacks.
 */
public final class ReplacementPolicy {

    private ReplacementPolicy() {
    }

    /** Avalia o bloco que será substituído. Vazio significa que pode prosseguir. */
    public static Optional<ReplacementResult> evaluateTarget(BlockView world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return refuse(ReplacementResult.TARGET_AIR);
        }
        // Líquidos e blocos com água: substituir apagaria o fluido sem regra de volta.
        if (!state.getFluidState().isEmpty()) {
            return refuse(ReplacementResult.TARGET_PROTECTED);
        }
        // BlockEntity na V1 fica fora: baús, fornalhas e máquinas guardam dados próprios.
        if (state.hasBlockEntity() || state.getBlock() instanceof BlockWithEntity) {
            return refuse(ReplacementResult.TARGET_PROTECTED);
        }
        if (isMultiPart(state.getBlock())) {
            return refuse(ReplacementResult.TARGET_PROTECTED);
        }
        // Hardness negativa = inquebrável (bedrock, barreira).
        if (state.getHardness(world, pos) < 0) {
            return refuse(ReplacementResult.TARGET_PROTECTED);
        }
        return Optional.empty();
    }

    /**
     * Regra de célula para operações de área. Ar é sempre aceito; o resto segue
     * {@link #evaluateTarget}, para que fluidos, BlockEntity e inquebráveis fiquem de fora.
     */
    public static Optional<ReplacementResult> evaluateAreaCell(BlockView world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return Optional.empty();
        }
        return evaluateTarget(world, pos, state);
    }

    /** Limpar: remove qualquer bloco que a política permite tocar. Ar fica como está. */
    public static boolean acceptsClear(BlockView world, BlockPos pos, BlockState state) {
        return !state.isAir() && evaluateAreaCell(world, pos, state).isEmpty();
    }

    /** Preencher: só ocupa ar ou bloco substituível (grama, neve em camada), nunca sólido. */
    public static boolean acceptsFill(BlockView world, BlockPos pos, BlockState state) {
        return (state.isAir() || state.isReplaceable()) && evaluateAreaCell(world, pos, state).isEmpty();
    }

    /** Avalia o bloco que será colocado. */
    public static Optional<ReplacementResult> evaluateNewBlock(Block block) {
        if (block instanceof BlockEntityProvider || block instanceof BlockWithEntity || isMultiPart(block)) {
            return refuse(ReplacementResult.INVALID_PLACEMENT);
        }
        return Optional.empty();
    }

    /** Portas, camas e plantas de duas metades precisam de coordenadas extras. Fora da V1. */
    public static boolean isMultiPart(Block block) {
        return block instanceof DoorBlock || block instanceof BedBlock || block instanceof TallPlantBlock;
    }

    /** Survival consome exatamente um item do stack; Creative não consome. */
    public static boolean consumesItem(PlayerEntity player) {
        return !player.getAbilities().creativeMode;
    }

    /** Survival recebe os drops normais do bloco antigo; Creative não. */
    public static boolean dropsOldBlock(PlayerEntity player) {
        return !player.getAbilities().creativeMode;
    }

    private static Optional<ReplacementResult> refuse(ReplacementResult result) {
        return Optional.of(result);
    }
}

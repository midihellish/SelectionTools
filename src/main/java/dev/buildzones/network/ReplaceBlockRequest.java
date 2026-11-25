package dev.buildzones.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Pedido de substituição enviado pelo cliente. É só uma intenção: o servidor
 * refaz o raycast e decide tudo com o estado atual do mundo e do inventário.
 */
public record ReplaceBlockRequest(BlockPos pos, Direction side, Hand hand) {

    public static ReplaceBlockRequest read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Direction side = buf.readEnumConstant(Direction.class);
        Hand hand = buf.readEnumConstant(Hand.class);
        return new ReplaceBlockRequest(pos, side, hand);
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnumConstant(side);
        buf.writeEnumConstant(hand);
    }
}

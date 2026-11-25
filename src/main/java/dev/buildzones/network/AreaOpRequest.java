package dev.buildzones.network;

import dev.buildzones.area.AreaOperation;
import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;

/**
 * Pedido de operação em massa (CLEAR, FILL) ou de desfazer (UNDO). O servidor resolve
 * a região pelo registro do jogador, comparando o id. Para UNDO o id é ignorado.
 */
public record AreaOpRequest(UUID regionId, AreaOperation operation, Hand hand) {

    public static AreaOpRequest read(PacketByteBuf buf) {
        UUID regionId = buf.readUuid();
        AreaOperation operation = buf.readEnumConstant(AreaOperation.class);
        Hand hand = buf.readEnumConstant(Hand.class);
        return new AreaOpRequest(regionId, operation, hand);
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(regionId);
        buf.writeEnumConstant(operation);
        buf.writeEnumConstant(hand);
    }
}

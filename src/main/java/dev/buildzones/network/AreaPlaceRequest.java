package dev.buildzones.network;

import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/** Pedido de colocação do pincel de área: uma célula da região ativa. */
public record AreaPlaceRequest(UUID regionId, BlockPos pos, Hand hand) {

    public static AreaPlaceRequest read(PacketByteBuf buf) {
        UUID regionId = buf.readUuid();
        BlockPos pos = buf.readBlockPos();
        Hand hand = buf.readEnumConstant(Hand.class);
        return new AreaPlaceRequest(regionId, pos, hand);
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(regionId);
        buf.writeBlockPos(pos);
        buf.writeEnumConstant(hand);
    }
}

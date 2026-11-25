package dev.buildzones.network;

import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

/** Pedido de quebra do pincel de área: um bloco dentro de uma área conhecida. */
public record AreaBreakRequest(UUID regionId, BlockPos pos) {

    public static AreaBreakRequest read(PacketByteBuf buf) {
        UUID regionId = buf.readUuid();
        BlockPos pos = buf.readBlockPos();
        return new AreaBreakRequest(regionId, pos);
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(regionId);
        buf.writeBlockPos(pos);
    }
}

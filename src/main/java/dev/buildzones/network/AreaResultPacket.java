package dev.buildzones.network;

import dev.buildzones.area.AreaResult;
import net.minecraft.network.PacketByteBuf;

/**
 * Resposta do servidor a um pedido de área. {@code changed} e {@code skipped} só têm
 * sentido em {@link AreaResult#DONE}: blocos alterados e células puladas (chunk
 * descarregado ou protegida).
 */
public record AreaResultPacket(AreaResult result, int changed, int skipped) {

    public static AreaResultPacket of(AreaResult result) {
        return new AreaResultPacket(result, 0, 0);
    }

    public static AreaResultPacket read(PacketByteBuf buf) {
        AreaResult result = buf.readEnumConstant(AreaResult.class);
        int changed = buf.readVarInt();
        int skipped = buf.readVarInt();
        return new AreaResultPacket(result, changed, skipped);
    }

    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(result);
        buf.writeVarInt(changed);
        buf.writeVarInt(skipped);
    }
}

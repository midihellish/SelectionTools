package dev.buildzones.network;

import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

/**
 * Região ativa do cliente, enviada ao confirmar, alterar ou remover. O servidor guarda
 * uma cópia e revalida tudo contra ela. {@code present=false} significa que não há
 * região ativa. O alvo (raw id do BlockState) só vem quando o modo área está ligado.
 */
public record SyncRegionPacket(boolean present, UUID id, String dimension, BlockPos anchor, BlockPos opposite,
                               boolean hasTarget, int targetRawId) {

    /** Tamanho máximo do id de dimensão, para recusar lixo vindo da rede. */
    public static final int MAX_DIMENSION_LENGTH = 128;

    public static SyncRegionPacket none() {
        return new SyncRegionPacket(false, null, null, null, null, false, 0);
    }

    public static SyncRegionPacket read(PacketByteBuf buf) {
        if (!buf.readBoolean()) {
            return none();
        }
        UUID id = buf.readUuid();
        String dimension = buf.readString(MAX_DIMENSION_LENGTH);
        BlockPos anchor = buf.readBlockPos();
        BlockPos opposite = buf.readBlockPos();
        boolean hasTarget = buf.readBoolean();
        int targetRawId = hasTarget ? buf.readVarInt() : 0;
        return new SyncRegionPacket(true, id, dimension, anchor, opposite, hasTarget, targetRawId);
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(present);
        if (!present) {
            return;
        }
        buf.writeUuid(id);
        buf.writeString(dimension, MAX_DIMENSION_LENGTH);
        buf.writeBlockPos(anchor);
        buf.writeBlockPos(opposite);
        buf.writeBoolean(hasTarget);
        if (hasTarget) {
            buf.writeVarInt(targetRawId);
        }
    }
}

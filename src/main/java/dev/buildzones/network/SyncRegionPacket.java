package dev.buildzones.network;

import dev.buildzones.area.AreaLimits;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

/**
 * Áreas que o cliente quer que o servidor conheça: as da dimensão atual, enviadas ao confirmar, alterar
 * ou remover uma área. O servidor guarda uma cópia e revalida tudo contra ela. Lista vazia significa
 * nenhuma área. O alvo (raw id do BlockState) só vem quando o pincel está ligado.
 */
public record SyncRegionPacket(List<Entry> regions, boolean hasTarget, int targetRawId) {

    /** Tamanho máximo do id de dimensão, para recusar lixo vindo da rede. */
    public static final int MAX_DIMENSION_LENGTH = 128;

    /** Uma área: id, dimensão e os dois cantos. */
    public record Entry(UUID id, String dimension, BlockPos anchor, BlockPos opposite) {
    }

    public static SyncRegionPacket none() {
        return new SyncRegionPacket(List.of(), false, 0);
    }

    public static SyncRegionPacket read(PacketByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > AreaLimits.MAX_SYNC_REGIONS) {
            throw new IllegalArgumentException("Áreas demais no pacote: " + count);
        }
        List<Entry> regions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUuid();
            String dimension = buf.readString(MAX_DIMENSION_LENGTH);
            BlockPos anchor = buf.readBlockPos();
            BlockPos opposite = buf.readBlockPos();
            regions.add(new Entry(id, dimension, anchor, opposite));
        }
        boolean hasTarget = buf.readBoolean();
        int targetRawId = hasTarget ? buf.readVarInt() : 0;
        return new SyncRegionPacket(List.copyOf(regions), hasTarget, targetRawId);
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(regions.size());
        for (Entry entry : regions) {
            buf.writeUuid(entry.id());
            buf.writeString(entry.dimension(), MAX_DIMENSION_LENGTH);
            buf.writeBlockPos(entry.anchor());
            buf.writeBlockPos(entry.opposite());
        }
        buf.writeBoolean(hasTarget);
        if (hasTarget) {
            buf.writeVarInt(targetRawId);
        }
    }
}

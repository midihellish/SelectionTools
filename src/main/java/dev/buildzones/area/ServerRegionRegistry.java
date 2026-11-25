package dev.buildzones.area;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;

/**
 * Áreas conhecidas de cada jogador, como o servidor as conhece. Nunca confia no que o pedido afirma:
 * a operação procura o id aqui. Sem entrada significa "região desconhecida".
 */
public final class ServerRegionRegistry {

    /** Cópia validada de uma área. */
    public record ActiveRegion(UUID id, String dimension, AreaBox box) {
    }

    /** Áreas do jogador e o bloco do pincel, ou nulo quando o pincel está desligado. */
    private record Snapshot(List<ActiveRegion> regions, BlockState target) {
    }

    private final Map<UUID, Snapshot> byPlayer = new ConcurrentHashMap<>();

    /** Guarda as áreas se todas estiverem dentro dos limites. Falso se alguma for recusada. */
    public boolean put(UUID playerId, List<ActiveRegion> regions, BlockState target) {
        if (regions.size() > AreaLimits.MAX_SYNC_REGIONS) {
            return false;
        }
        for (ActiveRegion region : regions) {
            if (region.id() == null || region.dimension() == null || region.dimension().isEmpty()
                    || region.box() == null || !region.box().fitsDimension(AreaLimits.MAX_DIMENSION)) {
                return false;
            }
        }
        byPlayer.put(playerId, new Snapshot(List.copyOf(regions), target));
        return true;
    }

    /** Área do jogador com esse id, se o servidor a conhece. */
    public Optional<ActiveRegion> find(UUID playerId, UUID regionId) {
        Snapshot snapshot = byPlayer.get(playerId);
        if (snapshot == null) {
            return Optional.empty();
        }
        return snapshot.regions().stream().filter(region -> region.id().equals(regionId)).findFirst();
    }

    /** Bloco do pincel do jogador, ou vazio com o pincel desligado. */
    public Optional<BlockState> target(UUID playerId) {
        Snapshot snapshot = byPlayer.get(playerId);
        return snapshot == null ? Optional.empty() : Optional.ofNullable(snapshot.target());
    }

    public void remove(UUID playerId) {
        byPlayer.remove(playerId);
    }

    public void clear() {
        byPlayer.clear();
    }
}

package dev.buildzones.area;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;

/**
 * Região ativa de cada jogador, como o servidor a conhece. Nunca confia no que o pedido
 * afirma: a operação compara o id e a dimensão daqui. Sem entrada significa "região desconhecida".
 */
public final class ServerRegionRegistry {

    /** Cópia validada da região ativa. {@code target} é o bloco do pincel, ou nulo sem modo área. */
    public record ActiveRegion(UUID id, String dimension, AreaBox box, BlockState target) {

        public Optional<BlockState> targetState() {
            return Optional.ofNullable(target);
        }
    }

    private final Map<UUID, ActiveRegion> byPlayer = new ConcurrentHashMap<>();

    /** Guarda a região se as dimensões estiverem dentro do limite. Falso se recusada. */
    public boolean put(UUID playerId, UUID regionId, String dimension, AreaBox box, BlockState target) {
        if (regionId == null || dimension == null || dimension.isEmpty() || box == null
                || !box.fitsDimension(AreaLimits.MAX_DIMENSION)) {
            return false;
        }
        byPlayer.put(playerId, new ActiveRegion(regionId, dimension, box, target));
        return true;
    }

    public Optional<ActiveRegion> get(UUID playerId) {
        return Optional.ofNullable(byPlayer.get(playerId));
    }

    public void remove(UUID playerId) {
        byPlayer.remove(playerId);
    }

    public void clear() {
        byPlayer.clear();
    }
}

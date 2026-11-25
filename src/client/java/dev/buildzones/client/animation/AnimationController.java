package dev.buildzones.client.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.util.math.Box;

/**
 * Efeitos discretos: pulso na confirmação e fantasma que some no undo/clear.
 * Usa o relógio monotônico do Minecraft e nunca altera dados de seleção.
 */
public final class AnimationController {
    public static final long PULSE_MS = 220L;
    public static final long GHOST_MS = 220L;

    /** Região que sumiu, desenhada com alpha decrescente. */
    public record Ghost(Box box, int rgb, long startMs) {
    }

    private final Map<UUID, Long> pulses = new HashMap<>();
    private final List<Ghost> ghosts = new ArrayList<>();

    public void pulse(UUID regionId, long now) {
        pulses.put(regionId, now);
    }

    /** 0 = pulso no começo, 1 = repouso. */
    public double pulseProgress(UUID regionId, long now) {
        Long start = pulses.get(regionId);
        if (start == null) {
            return 1.0;
        }
        double t = (now - start) / (double) PULSE_MS;
        if (t >= 1.0) {
            pulses.remove(regionId);
            return 1.0;
        }
        return Easing.easeOutCubic(t);
    }

    public void ghost(Box box, int rgb, long now) {
        ghosts.add(new Ghost(box, rgb, now));
    }

    /** Fantasmas ainda vivos. Os expirados são removidos aqui. */
    public List<Ghost> activeGhosts(long now) {
        Iterator<Ghost> it = ghosts.iterator();
        while (it.hasNext()) {
            if (now - it.next().startMs() >= GHOST_MS) {
                it.remove();
            }
        }
        return List.copyOf(ghosts);
    }

    /** Alpha do fantasma: 1 no início, 0 no fim, com saída suave. */
    public static double ghostAlpha(Ghost ghost, long now) {
        double t = (now - ghost.startMs()) / (double) GHOST_MS;
        return 1.0 - Easing.easeInCubic(t);
    }

    public void clear() {
        pulses.clear();
        ghosts.clear();
    }
}

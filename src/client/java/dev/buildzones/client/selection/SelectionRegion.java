package dev.buildzones.client.selection;

import dev.buildzones.client.area.AreaShape;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;

/**
 * Região confirmada. Guarda só dados primitivos e posições imutáveis;
 * não conhece input nem renderização.
 */
public final class SelectionRegion {
    private final UUID id;
    private final BlockPos anchor;
    private final BlockPos opposite;
    private final String dimension;
    private final int color;
    private final String name;
    private final boolean visible;
    private final AreaShape shape;

    public SelectionRegion(UUID id, BlockPos anchor, BlockPos opposite, String dimension,
                           int color, String name, boolean visible, AreaShape shape) {
        this.id = id;
        this.anchor = anchor.toImmutable();
        this.opposite = opposite.toImmutable();
        this.dimension = dimension;
        this.color = color;
        this.name = name;
        this.visible = visible;
        this.shape = shape == null ? AreaShape.FILL : shape;
    }

    public SelectionRegion(UUID id, BlockPos anchor, BlockPos opposite, String dimension,
                           int color, String name, boolean visible) {
        this(id, anchor, opposite, dimension, color, name, visible, AreaShape.FILL);
    }

    public SelectionRegion(UUID id, BlockPos anchor, BlockPos opposite, String dimension, int color, String name) {
        this(id, anchor, opposite, dimension, color, name, true);
    }

    public UUID id() {
        return id;
    }

    public BlockPos anchor() {
        return anchor;
    }

    public BlockPos opposite() {
        return opposite;
    }

    public String dimension() {
        return dimension;
    }

    /** Cor RGB (sem alpha). */
    public int color() {
        return color;
    }

    public String name() {
        return name;
    }

    /** Tipo da área, escolhido no menu ao marcá-la. O pincel constrói segundo ele. */
    public AreaShape shape() {
        return shape;
    }

    public boolean visible() {
        return visible;
    }

    public SelectionBounds bounds() {
        return SelectionBounds.between(anchor, opposite);
    }
}

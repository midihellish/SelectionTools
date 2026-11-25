package dev.buildzones.client.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.buildzones.client.area.AreaShape;
import dev.buildzones.client.selection.SelectionRegion;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;

/** Converte entre regiões em memória e o documento JSON. */
public final class SelectionCodec {
    public static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SelectionCodec() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static SelectionDocument toDocument(String dimension, String label, List<SelectionRegion> regions, long now) {
        SelectionDocument doc = new SelectionDocument();
        doc.schemaVersion = SCHEMA_VERSION;
        doc.dimension = dimension;
        doc.label = label;
        doc.updatedAt = now;
        for (SelectionRegion region : regions) {
            SelectionDocument.RegionEntry entry = new SelectionDocument.RegionEntry();
            entry.id = region.id().toString();
            entry.anchor = toArray(region.anchor());
            entry.opposite = toArray(region.opposite());
            entry.color = toHex(region.color());
            entry.visible = region.visible();
            entry.name = region.name();
            entry.shape = region.shape().name();
            doc.regions.add(entry);
        }
        return doc;
    }

    /** Entradas inválidas são ignoradas, sem derrubar as demais. */
    public static List<SelectionRegion> toRegions(SelectionDocument doc, String dimension) {
        List<SelectionRegion> regions = new ArrayList<>();
        if (doc == null || doc.regions == null || doc.schemaVersion != SCHEMA_VERSION) {
            return regions;
        }
        for (SelectionDocument.RegionEntry entry : doc.regions) {
            if (entry == null || entry.anchor == null || entry.opposite == null
                    || entry.anchor.length != 3 || entry.opposite.length != 3) {
                continue;
            }
            try {
                regions.add(new SelectionRegion(
                        UUID.fromString(entry.id),
                        toPos(entry.anchor),
                        toPos(entry.opposite),
                        dimension,
                        parseHex(entry.color, 0x53DDF2),
                        entry.name == null ? "Área" : entry.name,
                        entry.visible,
                        parseShape(entry.shape)));
            } catch (IllegalArgumentException e) {
                // UUID inválido: descarta só esta entrada.
            }
        }
        return regions;
    }

    /** Forma desconhecida ou ausente (arquivo antigo) vira volume inteiro. */
    private static AreaShape parseShape(String name) {
        if (name != null) {
            for (AreaShape shape : AreaShape.values()) {
                if (shape.name().equals(name)) {
                    return shape;
                }
            }
        }
        return AreaShape.FILL;
    }

    public static String toHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    public static int parseHex(String hex, int fallback) {
        if (hex == null || !hex.matches("#[0-9A-Fa-f]{6}")) {
            return fallback;
        }
        return Integer.parseInt(hex.substring(1), 16);
    }

    private static int[] toArray(BlockPos pos) {
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
    }

    private static BlockPos toPos(int[] values) {
        return new BlockPos(values[0], values[1], values[2]);
    }
}

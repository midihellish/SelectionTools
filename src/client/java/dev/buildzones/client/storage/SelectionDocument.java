package dev.buildzones.client.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Formato JSON gravado em disco. Campos públicos porque o Gson os lê direto.
 * Só contém primitivos e strings, nunca objetos do Minecraft.
 */
public final class SelectionDocument {
    public int schemaVersion = 1;
    public String dimension;
    public String label;
    public long updatedAt;
    public List<RegionEntry> regions = new ArrayList<>();

    public static final class RegionEntry {
        public String id;
        public int[] anchor;
        public int[] opposite;
        public String color;
        public boolean visible = true;
        public String name;
    }
}

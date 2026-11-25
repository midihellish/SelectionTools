package dev.buildzones.client.selection;

import dev.buildzones.client.storage.SelectionStorage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Coleção ordenada de regiões confirmadas do contexto atual (perfil + dimensão).
 * A ordem de inserção é o histórico: undo remove sempre a última.
 * Cada mutação grava no disco; nada é gravado por frame.
 */
public final class SelectionManager {
    private final SelectionStorage storage;
    private final List<SelectionRegion> regions = new ArrayList<>();
    private String profileKey;
    private String profileLabel;
    private String dimension;

    public SelectionManager(SelectionStorage storage) {
        this.storage = storage;
    }

    /**
     * Troca o contexto. Antes de trocar, o contexto anterior já foi gravado a cada mutação,
     * então só é preciso limpar a memória e carregar o novo arquivo.
     */
    public void setContext(String profileKey, String profileLabel, String dimension) {
        if (Objects.equals(this.profileKey, profileKey) && Objects.equals(this.dimension, dimension)) {
            return;
        }
        regions.clear();
        this.profileKey = profileKey;
        this.profileLabel = profileLabel;
        this.dimension = dimension;
        if (profileKey != null && dimension != null) {
            regions.addAll(storage.load(profileKey, dimension));
        }
    }

    public boolean hasContext() {
        return profileKey != null && dimension != null;
    }

    /** Dimensão do contexto atual, ex.: "minecraft:overworld". Nulo sem mundo. */
    public String dimension() {
        return dimension;
    }

    public List<SelectionRegion> regions() {
        return Collections.unmodifiableList(regions);
    }

    /** Adiciona no fim da ordem. Falso se não há contexto. */
    public boolean add(SelectionRegion region) {
        if (!hasContext()) {
            return false;
        }
        regions.add(region);
        persist();
        return true;
    }

    /** Remove a última região confirmada, na ordem de criação. */
    public Optional<SelectionRegion> undoLast() {
        if (!hasContext() || regions.isEmpty()) {
            return Optional.empty();
        }
        SelectionRegion removed = regions.remove(regions.size() - 1);
        persist();
        return Optional.of(removed);
    }

    /** Remove todas as regiões deste perfil e dimensão. Outros mundos não são tocados. */
    public List<SelectionRegion> clear() {
        if (!hasContext() || regions.isEmpty()) {
            return List.of();
        }
        List<SelectionRegion> removed = new ArrayList<>(regions);
        regions.clear();
        persist();
        return removed;
    }

    private void persist() {
        storage.save(profileKey, profileLabel, dimension, regions, System.currentTimeMillis());
    }
}

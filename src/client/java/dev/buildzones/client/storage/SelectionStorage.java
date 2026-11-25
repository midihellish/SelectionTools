package dev.buildzones.client.storage;

import dev.buildzones.client.selection.SelectionRegion;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lê e grava as regiões de um perfil de mundo e dimensão.
 * Grava em arquivo temporário e depois troca pelo final, para não corromper o JSON.
 */
public final class SelectionStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("BuildZones/Storage");

    private final Path root;

    public SelectionStorage(Path root) {
        this.root = root;
    }

    public Path fileFor(String profileKey, String dimension) {
        String safeProfile = profileKey.replaceAll("[^A-Za-z0-9_-]", "_");
        String safeDimension = dimension.replaceAll("[^A-Za-z0-9_.-]", "_");
        return root.resolve(safeProfile).resolve(safeDimension + ".json");
    }

    /** Erros de leitura viram lista vazia, com log; nunca derrubam o jogo. */
    public List<SelectionRegion> load(String profileKey, String dimension) {
        Path file = fileFor(profileKey, dimension);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            SelectionDocument doc = SelectionCodec.gson().fromJson(reader, SelectionDocument.class);
            return new ArrayList<>(SelectionCodec.toRegions(doc, dimension));
        } catch (IOException | JsonParseException | IllegalStateException e) {
            LOGGER.warn("Não foi possível ler as seleções de {}: {}", file, e.toString());
            return new ArrayList<>();
        }
    }

    /** @return true se gravou com sucesso */
    public boolean save(String profileKey, String label, String dimension, List<SelectionRegion> regions, long now) {
        Path file = fileFor(profileKey, dimension);
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            SelectionDocument doc = SelectionCodec.toDocument(dimension, label, regions, now);
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                SelectionCodec.gson().toJson(doc, writer);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Não foi possível gravar as seleções em {}", file, e);
            return false;
        }
    }
}

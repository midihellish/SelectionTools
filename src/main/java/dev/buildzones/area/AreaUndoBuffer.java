package dev.buildzones.area;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Pilha de lotes para desfazer operações em massa. Cada lote é a lista de células
 * alteradas por uma operação. Pura e genérica: não sabe de mundo nem de rede.
 *
 * Há um teto de células no total. Ao estourar, os lotes mais antigos saem primeiro.
 * Um lote maior que o teto inteiro é recusado.
 */
public final class AreaUndoBuffer<T> {
    private final long maxCells;
    private final Deque<List<T>> batches = new ArrayDeque<>();
    private long totalCells;

    public AreaUndoBuffer(long maxCells) {
        this.maxCells = maxCells;
    }

    /** Guarda um lote. Falso se o lote sozinho não cabe no teto. Lote vazio não é guardado. */
    public boolean push(List<T> batch) {
        if (batch.isEmpty()) {
            return true;
        }
        if (batch.size() > maxCells) {
            return false;
        }
        while (totalCells + batch.size() > maxCells) {
            totalCells -= batches.removeFirst().size();
        }
        batches.addLast(List.copyOf(batch));
        totalCells += batch.size();
        return true;
    }

    /** Remove e devolve o lote mais recente. */
    public Optional<List<T>> pop() {
        if (batches.isEmpty()) {
            return Optional.empty();
        }
        List<T> batch = batches.removeLast();
        totalCells -= batch.size();
        return Optional.of(batch);
    }

    public boolean isEmpty() {
        return batches.isEmpty();
    }

    public long totalCells() {
        return totalCells;
    }

    public void clear() {
        batches.clear();
        totalCells = 0;
    }
}

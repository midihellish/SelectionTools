package dev.buildzones.area;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Executa um trabalho célula a célula em fatias. Cada chamada a {@link #runSlice(int)}
 * processa no máximo o orçamento pedido, para que o tick do servidor não trave.
 * Genérica e pura: o trabalho real é o {@code step} passado pelo serviço.
 */
public final class AreaJobRunner<T> {
    private final Iterator<T> cells;
    private final Consumer<T> step;

    public AreaJobRunner(Iterator<T> cells, Consumer<T> step) {
        this.cells = cells;
        this.step = step;
    }

    /** Processa até {@code budget} itens. Devolve quantos foram processados. */
    public int runSlice(int budget) {
        int done = 0;
        while (done < budget && cells.hasNext()) {
            step.accept(cells.next());
            done++;
        }
        return done;
    }

    public boolean finished() {
        return !cells.hasNext();
    }
}

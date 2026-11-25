package dev.buildzones.client.selection;

import dev.buildzones.client.axis.AxisResolver;
import dev.buildzones.client.config.BuildZonesConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Única autoridade sobre o estado da seleção (IDLE, WAITING_SECOND_CORNER, PREVIEW).
 * Não sabe de teclado, mouse nem render: quem chama decide o que mostrar.
 */
public final class SelectionStateMachine {

    /** Resultado de uma ação, usado pelo controller para animação e HUD. */
    public enum Feedback {
        FIRST_CORNER,
        PREVIEW_STARTED,
        RESIZED,
        AT_LIMIT,
        IGNORED,
        CANCELLED,
        NOTHING_TO_CANCEL
    }

    private final SelectionManager manager;
    private final AxisResolver axisResolver;
    private SelectionState state = SelectionState.IDLE;
    private SelectionDraft draft;

    public SelectionStateMachine(SelectionManager manager, AxisResolver axisResolver) {
        this.manager = manager;
        this.axisResolver = axisResolver;
    }

    public SelectionState state() {
        return state;
    }

    public SelectionDraft draft() {
        return draft;
    }

    public Optional<SelectionDraft> draftOptional() {
        return Optional.ofNullable(draft);
    }

    /** Primeiro clique: marca o canto A e entra em WAITING_SECOND_CORNER. */
    public Feedback beginDraft(BlockPos anchor) {
        if (state == SelectionState.PREVIEW) {
            return Feedback.IGNORED;
        }
        draft = new SelectionDraft(anchor);
        state = SelectionState.WAITING_SECOND_CORNER;
        return Feedback.FIRST_CORNER;
    }

    /** Segundo clique: marca o canto B e abre a prévia. */
    public Feedback setSecondCorner(BlockPos opposite, Vec3d look) {
        if (state != SelectionState.WAITING_SECOND_CORNER) {
            return Feedback.IGNORED;
        }
        draft.setOpposite(opposite);
        draft.setActiveAxis(axisResolver.resolve(look, null));
        state = SelectionState.PREVIEW;
        return Feedback.PREVIEW_STARTED;
    }

    /** Atualiza o eixo ativo com histerese. Só vale durante a prévia. */
    public void updateAxis(Vec3d look) {
        if (state == SelectionState.PREVIEW) {
            draft.setActiveAxis(axisResolver.resolve(look, draft.activeAxis()));
        }
    }

    /**
     * Aplica passos de scroll no eixo ativo.
     *
     * @param minY menor Y válido do mundo
     * @param maxY maior Y válido do mundo
     */
    public Feedback resize(int steps, Vec3d look, int minY, int maxY) {
        if (state != SelectionState.PREVIEW || steps == 0) {
            return Feedback.IGNORED;
        }
        SelectionAxis axis = draft.activeAxis() == null ? SelectionAxis.Y : draft.activeAxis();
        int lookSign = axis.sign(look);
        boolean changed = axis == SelectionAxis.Y
                ? draft.resize(axis, steps, lookSign, minY, maxY)
                : draft.resize(axis, steps, lookSign, -BuildZonesConfig.MAX_WORLD_COORD, BuildZonesConfig.MAX_WORLD_COORD);
        return changed ? Feedback.RESIZED : Feedback.AT_LIMIT;
    }

    /** Confirma a prévia como região. Só funciona em PREVIEW e com contexto. */
    public Optional<SelectionRegion> confirm(int color) {
        if (state != SelectionState.PREVIEW || !manager.hasContext()) {
            return Optional.empty();
        }
        UUID id = UUID.randomUUID();
        String name = String.format("Área %02d", manager.regions().size() + 1);
        SelectionRegion region = new SelectionRegion(id, draft.anchor(), draft.opposite(), manager.dimension(), color, name);
        if (!manager.add(region)) {
            return Optional.empty();
        }
        reset();
        return Optional.of(region);
    }

    /** ESC: descarta só o rascunho, nunca as regiões confirmadas. */
    public Feedback cancel() {
        if (state == SelectionState.IDLE) {
            return Feedback.NOTHING_TO_CANCEL;
        }
        reset();
        return Feedback.CANCELLED;
    }

    /** ALT+Z: desfaz a última região confirmada. Mantém o rascunho, se existir. */
    public Optional<SelectionRegion> undo() {
        return manager.undoLast();
    }

    /** ALT+F: limpa as regiões do contexto e cancela o rascunho. */
    public List<SelectionRegion> clearAll() {
        reset();
        return manager.clear();
    }

    private void reset() {
        draft = null;
        state = SelectionState.IDLE;
    }
}

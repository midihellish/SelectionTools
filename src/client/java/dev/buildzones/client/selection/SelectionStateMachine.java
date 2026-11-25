package dev.buildzones.client.selection;

import dev.buildzones.client.area.AreaShape;
import dev.buildzones.client.axis.AxisResolver;
import dev.buildzones.client.config.BuildZonesConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Única autoridade sobre o estado da seleção (IDLE, ARMED, WAITING_SECOND_CORNER, PREVIEW).
 * Não sabe de teclado, mouse nem render: quem chama decide o que mostrar.
 *
 * Fluxo: arm (escolha no menu) -> beginDraft (1º clique, canto A) -> lockSecondCorner (2º clique,
 * canto B fixado; o scroll passa a ajustar) -> confirm (3º clique, salva a região).
 */
public final class SelectionStateMachine {

    /** Resultado de uma ação, usado pelo controller para animação e HUD. */
    public enum Feedback {
        ARMED,
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
    private AreaShape armedShape = AreaShape.FILL;

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

    /** Escolha no menu: a seleção fica pronta para o primeiro clique, com o tipo de área. Só sai de IDLE. */
    public Feedback arm(AreaShape shape) {
        if (state != SelectionState.IDLE) {
            return Feedback.IGNORED;
        }
        armedShape = shape;
        state = SelectionState.ARMED;
        return Feedback.ARMED;
    }

    /** Primeiro clique: marca o canto A e começa a seguir a mira. Só vale depois de armar. */
    public Feedback beginDraft(BlockPos anchor) {
        if (state != SelectionState.ARMED) {
            return Feedback.IGNORED;
        }
        draft = new SelectionDraft(anchor);
        state = SelectionState.WAITING_SECOND_CORNER;
        return Feedback.FIRST_CORNER;
    }

    /**
     * Move o canto B para o alvo da mira. Só vale enquanto o canto B não foi fixado.
     *
     * @param minY menor Y válido do mundo
     * @param maxY maior Y válido do mundo
     */
    public void aim(BlockPos target, int minY, int maxY) {
        if (state == SelectionState.WAITING_SECOND_CORNER) {
            draft.setOpposite(target, minY, maxY);
        }
    }

    /** Segundo clique: fixa o canto B onde está e libera o scroll. */
    public Feedback lockSecondCorner() {
        if (state != SelectionState.WAITING_SECOND_CORNER) {
            return Feedback.IGNORED;
        }
        state = SelectionState.PREVIEW;
        return Feedback.PREVIEW_STARTED;
    }

    /**
     * Atualiza o eixo ativo pela face da caixa que o olhar atinge, com histerese. Só vale na prévia.
     * Se o olhar não toca a caixa, vale o eixo dominante da câmera.
     */
    public void updateAxis(Vec3d eye, Vec3d look) {
        if (state == SelectionState.PREVIEW) {
            draft.setActiveAxis(axisResolver.resolveFace(eye, look, draft.bounds().toBox(), draft.activeAxis()));
        }
    }

    /** Atualiza o eixo ativo pelo eixo dominante da câmera, com histerese. Só vale na prévia, com o canto B fixado. */
    public void updateAxis(Vec3d look) {
        if (state == SelectionState.PREVIEW) {
            draft.setActiveAxis(axisResolver.resolve(look, draft.activeAxis()));
        }
    }

    /**
     * Aplica passos de scroll no eixo ativo. Só na prévia.
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

    /** Terceiro clique: salva a região entre A e o canto B fixado. Só na prévia, com contexto. */
    public Optional<SelectionRegion> confirm(int color) {
        if (state != SelectionState.PREVIEW || !manager.hasContext()) {
            return Optional.empty();
        }
        UUID id = UUID.randomUUID();
        String name = String.format("Área %02d", manager.regions().size() + 1);
        SelectionRegion region = new SelectionRegion(id, draft.anchor(), draft.opposite(), manager.dimension(), color, name,
                true, armedShape);
        if (!manager.add(region)) {
            return Optional.empty();
        }
        reset();
        return Optional.of(region);
    }

    /** ESC ou toque em ALT: descarta só a escolha ou o rascunho, nunca as regiões confirmadas. */
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
        armedShape = AreaShape.FILL;
        state = SelectionState.IDLE;
    }
}

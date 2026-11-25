package dev.buildzones.client.rendering;

import dev.buildzones.client.animation.AnimatedBounds;
import dev.buildzones.client.animation.AnimationController;
import dev.buildzones.client.config.BuildZonesConfig;
import dev.buildzones.client.selection.SelectionDraft;
import dev.buildzones.client.selection.SelectionManager;
import dev.buildzones.client.selection.SelectionAxis;
import dev.buildzones.client.selection.SelectionRegion;
import dev.buildzones.client.selection.SelectionState;
import dev.buildzones.client.selection.SelectionStateMachine;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Desenha regiões confirmadas, fantasmas de undo/clear e a seleção em andamento.
 * Só lê o estado; nunca altera dados de seleção.
 *
 * Ordem de desenho: primeiro todas as faces (uma camada), depois todas as arestas
 * (outra camada). O VertexConsumerProvider do vanilla encerra a camada anterior
 * quando outra é pedida, então cada buffer é obtido só uma vez por passada.
 */
public final class SelectionWorldRenderer {
    private static final double MAX_FRAME_DT = 0.1;

    private final SelectionStateMachine machine;
    private final SelectionManager manager;
    private final AnimationController animations;
    private final AnimatedBounds draftBounds = new AnimatedBounds();
    private long lastFrameMs = -1L;

    /** Rascunho para este frame, já com a animação avançada. Nulo fora da seleção. */
    private record DraftView(Box box, Vec3d anchor, Vec3d opposite, int axisMask) {
    }

    public SelectionWorldRenderer(SelectionStateMachine machine, SelectionManager manager,
                                  AnimationController animations) {
        this.machine = machine;
        this.manager = manager;
        this.animations = animations;
    }

    public void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::render);
    }

    private void render(WorldRenderContext context) {
        VertexConsumerProvider consumers = context.consumers();
        Camera camera = context.camera();
        MatrixStack matrices = context.matrixStack();
        if (consumers == null || camera == null || matrices == null) {
            return;
        }

        Vec3d cam = camera.getPos();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        long now = Util.getMeasuringTimeMs();
        double dt = lastFrameMs < 0 ? 0.0 : Math.min(MAX_FRAME_DT, (now - lastFrameMs) / 1000.0);
        lastFrameMs = now;

        // Cálculos do frame feitos uma vez, antes de pedir qualquer buffer.
        List<SelectionRegion> regions = visibleRegions(cam);
        List<AnimationController.Ghost> ghosts = animations.activeGhosts(now);
        DraftView draft = prepareDraft(dt);

        VertexConsumer faces = consumers.getBuffer(GhostRenderLayers.FACES);
        drawFaces(faces, matrix, cam, now, regions, ghosts, draft);

        VertexConsumer lines = consumers.getBuffer(GhostRenderLayers.EDGES);
        drawEdges(lines, matrix, cam, now, regions, ghosts, draft);

        flush(consumers);
    }

    private List<SelectionRegion> visibleRegions(Vec3d cam) {
        List<SelectionRegion> visible = new ArrayList<>();
        for (SelectionRegion region : manager.regions()) {
            if (region.visible() && inRange(cam, region.bounds().toBox())) {
                visible.add(region);
            }
        }
        return visible;
    }

    /** Avança a animação da caixa da seleção. Chamado uma vez por frame. */
    private DraftView prepareDraft(double dt) {
        SelectionDraft draft = machine.draft();
        if (machine.state() == SelectionState.IDLE || draft == null) {
            draftBounds.reset();
            return null;
        }
        Vec3d anchor = Vec3d.ofCenter(draft.anchor());
        // A caixa nasce no canto A e segue a mira, convergindo em vez de pular.
        if (!draftBounds.seeded()) {
            draftBounds.seed(Box.of(anchor, 1.0e-3, 1.0e-3, 1.0e-3));
        }
        Box box = draftBounds.approach(draft.bounds().toBox(), dt);
        // O eixo só aparece na prévia: antes do scroll, a câmera não escolhe eixo ainda.
        SelectionAxis axis = draft.activeAxis() == null ? SelectionAxis.Y : draft.activeAxis();
        int axisMask = machine.state() != SelectionState.PREVIEW ? 0 : switch (axis) {
            case X -> SelectionGeometry.X_MASK;
            case Y -> SelectionGeometry.Y_MASK;
            case Z -> SelectionGeometry.Z_MASK;
        };
        return new DraftView(box, anchor, Vec3d.ofCenter(draft.opposite()), axisMask);
    }

    private void drawFaces(VertexConsumer faces, Matrix4f matrix, Vec3d cam, long now,
                           List<SelectionRegion> regions, List<AnimationController.Ghost> ghosts,
                           DraftView draft) {
        for (SelectionRegion region : regions) {
            double pulse = animations.pulseProgress(region.id(), now);
            int boost = (int) ((1.0 - pulse) * RenderStyle.PULSE_BOOST);
            SelectionGeometry.faces(faces, matrix, cam, region.bounds().toBox(),
                    RenderStyle.argb(region.color(), RenderStyle.FILL_ALPHA + boost));
        }
        for (AnimationController.Ghost ghost : ghosts) {
            int alpha = (int) (AnimationController.ghostAlpha(ghost, now) * 255);
            SelectionGeometry.faces(faces, matrix, cam, ghost.box(),
                    RenderStyle.argb(ghost.rgb(), alpha * RenderStyle.FILL_ALPHA / 255));
        }
        if (draft == null) {
            return;
        }
        SelectionGeometry.faces(faces, matrix, cam, draft.box(),
                RenderStyle.argb(RenderStyle.PREVIEW_RGB, RenderStyle.PREVIEW_FILL_ALPHA));
        SelectionGeometry.vertexMarkers(faces, matrix, cam, draft.box(), RenderStyle.MARKER_SIZE,
                RenderStyle.argb(RenderStyle.PREVIEW_RGB, 255));
        SelectionGeometry.marker(faces, matrix, cam, draft.anchor(), RenderStyle.ANCHOR_MARKER_SIZE,
                RenderStyle.argb(RenderStyle.ANCHOR_RGB, 255));
        SelectionGeometry.marker(faces, matrix, cam, draft.opposite(), RenderStyle.ANCHOR_MARKER_SIZE,
                RenderStyle.argb(RenderStyle.OPPOSITE_RGB, 255));
    }

    private void drawEdges(VertexConsumer lines, Matrix4f matrix, Vec3d cam, long now,
                           List<SelectionRegion> regions, List<AnimationController.Ghost> ghosts,
                           DraftView draft) {
        for (SelectionRegion region : regions) {
            double pulse = animations.pulseProgress(region.id(), now);
            int boost = (int) ((1.0 - pulse) * RenderStyle.PULSE_BOOST);
            SelectionGeometry.edges(lines, matrix, cam, region.bounds().toBox(),
                    RenderStyle.argb(region.color(), Math.min(255, RenderStyle.EDGE_ALPHA + boost)),
                    SelectionGeometry.ALL_AXES);
        }
        for (AnimationController.Ghost ghost : ghosts) {
            int alpha = (int) (AnimationController.ghostAlpha(ghost, now) * 255);
            SelectionGeometry.edges(lines, matrix, cam, ghost.box(),
                    RenderStyle.argb(ghost.rgb(), alpha), SelectionGeometry.ALL_AXES);
        }
        if (draft == null) {
            return;
        }
        SelectionGeometry.edges(lines, matrix, cam, draft.box(),
                RenderStyle.argb(RenderStyle.PREVIEW_RGB, RenderStyle.PREVIEW_EDGE_ALPHA), SelectionGeometry.ALL_AXES);
        // Eixo ativo em âmbar, para o jogador saber o que o scroll vai mudar.
        SelectionGeometry.edges(lines, matrix, cam, draft.box(),
                RenderStyle.argb(RenderStyle.AXIS_RGB, 255), draft.axisMask());
    }

    private static boolean inRange(Vec3d cam, Box box) {
        double cx = (box.minX + box.maxX) / 2.0;
        double cy = (box.minY + box.maxY) / 2.0;
        double cz = (box.minZ + box.maxZ) / 2.0;
        double limit = BuildZonesConfig.RENDER_DISTANCE;
        return cam.squaredDistanceTo(cx, cy, cz) <= limit * limit;
    }

    /** O Immediate precisa ser descarregado para o que foi escrito aparecer neste frame. */
    private static void flush(VertexConsumerProvider consumers) {
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }
}

package dev.buildzones.client.rendering;

import dev.buildzones.client.area.AreaModeController;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Fantasmas do pincel de área: células vazias recebem preenchimento e contorno azul;
 * blocos que não pertencem ao alvo recebem só o contorno vermelho de remoção.
 * Só lê o {@link AreaModeController}; não altera dados.
 */
public final class AreaGhostRenderer {
    /** Deslocamento do vizinho para cada índice de face de {@link SelectionGeometry#face}. */
    private static final int[][] NEIGHBOR_OFFSETS = {
            {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1},
    };

    private final AreaModeController area;

    public AreaGhostRenderer(AreaModeController area) {
        this.area = area;
    }

    public void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::render);
    }

    private void render(WorldRenderContext context) {
        if (!area.isActive()) {
            return;
        }
        List<BlockPos> missing = area.missingCells();
        List<BlockPos> wrong = area.wrongCells();
        if (missing.isEmpty() && wrong.isEmpty()) {
            return;
        }
        VertexConsumerProvider consumers = context.consumers();
        Camera camera = context.camera();
        MatrixStack matrices = context.matrixStack();
        if (consumers == null || camera == null || matrices == null) {
            return;
        }
        Vec3d cam = camera.getPos();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Só o contorno do conjunto de fantasmas: face voltada para ar. Faces internas entre
        // células vizinhas não são desenhadas, então o alfa não acumula e não aparece grade.
        VertexConsumer faces = consumers.getBuffer(GhostRenderLayers.FACES);
        int fill = RenderStyle.argb(RenderStyle.AREA_GHOST_RGB, RenderStyle.AREA_GHOST_FILL_ALPHA);
        Set<BlockPos> ghostSet = new HashSet<>(missing);
        for (BlockPos pos : missing) {
            for (int dir = 0; dir < NEIGHBOR_OFFSETS.length; dir++) {
                BlockPos neighbor = pos.add(NEIGHBOR_OFFSETS[dir][0], NEIGHBOR_OFFSETS[dir][1], NEIGHBOR_OFFSETS[dir][2]);
                if (!ghostSet.contains(neighbor)) {
                    SelectionGeometry.face(faces, matrix, cam, cellBox(pos), dir, fill);
                }
            }
        }

        // Contorno de remoção: só arestas, porque blocos errados são poucos e precisam de destaque.
        VertexConsumer lines = consumers.getBuffer(GhostRenderLayers.EDGES);
        int removeEdge = RenderStyle.argb(RenderStyle.AREA_REMOVE_RGB, RenderStyle.AREA_REMOVE_EDGE_ALPHA);
        for (BlockPos pos : wrong) {
            SelectionGeometry.edges(lines, matrix, cam, cellBox(pos), removeEdge, SelectionGeometry.ALL_AXES);
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private static Box cellBox(BlockPos pos) {
        return new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
    }
}

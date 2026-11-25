package dev.buildzones.client.rendering;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

/**
 * Camadas de desenho do BuildZones.
 *
 * Translúcido, sem escrita no depth buffer e sem culling. Assim as caixas são
 * overlays: não escondem blocos atrás delas, não brigam com faces de bloco
 * (sem z-fighting) e a ordem de desenho entre caixas sobrepostas deixa de
 * causar piscadas. O teste de profundidade continua ativo (LEQUAL), então
 * terreno e construções na frente ainda ocultam a caixa.
 */
public final class GhostRenderLayers extends RenderLayer {
    // MultiPhaseParameters é protegida no Yarn 1.20.1: só uma subclasse de RenderLayer consegue
    // montar as fases. Esta classe nunca é instanciada; o construtor só satisfaz o super.

    /** Faces: quads translúcidos, sem culling, sem escrita de profundidade. */
    public static final RenderLayer FACES = RenderLayer.of(
            "buildzones_ghost_faces",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            256,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false));

    /** Arestas: linhas translúcidas, mesma regra de profundidade das faces. */
    public static final RenderLayer EDGES = RenderLayer.of(
            "buildzones_ghost_edges",
            VertexFormats.LINES,
            VertexFormat.DrawMode.LINES,
            256,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false));

    private GhostRenderLayers() {
        super("buildzones_unused", VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, 256, false, true,
                () -> { }, () -> { });
    }
}

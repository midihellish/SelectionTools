package dev.buildzones.client.rendering;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Gera vértices de faces, arestas e marcadores para uma caixa.
 * Custo fixo por caixa (6 faces, 12 arestas, 8 marcadores), independente do volume.
 * Coordenadas chegam em mundo e são deslocadas pela câmera antes do envio.
 */
public final class SelectionGeometry {
    /** Afastamento mínimo da superfície para evitar z-fighting com o bloco. */
    private static final double EPSILON = 0.005;

    /** Os 4 cantos de cada face, em ordem de contorno. Índice = bit x | bit y << 1 | bit z << 2. */
    private static final int[][] FACES = {
            {0, 2, 6, 4},
            {1, 5, 7, 3},
            {0, 4, 5, 1},
            {2, 3, 7, 6},
            {0, 1, 3, 2},
            {4, 6, 7, 5},
    };

    public static final int ALL_AXES = 0b111;
    public static final int X_MASK = 0b001;
    public static final int Y_MASK = 0b010;
    public static final int Z_MASK = 0b100;

    private SelectionGeometry() {
    }

    /** Faces translúcidas, uma vez cada. A camada não tem culling, então o sentido do quad não importa. */
    public static void faces(VertexConsumer vc, Matrix4f matrix, Vec3d camera, Box box, int argb) {
        Vec3d[] corners = corners(box);
        for (int[] face : FACES) {
            quad(vc, matrix, camera, corners, face, argb, false);
        }
    }

    /**
     * Uma face só. Índice segue a tabela FACES: 0 = -X, 1 = +X, 2 = -Y, 3 = +Y, 4 = -Z, 5 = +Z.
     * Usado quando só parte da caixa deve aparecer, por exemplo o contorno de um conjunto de células.
     */
    public static void face(VertexConsumer vc, Matrix4f matrix, Vec3d camera, Box box, int faceIndex, int argb) {
        quad(vc, matrix, camera, corners(box), FACES[faceIndex], argb, false);
    }

    /**
     * Arestas. A máscara escolhe quais eixos desenhar: ALL_AXES para a caixa inteira,
     * ou só um eixo para destacar o ativo.
     */
    public static void edges(VertexConsumer vc, Matrix4f matrix, Vec3d camera, Box box, int argb, int axisMask) {
        Vec3d[] corners = corners(box);
        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit <= 4; bit <<= 1) {
                if ((i & bit) != 0 || (bit & axisMask) == 0) {
                    continue;
                }
                int j = i | bit;
                Vec3d direction = corners[j].subtract(corners[i]).normalize();
                line(vc, matrix, camera, corners[i], corners[j], direction, argb);
            }
        }
    }

    /** Cubo pequeno em um ponto, usado para marcar os cantos. */
    public static void marker(VertexConsumer vc, Matrix4f matrix, Vec3d camera, Vec3d center, double size, int argb) {
        double h = size / 2.0;
        faces(vc, matrix, camera, new Box(center.x - h, center.y - h, center.z - h,
                center.x + h, center.y + h, center.z + h), argb);
    }

    /** Marcadores nos oito vértices da caixa. */
    public static void vertexMarkers(VertexConsumer vc, Matrix4f matrix, Vec3d camera, Box box, double size, int argb) {
        Vec3d[] corners = corners(box);
        for (Vec3d corner : corners) {
            marker(vc, matrix, camera, corner, size, argb);
        }
    }

    private static Vec3d[] corners(Box box) {
        Vec3d[] corners = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? box.minX - EPSILON : box.maxX + EPSILON;
            double y = (i & 2) == 0 ? box.minY - EPSILON : box.maxY + EPSILON;
            double z = (i & 4) == 0 ? box.minZ - EPSILON : box.maxZ + EPSILON;
            corners[i] = new Vec3d(x, y, z);
        }
        return corners;
    }

    private static void quad(VertexConsumer vc, Matrix4f matrix, Vec3d camera, Vec3d[] corners,
                             int[] face, int argb, boolean reversed) {
        for (int k = 0; k < 4; k++) {
            int index = reversed ? face[3 - k] : face[k];
            vertex(vc, matrix, camera, corners[index], argb);
        }
    }

    private static void line(VertexConsumer vc, Matrix4f matrix, Vec3d camera, Vec3d a, Vec3d b,
                             Vec3d normal, int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int bl = argb & 0xFF;
        int alpha = (argb >>> 24) & 0xFF;
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;
        vc.vertex(matrix, (float) (a.x - camera.x), (float) (a.y - camera.y), (float) (a.z - camera.z))
                .color(r, g, bl, alpha).normal(nx, ny, nz).next();
        vc.vertex(matrix, (float) (b.x - camera.x), (float) (b.y - camera.y), (float) (b.z - camera.z))
                .color(r, g, bl, alpha).normal(nx, ny, nz).next();
    }

    private static void vertex(VertexConsumer vc, Matrix4f matrix, Vec3d camera, Vec3d p, int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int alpha = (argb >>> 24) & 0xFF;
        vc.vertex(matrix, (float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z))
                .color(r, g, b, alpha).next();
    }
}

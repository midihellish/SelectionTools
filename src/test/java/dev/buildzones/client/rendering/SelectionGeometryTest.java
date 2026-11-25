package dev.buildzones.client.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

/**
 * Regressão do glitch de faces: cada face deve sair UMA vez. Emitir a mesma face
 * de novo, com ordem invertida, triangula a face em diagonais diferentes e a
 * sobreposição translúcida vira listras diagonais.
 */
class SelectionGeometryTest {

    /** Conta vértices e primitivas enviados. Proxy evita implementar toda a interface. */
    private static final class CountingConsumer {
        int vertices;
        final VertexConsumer consumer = (VertexConsumer) Proxy.newProxyInstance(
                VertexConsumer.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "vertex" -> {
                            // vertex(Matrix4f, x, y, z) é o único caminho usado pelo renderer
                            return proxy;
                        }
                        case "color", "normal" -> {
                            return proxy;
                        }
                        case "next" -> {
                            vertices++;
                            return null;
                        }
                        default -> {
                            return null;
                        }
                    }
                });
    }

    private static final Box UNIT = new Box(0, 0, 0, 1, 1, 1);

    @Test
    void boxFacesAreEmittedOncePerFace() {
        CountingConsumer c = new CountingConsumer();
        SelectionGeometry.faces(c.consumer, new Matrix4f(), new Vec3d(0, 0, 0), UNIT, 0xFF53DDF2);
        assertEquals(6 * 4, c.vertices, "6 faces × 4 vértices, sem duplicar");
    }

    @Test
    void boxEdgesAreTwelveLines() {
        CountingConsumer c = new CountingConsumer();
        SelectionGeometry.edges(c.consumer, new Matrix4f(), new Vec3d(0, 0, 0), UNIT, 0xFF53DDF2,
                SelectionGeometry.ALL_AXES);
        assertEquals(12 * 2, c.vertices, "12 arestas × 2 vértices");
    }
}

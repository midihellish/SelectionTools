package dev.buildzones.client.selection;

import net.minecraft.util.math.Vec3d;

public enum SelectionAxis {
    X, Y, Z;

    /** Componente do vetor no eixo. */
    public double component(Vec3d vector) {
        return switch (this) {
            case X -> vector.x;
            case Y -> vector.y;
            case Z -> vector.z;
        };
    }

    /** Sinal do componente: -1, 0 ou 1. */
    public int sign(Vec3d vector) {
        return (int) Math.signum(component(vector));
    }
}

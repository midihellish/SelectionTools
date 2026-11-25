package dev.buildzones.client.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Estado físico do ALT, consultado direto no GLFW. */
public final class ModifierKeyState {

    private ModifierKeyState() {
    }

    public static boolean isAltDown() {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_ALT)
                || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_ALT);
    }
}

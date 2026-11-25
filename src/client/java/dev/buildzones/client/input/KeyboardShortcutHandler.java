package dev.buildzones.client.input;

import dev.buildzones.area.AreaOperation;
import dev.buildzones.client.BuildZonesClient;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Atalhos do BuildZones e o ESC para cancelar a prévia.
 * Só age no momento do pressionamento e sem tela aberta.
 * Retorna true apenas quando um atalho do BuildZones foi executado,
 * para que o mixin cancele o evento e nenhuma função vanilla rode junto.
 *
 * Mapa: ALT+Z desfaz região, CTRL+ALT+Z desfaz blocos, ALT+F limpa regiões,
 * ALT+B liga/desliga o modo área, ALT+C limpa a área e ALT+V preenche a área.
 */
public final class KeyboardShortcutHandler {

    private KeyboardShortcutHandler() {
    }

    public static boolean handle(int action, int key, int modifiers) {
        if (action != GLFW.GLFW_PRESS) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) {
            return false;
        }
        var controller = BuildZonesClient.controller();
        if (controller == null) {
            return false;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE && controller.isDrafting()) {
            return controller.cancelDraft();
        }
        boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        if (!alt) {
            return false;
        }
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (key == GLFW.GLFW_KEY_Z) {
            return ctrl ? controller.areaUndoBlocks() : controller.undo();
        }
        if (key == GLFW.GLFW_KEY_F) {
            return controller.clear();
        }
        if (key == GLFW.GLFW_KEY_B) {
            return controller.toggleAreaMode();
        }
        if (key == GLFW.GLFW_KEY_C) {
            return controller.areaBulk(AreaOperation.CLEAR);
        }
        if (key == GLFW.GLFW_KEY_V) {
            return controller.areaBulk(AreaOperation.FILL);
        }
        return false;
    }
}

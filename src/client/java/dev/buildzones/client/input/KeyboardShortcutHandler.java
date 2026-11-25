package dev.buildzones.client.input;

import dev.buildzones.area.AreaOperation;
import dev.buildzones.client.BuildZonesClient;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Teclado do BuildZones, sem tela aberta.
 *
 * - Tocar e soltar a tecla de modificador: abre o menu radial, ou para a ação em curso.
 * - Modificador + tecla: atalhos (ALT+Z desfaz região, CTRL+ALT+Z desfaz blocos, ALT+F limpa regiões,
 *   ALT+C limpa a área e ALT+V preenche a área).
 * - ESC: cancela a escolha ou o rascunho, se houver.
 *
 * Retorna true só quando um atalho foi executado, para o mixin cancelar o evento.
 */
public final class KeyboardShortcutHandler {

    private KeyboardShortcutHandler() {
    }

    public static boolean handle(int action, int key, int scancode, int modifiers) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) {
            // Telas (inclusive o menu radial) tratam as próprias teclas.
            return false;
        }
        var controller = BuildZonesClient.controller();
        if (controller == null) {
            return false;
        }

        boolean modifierKey = ModifierKeyState.matches(key, scancode);
        if (action == GLFW.GLFW_RELEASE) {
            if (modifierKey) {
                controller.modifierReleased();
            }
            return false;
        }
        if (action != GLFW.GLFW_PRESS) {
            return false;
        }
        if (modifierKey) {
            controller.modifierPressed();
            return false;
        }
        controller.otherInputPressed();

        if (key == GLFW.GLFW_KEY_ESCAPE && controller.isDrafting()) {
            return controller.cancelDraft();
        }
        if (!ModifierKeyState.isHeld()) {
            return false;
        }
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        return switch (key) {
            case GLFW.GLFW_KEY_Z -> ctrl ? controller.areaUndoBlocks() : controller.undo();
            case GLFW.GLFW_KEY_F -> controller.clear();
            case GLFW.GLFW_KEY_C -> controller.areaBulk(AreaOperation.CLEAR);
            case GLFW.GLFW_KEY_V -> controller.areaBulk(AreaOperation.FILL);
            default -> false;
        };
    }
}

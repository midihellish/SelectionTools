package dev.buildzones.client.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

/**
 * Estado físico da tecla de modificador configurada em Controles. Consulta o GLFW direto,
 * para não depender de quando o vanilla atualiza o keybinding.
 */
public final class ModifierKeyState {

    private ModifierKeyState() {
    }

    /** Verdadeiro se a tecla de modificador está pressionada agora. */
    public static boolean isHeld() {
        InputUtil.Key key = InputUtil.fromTranslationKey(ModKeys.MODIFIER.getBoundKeyTranslationKey());
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, key.getCode());
    }

    /** Verdadeiro se o evento de teclado é a tecla de modificador configurada. */
    public static boolean matches(int keyCode, int scanCode) {
        return ModKeys.MODIFIER.matchesKey(keyCode, scanCode);
    }

    /** Nome da tecla como o jogador a vê em Controles, para textos do HUD. */
    public static String name() {
        return ModKeys.MODIFIER.getBoundKeyLocalizedText().getString();
    }
}

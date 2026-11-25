package dev.buildzones.client.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Teclas do mod no menu Opções > Controles, na categoria Selection Tools. */
public final class ModKeys {
    private static final String CATEGORY = "key.categories.buildzones";

    /**
     * Modificador do menu radial. Tocar e soltar abre o menu, ou para a ação em curso.
     * Segurado com outra tecla, vira atalho (ALT+Z, ALT+F, ALT+C, ALT+V, CTRL+ALT+Z).
     */
    public static KeyBinding MODIFIER;

    private ModKeys() {
    }

    /** Registra as teclas. Chamado uma vez, na inicialização do cliente, antes de as opções serem lidas. */
    public static void register() {
        MODIFIER = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.buildzones.modifier", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY));
    }
}

package dev.buildzones.client.mixin;

import dev.buildzones.client.input.KeyboardShortcutHandler;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captura ALT+Z, ALT+F e ESC (com rascunho) antes do vanilla.
 *
 * Alvo: {@code Keyboard.onKey(long, int, int, int, int)}. Um keybinding do jogo
 * (como F, troca de mão) é disparado dentro desse método; cancelar no HEAD impede
 * o comportamento vanilla só para o atalho reconhecido.
 */
@Mixin(Keyboard.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void buildzones$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (KeyboardShortcutHandler.handle(action, key, modifiers)) {
            ci.cancel();
        }
    }
}

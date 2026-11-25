package dev.buildzones.client.mixin;

import dev.buildzones.client.BuildZonesClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captura o scroll do mouse durante a prévia.
 *
 * Alvo: {@code Mouse.onMouseScroll(long, double, double)} (privado em 1.20.1, sem evento Fabric equivalente).
 * Consome o evento no HEAD só quando o controller confirma que usou o scroll;
 * fora da prévia o método vanilla segue e a hotbar troca normalmente.
 */
@Mixin(Mouse.class)
public abstract class MouseScrollMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void buildzones$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        var controller = BuildZonesClient.controller();
        if (controller != null && controller.handleScroll(vertical)) {
            ci.cancel();
        }
    }
}

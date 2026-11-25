package dev.buildzones.client.mixin;

import dev.buildzones.client.BuildZonesClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captura os cliques da seleção: o 1º marca o canto A e o 2º salva a região.
 *
 * Alvo: {@code Mouse.onMouseButton(long, int, int, int)} (privado em 1.20.1). O clique no ar
 * não chega ao callback de ataque do Fabric, então os cliques da seleção precisam vir daqui.
 * Consome o evento no HEAD só quando o controller confirma; fora da seleção o vanilla segue.
 */
@Mixin(Mouse.class)
public abstract class MouseButtonMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void buildzones$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        var controller = BuildZonesClient.controller();
        if (controller != null && controller.handleMouseButton(window, button, action)) {
            ci.cancel();
        }
    }
}

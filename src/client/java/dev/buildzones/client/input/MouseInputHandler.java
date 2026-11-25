package dev.buildzones.client.input;

import dev.buildzones.client.BuildZonesClient;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;

/**
 * Liga o callback Fabric de uso de bloco ao controller (substituição com modificador e pincel de área).
 * O callback também dispara no servidor integrado do singleplayer, por isso a checagem de {@code isClient}.
 * Os cliques da seleção não passam por aqui: vêm de MouseButtonMixin, que funciona também no ar.
 */
public final class MouseInputHandler {

    private MouseInputHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient) {
                return ActionResult.PASS;
            }
            return BuildZonesClient.controller().onUseBlock(player, hand, hitResult);
        });
    }
}

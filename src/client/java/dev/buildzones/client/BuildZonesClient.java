package dev.buildzones.client;

import dev.buildzones.client.animation.AnimationController;
import dev.buildzones.client.area.AreaModeController;
import dev.buildzones.client.axis.AxisResolver;
import dev.buildzones.client.config.BuildZonesConfig;
import dev.buildzones.client.hud.SelectionHud;
import dev.buildzones.client.input.MouseInputHandler;
import dev.buildzones.client.input.SelectionInputController;
import dev.buildzones.client.rendering.AreaGhostRenderer;
import dev.buildzones.client.rendering.SelectionWorldRenderer;
import dev.buildzones.client.selection.SelectionManager;
import dev.buildzones.client.selection.SelectionStateMachine;
import dev.buildzones.client.storage.SelectionStorage;
import dev.buildzones.network.AreaResultPacket;
import dev.buildzones.network.ReplaceBlockResult;
import dev.buildzones.replacement.ReplacementResult;
import dev.buildzones.util.ModIds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Entrypoint do cliente. Monta os serviços uma única vez e expõe só o controller,
 * que é a referência usada pelos mixins e callbacks.
 */
public class BuildZonesClient implements ClientModInitializer {
    private static SelectionInputController controller;

    /** Controller ativo, ou nulo antes da inicialização do cliente. */
    public static SelectionInputController controller() {
        return controller;
    }

    @Override
    public void onInitializeClient() {
        var root = FabricLoader.getInstance().getConfigDir().resolve("buildzones").resolve("worlds");
        SelectionStorage storage = new SelectionStorage(root);
        SelectionManager manager = new SelectionManager(storage);
        SelectionStateMachine machine = new SelectionStateMachine(
                manager, new AxisResolver(BuildZonesConfig.AXIS_HYSTERESIS));
        AnimationController animations = new AnimationController();
        SelectionHud hud = new SelectionHud(machine);
        AreaModeController area = new AreaModeController(manager, hud);

        controller = new SelectionInputController(machine, manager, animations, hud, area);

        new SelectionWorldRenderer(machine, manager, animations).register();
        new AreaGhostRenderer(area).register();
        hud.register();
        MouseInputHandler.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> controller.tick(client));

        ClientPlayNetworking.registerGlobalReceiver(ModIds.REPLACE_RESULT, (client, handler, buf, responseSender) -> {
            ReplacementResult result = ReplaceBlockResult.read(buf).result();
            client.execute(() -> hud.show(result.key()));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModIds.AREA_RESULT, (client, handler, buf, responseSender) -> {
            AreaResultPacket packet = AreaResultPacket.read(buf);
            client.execute(() -> controller.onAreaResult(packet));
        });
    }
}

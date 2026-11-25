package dev.buildzones;

import dev.buildzones.area.AreaOperationService;
import dev.buildzones.network.BuildZonesNetworking;
import net.fabricmc.api.ModInitializer;

/**
 * Entrypoint comum. Roda em cliente e servidor.
 * Registra os receptores de rede e o serviço de área no servidor; a lógica de seleção é exclusiva do cliente.
 */
public class BuildZones implements ModInitializer {
    public static final String MOD_ID = "buildzones";

    @Override
    public void onInitialize() {
        BuildZonesNetworking.registerServerReceivers();
        AreaOperationService.register();
    }
}

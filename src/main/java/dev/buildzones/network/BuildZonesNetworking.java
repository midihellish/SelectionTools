package dev.buildzones.network;

import dev.buildzones.area.AreaOperationService;
import dev.buildzones.replacement.ReplaceBlockService;
import dev.buildzones.replacement.ReplacementResult;
import dev.buildzones.util.ModIds;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BuildZonesNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger("BuildZones/Network");

    private BuildZonesNetworking() {
    }

    /**
     * Registra o receptor no servidor. O pacote é lido na thread de rede e a
     * mutação do mundo é agendada para a thread do servidor.
     */
    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ModIds.REPLACE_REQUEST, (server, player, handler, buf, responseSender) -> {
            ReplaceBlockRequest request;
            try {
                request = ReplaceBlockRequest.read(buf);
            } catch (RuntimeException e) {
                // Pacote malformado: rejeita sem mutação e sem despejar stack trace repetido.
                LOGGER.warn("Pacote de substituição malformado de {}", player.getName().getString());
                return;
            }

            server.execute(() -> {
                ReplacementResult result = ReplaceBlockService.replace(player, request);
                PacketByteBuf out = PacketByteBufs.create();
                new ReplaceBlockResult(result).write(out);
                ServerPlayNetworking.send(player, ModIds.REPLACE_RESULT, out);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ModIds.SYNC_REGION, (server, player, handler, buf, responseSender) -> {
            SyncRegionPacket packet;
            try {
                packet = SyncRegionPacket.read(buf);
            } catch (RuntimeException e) {
                LOGGER.warn("Pacote de região malformado de {}", player.getName().getString());
                return;
            }
            server.execute(() -> AreaOperationService.sync(player, packet));
        });

        ServerPlayNetworking.registerGlobalReceiver(ModIds.AREA_OP_REQUEST, (server, player, handler, buf, responseSender) -> {
            AreaOpRequest request;
            try {
                request = AreaOpRequest.read(buf);
            } catch (RuntimeException e) {
                LOGGER.warn("Pedido de área malformado de {}", player.getName().getString());
                return;
            }
            server.execute(() -> AreaOperationService.reply(player,
                    AreaResultPacket.of(AreaOperationService.requestBulk(player, request))));
        });

        ServerPlayNetworking.registerGlobalReceiver(ModIds.AREA_PLACE_REQUEST, (server, player, handler, buf, responseSender) -> {
            AreaPlaceRequest request;
            try {
                request = AreaPlaceRequest.read(buf);
            } catch (RuntimeException e) {
                LOGGER.warn("Pedido de colocação malformado de {}", player.getName().getString());
                return;
            }
            server.execute(() -> AreaOperationService.reply(player,
                    AreaResultPacket.of(AreaOperationService.place(player, request))));
        });

        ServerPlayNetworking.registerGlobalReceiver(ModIds.AREA_BREAK_REQUEST, (server, player, handler, buf, responseSender) -> {
            AreaBreakRequest request;
            try {
                request = AreaBreakRequest.read(buf);
            } catch (RuntimeException e) {
                LOGGER.warn("Pedido de quebra malformado de {}", player.getName().getString());
                return;
            }
            server.execute(() -> AreaOperationService.reply(player,
                    AreaResultPacket.of(AreaOperationService.breakBlock(player, request))));
        });
    }
}

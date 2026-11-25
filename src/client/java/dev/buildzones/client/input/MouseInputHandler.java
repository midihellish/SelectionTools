package dev.buildzones.client.input;

import dev.buildzones.client.BuildZonesClient;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

/**
 * Liga os callbacks Fabric de clique ao controller. Os callbacks também disparam
 * no servidor integrado do singleplayer, por isso a checagem de {@code isClient}.
 */
public final class MouseInputHandler {

    private MouseInputHandler() {
    }

    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient) {
                return ActionResult.PASS;
            }
            return BuildZonesClient.controller().onAttackBlock(player, pos);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient) {
                return ActionResult.PASS;
            }
            return BuildZonesClient.controller().onUseBlock(player, hand, hitResult);
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient) {
                return TypedActionResult.pass(stack);
            }
            return BuildZonesClient.controller().onUseItem(player, hand);
        });
    }
}

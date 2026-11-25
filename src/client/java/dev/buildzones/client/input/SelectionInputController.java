package dev.buildzones.client.input;

import dev.buildzones.area.AreaOperation;
import dev.buildzones.area.AreaResult;
import dev.buildzones.client.animation.AnimationController;
import dev.buildzones.client.area.AreaModeController;
import dev.buildzones.client.config.BuildZonesConfig;
import dev.buildzones.client.hud.SelectionHud;
import dev.buildzones.client.rendering.RenderStyle;
import dev.buildzones.client.selection.SelectionDraft;
import dev.buildzones.client.selection.SelectionManager;
import dev.buildzones.client.selection.SelectionRegion;
import dev.buildzones.client.selection.SelectionState;
import dev.buildzones.client.selection.SelectionStateMachine;
import dev.buildzones.client.storage.WorldProfileKey;
import dev.buildzones.network.AreaResultPacket;
import dev.buildzones.network.ReplaceBlockRequest;
import dev.buildzones.util.ModIds;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Ponto único que decide o efeito de cada entrada. A ordem de prioridade fica aqui,
 * não espalhada pelos callbacks:
 * GUI aberta > ALT+F/ALT+Z/ALT+B/ALT+C/ALT+V/CTRL+ALT+Z > prévia (RMB confirma, scroll ajusta)
 * > modo área (RMB coloca) > segundo canto > ocioso.
 */
public final class SelectionInputController {
    private final SelectionStateMachine machine;
    private final SelectionManager manager;
    private final AnimationController animations;
    private final SelectionHud hud;
    private final AreaModeController area;

    private ClientWorld lastWorld;
    private BlockPos lastAttackPos;
    private long lastAttackMs = -1L;
    private long lastReplaceMs = -1L;

    public SelectionInputController(SelectionStateMachine machine, SelectionManager manager,
                                    AnimationController animations, SelectionHud hud, AreaModeController area) {
        this.machine = machine;
        this.manager = manager;
        this.animations = animations;
        this.hud = hud;
        this.area = area;
    }

    // ---- Tick: contexto de mundo e eixo ativo ----

    public void tick(MinecraftClient client) {
        if (client.world != lastWorld) {
            lastWorld = client.world;
            onWorldChanged(client);
        }
        area.tick(client);
        if (client.world == null || client.player == null) {
            if (machine.state() != SelectionState.IDLE) {
                machine.cancel();
            }
            return;
        }
        if (client.currentScreen == null) {
            machine.updateAxis(client.player.getRotationVec(1.0F));
        }
    }

    private void onWorldChanged(MinecraftClient client) {
        machine.cancel();
        animations.clear();
        area.resetForWorld();
        if (client.world == null) {
            manager.setContext(null, null, null);
            return;
        }
        String dimension = client.world.getRegistryKey().getValue().toString();
        Optional<WorldProfileKey.Profile> profile = WorldProfileKey.resolve(client);
        manager.setContext(profile.map(WorldProfileKey.Profile::key).orElse(null),
                profile.map(WorldProfileKey.Profile::label).orElse(null), dimension);
        area.syncActive();
    }

    // ---- Mouse: ALT+LMB, RMB e uso de item ----

    /** ALT+botão esquerdo: marca um canto. Sempre consome o ataque quando ALT está segurado. */
    public ActionResult onAttackBlock(PlayerEntity player, BlockPos pos) {
        if (!ModifierKeyState.isAltDown() || player.isSpectator()) {
            return ActionResult.PASS;
        }
        // O vanilla repete o ataque com o botão segurado; o debounce evita marcar duas vezes.
        long now = Util.getMeasuringTimeMs();
        if (pos.equals(lastAttackPos) && now - lastAttackMs < BuildZonesConfig.ATTACK_DEBOUNCE_MS) {
            return ActionResult.FAIL;
        }
        lastAttackPos = pos.toImmutable();
        lastAttackMs = now;

        if (area.isActive()) {
            hud.show("buildzones.hud.area_in_mode");
            return ActionResult.FAIL;
        }
        if (!manager.hasContext()) {
            hud.show("buildzones.hud.no_profile");
            return ActionResult.FAIL;
        }
        switch (machine.state()) {
            case IDLE -> {
                machine.beginDraft(pos);
                hud.show("buildzones.hud.first_corner");
            }
            case WAITING_SECOND_CORNER -> {
                machine.setSecondCorner(pos, player.getRotationVec(1.0F));
                hud.show("buildzones.hud.preview_started");
            }
            case PREVIEW -> hud.show("buildzones.hud.finish_preview");
        }
        return ActionResult.FAIL;
    }

    /** Botão direito num bloco: confirma a prévia, bloqueia a espera, ou modo área / substituição com ALT. */
    public ActionResult onUseBlock(PlayerEntity player, Hand hand, BlockHitResult hit) {
        switch (machine.state()) {
            case PREVIEW -> {
                confirmDraft();
                return ActionResult.FAIL;
            }
            case WAITING_SECOND_CORNER -> {
                hud.show("buildzones.hud.waiting_hint");
                return ActionResult.FAIL;
            }
            default -> {
                if (area.isActive()) {
                    // No modo área o RMB é do pincel: o vanilla não coloca nem abre nada com o bloco na mão.
                    return area.suppressesVanillaUse(player) ? ActionResult.FAIL : ActionResult.PASS;
                }
                return requestReplacement(player, hand, hit);
            }
        }
    }

    /** Botão direito no ar: permite confirmar a prévia sem apontar para um bloco. */
    public TypedActionResult<ItemStack> onUseItem(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        switch (machine.state()) {
            case PREVIEW -> {
                confirmDraft();
                return TypedActionResult.fail(stack);
            }
            case WAITING_SECOND_CORNER -> {
                hud.show("buildzones.hud.waiting_hint");
                return TypedActionResult.fail(stack);
            }
            default -> {
                return TypedActionResult.pass(stack);
            }
        }
    }

    private ActionResult requestReplacement(PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!ModifierKeyState.isAltDown() || player.isSpectator()) {
            return ActionResult.PASS;
        }
        if (!(player.getStackInHand(hand).getItem() instanceof BlockItem)) {
            return ActionResult.PASS;
        }
        if (!ClientPlayNetworking.canSend(ModIds.REPLACE_REQUEST)) {
            // Sem o mod no servidor não fingimos a troca; o vanilla segue.
            hud.show("buildzones.hud.no_server_support");
            return ActionResult.PASS;
        }
        long now = Util.getMeasuringTimeMs();
        if (now - lastReplaceMs < BuildZonesConfig.REPLACE_COOLDOWN_MS) {
            return ActionResult.FAIL;
        }
        lastReplaceMs = now;

        PacketByteBuf buf = PacketByteBufs.create();
        new ReplaceBlockRequest(hit.getBlockPos(), hit.getSide(), hand).write(buf);
        ClientPlayNetworking.send(ModIds.REPLACE_REQUEST, buf);
        hud.show("buildzones.hud.replacement_sent");
        return ActionResult.FAIL;
    }

    // ---- Scroll e teclado (chamados pelos mixins) ----

    /**
     * Scroll só é consumido durante a prévia. Fora dela, a hotbar continua funcionando.
     *
     * @return true se o scroll foi consumido e não deve chegar ao vanilla
     */
    public boolean handleScroll(double vertical) {
        if (machine.state() != SelectionState.PREVIEW) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null || client.player == null || client.world == null) {
            return false;
        }
        SelectionDraft draft = machine.draft();
        int steps = draft.consumeScroll(vertical);
        if (steps != 0) {
            Vec3d look = client.player.getRotationVec(1.0F);
            SelectionStateMachine.Feedback feedback = machine.resize(
                    steps, look, client.world.getBottomY(), client.world.getTopY() - 1);
            if (feedback == SelectionStateMachine.Feedback.AT_LIMIT) {
                hud.show("buildzones.hud.at_limit");
            }
        }
        return true;
    }

    /** ALT+Z. Consome a tecla mesmo sem região, para não disparar nada do vanilla. */
    public boolean undo() {
        Optional<SelectionRegion> removed = machine.undo();
        if (removed.isPresent()) {
            SelectionRegion region = removed.get();
            animations.ghost(region.bounds().toBox(), region.color(), Util.getMeasuringTimeMs());
            hud.show("buildzones.hud.region_removed");
            area.onRegionsChanged();
        } else {
            hud.show("buildzones.hud.nothing_to_undo");
        }
        return true;
    }

    /** ALT+F. Limpa as regiões do contexto atual e cancela o rascunho. */
    public boolean clear() {
        List<SelectionRegion> removed = machine.clearAll();
        long now = Util.getMeasuringTimeMs();
        for (SelectionRegion region : removed) {
            animations.ghost(region.bounds().toBox(), region.color(), now);
        }
        hud.show("buildzones.hud.cleared", removed.size());
        area.onRegionsChanged();
        return true;
    }

    /** ESC com rascunho ativo. Sem rascunho, o ESC segue para o menu do jogo. */
    public boolean cancelDraft() {
        if (machine.cancel() != SelectionStateMachine.Feedback.CANCELLED) {
            return false;
        }
        hud.show("buildzones.hud.cancelled");
        return true;
    }

    public boolean isDrafting() {
        return machine.state() != SelectionState.IDLE;
    }

    /** ALT+B. Não liga o modo com prévia aberta: a prévia precisa ser confirmada ou cancelada antes. */
    public boolean toggleAreaMode() {
        if (!area.isActive() && machine.state() != SelectionState.IDLE) {
            hud.show("buildzones.hud.finish_preview");
            return true;
        }
        area.toggle(MinecraftClient.getInstance());
        return true;
    }

    /** ALT+C (limpar) e ALT+V (preencher). Só age dentro do modo área e no criativo. */
    public boolean areaBulk(AreaOperation operation) {
        area.requestBulk(MinecraftClient.getInstance(), operation);
        return true;
    }

    /** CTRL+ALT+Z. Desfaz a última operação em massa de blocos. */
    public boolean areaUndoBlocks() {
        area.requestUndo(MinecraftClient.getInstance());
        return true;
    }

    /** Resposta do servidor a um pedido de área. Mostra o motivo ou o resumo. */
    public void onAreaResult(AreaResultPacket packet) {
        AreaResult result = packet.result();
        switch (result) {
            case PLACED, RATE_LIMITED, CELL_OCCUPIED -> {
                // Colocação repetida é silenciosa: o fantasma sumindo já mostra o resultado,
                // e uma célula ainda não atualizada no cliente não deve virar um toast a cada repetição.
            }
            case DONE -> hud.show(result.key(), packet.changed(), packet.skipped());
            case REGION_UNKNOWN -> {
                hud.show(result.key());
                area.syncActive();
            }
            default -> hud.show(result.key());
        }
    }

    private void confirmDraft() {
        Optional<SelectionRegion> region = machine.confirm(RenderStyle.REGION_RGB);
        if (region.isPresent()) {
            animations.pulse(region.get().id(), Util.getMeasuringTimeMs());
            hud.show("buildzones.hud.region_added", formatBounds(region.get()));
            area.onRegionsChanged();
        }
    }

    private static String formatBounds(SelectionRegion region) {
        var b = region.bounds();
        return b.sizeX() + " × " + b.sizeY() + " × " + b.sizeZ();
    }
}

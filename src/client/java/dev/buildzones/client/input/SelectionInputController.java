package dev.buildzones.client.input;

import dev.buildzones.area.AreaOperation;
import dev.buildzones.area.AreaResult;
import dev.buildzones.client.animation.AnimationController;
import dev.buildzones.client.config.BuildZonesConfig;
import dev.buildzones.client.area.AreaModeController;
import dev.buildzones.client.hud.SelectionHud;
import dev.buildzones.client.radial.RadialMenuScreen;
import dev.buildzones.client.radial.RadialOption;
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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Ponto único que decide o efeito de cada entrada. A ordem de prioridade fica aqui:
 *
 * Tela aberta (menu radial) > toque em ALT (para a ação em curso, ou abre o menu)
 * > cliques da seleção (1º marca o canto A, 2º salva a região e aplica a escolha)
 * > modo área (RMB coloca) > ocioso.
 */
public final class SelectionInputController {
    private final SelectionStateMachine machine;
    private final SelectionManager manager;
    private final AnimationController animations;
    private final SelectionHud hud;
    private final AreaModeController area;

    private ClientWorld lastWorld;
    private long lastReplaceMs = -1L;
    /** Opção escolhida no menu, enquanto a seleção dela não foi salva. */
    private RadialOption pending;
    private AreaResult lastAreaRefusal;
    private long lastAreaRefusalMs;
    /** Verdadeiro entre o pressionar e o soltar da tecla de modificador, sem outra entrada no meio. */
    private boolean tapCandidate;

    public SelectionInputController(SelectionStateMachine machine, SelectionManager manager,
                                    AnimationController animations, SelectionHud hud, AreaModeController area) {
        this.machine = machine;
        this.manager = manager;
        this.animations = animations;
        this.hud = hud;
        this.area = area;
    }

    /** Opção do menu em andamento, ou nula. Usado pelo HUD. */
    public RadialOption pendingOption() {
        return pending;
    }

    // ---- Tick: contexto de mundo e canto B seguindo a mira ----

    public void tick(MinecraftClient client) {
        if (client.world != lastWorld) {
            lastWorld = client.world;
            onWorldChanged(client);
        }
        area.tick(client);
        if (client.world == null || client.player == null) {
            if (machine.state() != SelectionState.IDLE) {
                machine.cancel();
                pending = null;
            }
            return;
        }
        if (client.currentScreen == null) {
            if (machine.state() == SelectionState.WAITING_SECOND_CORNER) {
                aimAt(client);
            } else if (machine.state() == SelectionState.PREVIEW) {
                machine.updateAxis(client.player.getEyePos(), client.player.getRotationVec(1.0F));
            }
        }
    }

    /** Leva o canto B para a mira, dentro da altura do mundo. */
    private void aimAt(MinecraftClient client) {
        machine.aim(aimTarget(client), client.world.getBottomY(), client.world.getTopY() - 1);
    }

    private void onWorldChanged(MinecraftClient client) {
        machine.cancel();
        pending = null;
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

    /**
     * Célula em frente ao bloco que a mira aponta (do lado da face mirada), dentro do alcance de colocação
     * do jogador. Se não há bloco nesse alcance, usa o ponto de alcance na direção do olhar, mesmo no ar. Y fica dentro do mundo.
     */
    private static BlockPos aimTarget(MinecraftClient client) {
        double reach = client.interactionManager.getReachDistance();
        HitResult hit = client.player.raycast(reach, 0.0F, false);
        BlockPos pos;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            // A célula livre em frente à face mirada: a caixa para na superfície, sem entrar no bloco.
            pos = blockHit.getBlockPos().offset(blockHit.getSide());
        } else {
            var p = hit.getPos();
            pos = new BlockPos(MathHelper.floor(p.x), MathHelper.floor(p.y), MathHelper.floor(p.z));
        }
        int y = MathHelper.clamp(pos.getY(), client.world.getBottomY(), client.world.getTopY() - 1);
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    // ---- Tecla de modificador: toque abre o menu ou para a ação ----

    /** Pressionar o modificador. Só vira toque se nada mais for pressionado até soltar. */
    public void modifierPressed() {
        tapCandidate = true;
    }

    /** Qualquer outra tecla pressionada enquanto o modificador está para baixo: não é toque. */
    public void otherInputPressed() {
        tapCandidate = false;
    }

    /** Soltar o modificador. Se foi um toque, age: para a ação em curso ou abre o menu. */
    public void modifierReleased() {
        if (!tapCandidate) {
            return;
        }
        tapCandidate = false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (machine.state() != SelectionState.IDLE) {
            cancelDraft();
            return;
        }
        if (area.isActive()) {
            area.deactivate();
            hud.show("buildzones.hud.area_off");
            return;
        }
        openRadial(client);
    }

    private void openRadial(MinecraftClient client) {
        if (!manager.hasContext()) {
            hud.show("buildzones.hud.no_profile");
            return;
        }
        client.setScreen(new RadialMenuScreen(this::choose));
    }

    /**
     * Opção escolhida no menu. Sempre encerra o que estava em curso (pincel ligado, rascunho aberto).
     * Construir só liga o pincel, sem marcar nada. Oco, Arestas e Preencher armam uma seleção nova
     * (três cliques), que vira uma região independente com o tipo escolhido. As anteriores não mudam.
     */
    public void choose(RadialOption option) {
        if (!manager.hasContext()) {
            hud.show("buildzones.hud.no_profile");
            return;
        }
        if (!ClientPlayNetworking.canSend(ModIds.SYNC_REGION) || !ClientPlayNetworking.canSend(ModIds.AREA_OP_REQUEST)) {
            hud.show("buildzones.hud.no_server_support");
            return;
        }
        if (machine.state() != SelectionState.IDLE) {
            machine.cancel();
        }
        pending = null;
        if (option.isBrush()) {
            area.activate(MinecraftClient.getInstance());
            return;
        }
        if (area.isActive()) {
            area.deactivate();
        }
        pending = option;
        machine.arm(option.shape());
        hud.show("buildzones.hud.armed", Text.translatable(option.labelKey()).getString());
    }

    /**
     * Roda do mouse. Com a seleção ativa (armada, marcando ou na prévia), a roda nunca troca o item
     * da hotbar. Na prévia, ajusta a caixa. Com a seleção ociosa, a hotbar segue normal.
     *
     * @return true se a roda foi consumida e não deve chegar ao vanilla
     */
    public boolean handleScroll(double vertical) {
        if (machine.state() == SelectionState.IDLE) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null || client.player == null || client.world == null) {
            return false;
        }
        if (machine.state() != SelectionState.PREVIEW) {
            return true;
        }
        SelectionDraft draft = machine.draft();
        int steps = draft.consumeScroll(vertical);
        if (steps != 0) {
            var look = client.player.getRotationVec(1.0F);
            SelectionStateMachine.Feedback feedback = machine.resize(
                    steps, look, client.world.getBottomY(), client.world.getTopY() - 1);
            if (feedback == SelectionStateMachine.Feedback.AT_LIMIT) {
                hud.show("buildzones.hud.at_limit");
            }
        }
        return true;
    }

    /** ESC ou toque em ALT com a seleção armada ou em andamento. */
    public boolean cancelDraft() {
        if (machine.cancel() != SelectionStateMachine.Feedback.CANCELLED) {
            return false;
        }
        pending = null;
        hud.show("buildzones.hud.cancelled");
        return true;
    }

    public boolean isDrafting() {
        return machine.state() != SelectionState.IDLE;
    }

    // ---- Mouse: cliques da seleção ----

    /**
     * Clique com a seleção armada ou em andamento. Chamado por {@link dev.buildzones.client.mixin.MouseButtonMixin}
     * antes do vanilla, então o clique não ataca nem usa item. Só botões esquerdo e direito contam.
     * O primeiro clique marca o canto A; o segundo salva a região e aplica a escolha.
     *
     * @return true se o clique foi consumido
     */
    public boolean handleMouseButton(long window, int button, int action) {
        if (action != GLFW.GLFW_PRESS) {
            return false;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return false;
        }
        otherInputPressed();
        if (machine.state() == SelectionState.IDLE) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (window != client.getWindow().getHandle() || client.currentScreen != null
                || client.player == null || client.world == null) {
            return false;
        }
        if (machine.state() == SelectionState.ARMED) {
            machine.beginDraft(aimTarget(client));
            hud.show("buildzones.hud.first_corner");
            return true;
        }
        if (machine.state() == SelectionState.WAITING_SECOND_CORNER) {
            aimAt(client);
            machine.lockSecondCorner();
            hud.show("buildzones.hud.preview_started");
            return true;
        }
        pending = null;
        Optional<SelectionRegion> region = machine.confirm(RenderStyle.REGION_RGB);
        if (region.isEmpty()) {
            hud.show("buildzones.hud.no_profile");
            return true;
        }
        animations.pulse(region.get().id(), Util.getMeasuringTimeMs());
        hud.show("buildzones.hud.region_added", formatBounds(region.get()));
        area.onRegionsChanged();
        return true;
    }

    /** Botão direito num bloco: sem seleção, modo área ou substituição com modificador. */
    public ActionResult onUseBlock(PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (machine.state() != SelectionState.IDLE) {
            // Defensivo: o clique já foi consumido em handleMouseButton.
            return ActionResult.FAIL;
        }
        if (area.isActive()) {
            // No modo área o RMB é do pincel: o vanilla não coloca nem abre nada com o bloco na mão.
            return area.suppressesVanillaUse(player) ? ActionResult.FAIL : ActionResult.PASS;
        }
        return requestReplacement(player, hand, hit);
    }

    private ActionResult requestReplacement(PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!ModifierKeyState.isHeld() || player.isSpectator()) {
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

    // ---- Atalhos com modificador ----

    /** Modificador+Z. Consome a tecla mesmo sem região, para não disparar nada do vanilla. */
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

    /** Modificador+F. Limpa as regiões do contexto atual e cancela o rascunho. */
    public boolean clear() {
        pending = null;
        List<SelectionRegion> removed = machine.clearAll();
        long now = Util.getMeasuringTimeMs();
        for (SelectionRegion region : removed) {
            animations.ghost(region.bounds().toBox(), region.color(), now);
        }
        hud.show("buildzones.hud.cleared", removed.size());
        area.onRegionsChanged();
        return true;
    }

    /** Modificador+C (limpar) e Modificador+V (preencher). Só age no criativo. */
    public boolean areaBulk(AreaOperation operation) {
        area.requestBulk(MinecraftClient.getInstance(), operation);
        return true;
    }

    /** Modificador+Ctrl+Z. Desfaz a última operação em massa de blocos. */
    public boolean areaUndoBlocks() {
        area.requestUndo(MinecraftClient.getInstance());
        return true;
    }

    /** Resposta do servidor a um pedido de área. Mostra o motivo ou o resumo. */
    public void onAreaResult(AreaResultPacket packet) {
        AreaResult result = packet.result();
        switch (result) {
            case PLACED, BROKEN, RATE_LIMITED, CELL_OCCUPIED -> {
                // Colocação repetida é silenciosa: o fantasma sumindo já mostra o resultado,
                // e uma célula ainda não atualizada no cliente não deve virar um toast a cada repetição.
            }
            case DONE -> hud.show(result.key(), packet.changed(), packet.skipped());
            case REGION_UNKNOWN -> {
                hud.show(result.key());
                area.syncActive();
            }
            default -> {
                // Segurando o botão, a mesma recusa chega a cada tick: mostra uma vez por janela.
                long now = Util.getMeasuringTimeMs();
                if (result != lastAreaRefusal || now - lastAreaRefusalMs > 1500L) {
                    hud.show(result.key());
                    lastAreaRefusal = result;
                    lastAreaRefusalMs = now;
                }
            }
        }
    }

    private static String formatBounds(SelectionRegion region) {
        var b = region.bounds();
        return b.sizeX() + " × " + b.sizeY() + " × " + b.sizeZ();
    }
}

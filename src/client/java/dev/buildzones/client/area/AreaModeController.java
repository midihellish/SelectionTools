package dev.buildzones.client.area;

import dev.buildzones.area.AreaBox;
import dev.buildzones.area.AreaLimits;
import dev.buildzones.area.AreaOperation;
import dev.buildzones.client.config.BuildZonesConfig;
import dev.buildzones.client.hud.SelectionHud;
import dev.buildzones.client.selection.SelectionManager;
import dev.buildzones.client.selection.SelectionRegion;
import dev.buildzones.network.AreaOpRequest;
import dev.buildzones.network.AreaPlaceRequest;
import dev.buildzones.network.SyncRegionPacket;
import dev.buildzones.replacement.ReplaceBlockValidator;
import dev.buildzones.replacement.ReplacementPolicy;
import dev.buildzones.util.ModIds;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

/**
 * Modo área (ALT+B). Alvo = bloco da mão no momento da ativação. Dentro do modo:
 * RMB coloca no fantasma mirado (sobrevivência e criativo), ALT+C e ALT+V fazem operação
 * em massa só no criativo, com confirmação em duas pressões.
 *
 * O cliente nunca altera blocos: ele só lê o mundo para desenhar e para contar,
 * e manda pedidos. A região que vale é a última confirmada, a mesma que o servidor guarda.
 */
public final class AreaModeController {
    private static final UUID NO_REGION = new UUID(0L, 0L);

    private record Pending(AreaOperation operation, UUID regionId, long expiresAt) {
    }

    private final SelectionManager manager;
    private final SelectionHud hud;

    private boolean active;
    private BlockState target;
    private Pending pending;
    private long lastPlaceMs = -1L;
    private List<BlockPos> missing = List.of();
    private List<BlockPos> wrong = List.of();
    private Set<BlockPos> missingSet = Set.of();

    public AreaModeController(SelectionManager manager, SelectionHud hud) {
        this.manager = manager;
        this.hud = hud;
    }

    public boolean isActive() {
        return active;
    }

    /** Fantasmas de colocação, prontos para o renderer. Vazio fora do modo. */
    public List<BlockPos> missingCells() {
        return missing;
    }

    /** Contornos de remoção, para blocos que não pertencem ao alvo. */
    public List<BlockPos> wrongCells() {
        return wrong;
    }

    // ---- Ativação ----

    /** ALT+B. Liga pelo bloco da mão ou desliga. Sem o mod no servidor, avisa e não liga. */
    public void toggle(MinecraftClient client) {
        if (!ClientPlayNetworking.canSend(ModIds.SYNC_REGION)) {
            hud.show("buildzones.hud.no_server_support");
            return;
        }
        if (active) {
            deactivate();
            hud.show("buildzones.hud.area_off");
            return;
        }
        if (manager.regions().isEmpty()) {
            hud.show("buildzones.hud.area_need_region");
            return;
        }
        if (client.player == null || !(client.player.getMainHandStack().getItem() instanceof BlockItem item)) {
            hud.show("buildzones.hud.area_need_block");
            return;
        }
        target = item.getBlock().getDefaultState();
        active = true;
        syncActive();
        hud.show("buildzones.hud.area_on", target.getBlock().getName());
    }

    /** Sai do modo sem mensagem. Usado ao trocar de mundo ou quando não há mais região. */
    public void deactivate() {
        active = false;
        target = null;
        pending = null;
        missing = List.of();
        wrong = List.of();
        missingSet = Set.of();
        syncActive();
    }

    /** Troca de mundo: o modo não atravessa dimensões, mas a região nova ainda precisa ser sincronizada. */
    public void resetForWorld() {
        active = false;
        target = null;
        pending = null;
        missing = List.of();
        wrong = List.of();
        missingSet = Set.of();
    }

    /** Depois de confirmar, desfazer ou limpar: a região ativa mudou, ou não há mais nenhuma. */
    public void onRegionsChanged() {
        if (active && manager.regions().isEmpty()) {
            deactivate();
            return;
        }
        syncActive();
    }

    // ---- Sincronia da região ativa ----

    /** Manda ao servidor a última região confirmada. Sem o mod, não faz nada. */
    public void syncActive() {
        if (!ClientPlayNetworking.canSend(ModIds.SYNC_REGION)) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        Optional<SelectionRegion> region = activeRegion();
        SyncRegionPacket packet = region.map(r -> new SyncRegionPacket(true, r.id(), r.dimension(),
                        r.anchor(), r.opposite(), active && target != null,
                        active && target != null ? Block.getRawIdFromState(target) : 0))
                .orElse(SyncRegionPacket.none());
        packet.write(buf);
        ClientPlayNetworking.send(ModIds.SYNC_REGION, buf);
    }

    /** Última região confirmada: é a que ALT+B, ALT+C e ALT+V usam. */
    public Optional<SelectionRegion> activeRegion() {
        List<SelectionRegion> regions = manager.regions();
        return regions.isEmpty() ? Optional.empty() : Optional.of(regions.get(regions.size() - 1));
    }

    // ---- Operação em massa (criativo) ----

    /**
     * ALT+C ou ALT+V. Primeira pressão mostra a contagem; a segunda, dentro da janela,
     * confirma e envia. Trocar de operação recomeça a contagem.
     */
    public void requestBulk(MinecraftClient client, AreaOperation operation) {
        if (!active) {
            hud.show("buildzones.hud.area_need_mode");
            return;
        }
        if (client.player == null || client.world == null) {
            return;
        }
        if (!client.player.getAbilities().creativeMode) {
            hud.show("buildzones.hud.area_creative_only");
            return;
        }
        Optional<SelectionRegion> region = activeRegion();
        if (region.isEmpty()) {
            hud.show("buildzones.hud.area_need_region");
            return;
        }
        if (!ClientPlayNetworking.canSend(ModIds.AREA_OP_REQUEST)) {
            hud.show("buildzones.hud.no_server_support");
            return;
        }
        AreaBox box = AreaBox.between(region.get().anchor(), region.get().opposite());
        if (box.volume() > AreaLimits.MAX_OP_VOLUME) {
            hud.show("buildzones.hud.area_too_large", formatCount(AreaLimits.MAX_OP_VOLUME));
            return;
        }
        if (operation == AreaOperation.FILL && !(client.player.getMainHandStack().getItem() instanceof BlockItem)) {
            hud.show("buildzones.hud.area_need_held_block");
            return;
        }

        long now = Util.getMeasuringTimeMs();
        if (pending != null && pending.operation() == operation
                && pending.regionId().equals(region.get().id()) && now < pending.expiresAt()) {
            pending = null;
            send(new AreaOpRequest(region.get().id(), operation, Hand.MAIN_HAND));
            hud.show("buildzones.hud.area_sent");
            return;
        }

        int count = countCells(operation, box, client.world);
        if (count == 0) {
            pending = null;
            hud.show("buildzones.hud.area_nothing");
            return;
        }
        pending = new Pending(operation, region.get().id(), now + BuildZonesConfig.AREA_CONFIRM_WINDOW_MS);
        if (operation == AreaOperation.CLEAR) {
            hud.show("buildzones.hud.area_confirm_clear", formatCount(count));
        } else {
            hud.show("buildzones.hud.area_confirm_fill", formatCount(count),
                    client.player.getMainHandStack().getName());
        }
    }

    /** CTRL+ALT+Z. Desfaz a última operação em massa. Só criativo. */
    public void requestUndo(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        if (!client.player.getAbilities().creativeMode) {
            hud.show("buildzones.hud.area_creative_only");
            return;
        }
        if (!ClientPlayNetworking.canSend(ModIds.AREA_OP_REQUEST)) {
            hud.show("buildzones.hud.no_server_support");
            return;
        }
        send(new AreaOpRequest(NO_REGION, AreaOperation.UNDO, Hand.MAIN_HAND));
        hud.show("buildzones.hud.area_undo_sent");
    }

    /** Conta células que a operação de fato alteraria, com a mesma regra do servidor. */
    private static int countCells(AreaOperation operation, AreaBox box, ClientWorld world) {
        int count = 0;
        Iterator<BlockPos> cells = box.cells();
        while (cells.hasNext()) {
            BlockPos pos = cells.next();
            BlockState state = world.getBlockState(pos);
            boolean accepted = operation == AreaOperation.CLEAR
                    ? ReplacementPolicy.acceptsClear(world, pos, state)
                    : ReplacementPolicy.acceptsFill(world, pos, state);
            if (accepted) {
                count++;
            }
        }
        return count;
    }

    private void send(AreaOpRequest request) {
        PacketByteBuf buf = PacketByteBufs.create();
        request.write(buf);
        ClientPlayNetworking.send(ModIds.AREA_OP_REQUEST, buf);
    }

    // ---- Tick: fantasmas e colocação repetida ----

    /** Recalcula os fantasmas e, com o botão direito segurado, coloca a cada intervalo. */
    public void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        PlayerEntity player = client.player;
        if (!active || world == null || player == null || client.currentScreen != null || target == null) {
            missing = List.of();
            wrong = List.of();
            missingSet = Set.of();
            return;
        }
        Optional<SelectionRegion> region = activeRegion();
        if (region.isEmpty() || !region.get().dimension().equals(world.getRegistryKey().getValue().toString())) {
            missing = List.of();
            wrong = List.of();
            missingSet = Set.of();
            return;
        }
        AreaBox box = AreaBox.between(region.get().anchor(), region.get().opposite());
        SurvivalBrushPlanner.Plan plan = SurvivalBrushPlanner.plan(box, player.getBlockPos(),
                BuildZonesConfig.AREA_GHOST_RANGE, BuildZonesConfig.AREA_GHOST_LAYER,
                BuildZonesConfig.AREA_GHOST_MAX, pos -> classify(world, pos));
        missing = plan.missing();
        wrong = plan.wrong();
        missingSet = new HashSet<>(missing);

        // Só coloca se a mão tem exatamente o bloco do alvo; senão o servidor recusaria a cada repetição.
        if (!client.options.useKey.isPressed() || !player.getMainHandStack().isOf(target.getBlock().asItem())) {
            return;
        }
        long now = Util.getMeasuringTimeMs();
        if (lastPlaceMs >= 0 && now - lastPlaceMs < BuildZonesConfig.AREA_REPEAT_MS) {
            return;
        }
        Optional<BlockPos> cell = SurvivalBrushPlanner.firstOnRay(missingSet,
                pos -> !world.getBlockState(pos).isAir(),
                player.getEyePos(), player.getRotationVec(1.0F), ReplaceBlockValidator.REACH);
        if (cell.isPresent()) {
            lastPlaceMs = now;
            PacketByteBuf buf = PacketByteBufs.create();
            new AreaPlaceRequest(region.get().id(), cell.get(), Hand.MAIN_HAND).write(buf);
            ClientPlayNetworking.send(ModIds.AREA_PLACE_REQUEST, buf);
        }
    }

    /** Classifica uma célula em relação ao alvo. Chunk descarregado conta como já correto, para não gerar fantasma. */
    private SurvivalBrushPlanner.Cell classify(ClientWorld world, BlockPos pos) {
        if (!world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return SurvivalBrushPlanner.Cell.MATCHES;
        }
        BlockState state = world.getBlockState(pos);
        if (state.equals(target)) {
            return SurvivalBrushPlanner.Cell.MATCHES;
        }
        return state.isAir() || state.isReplaceable()
                ? SurvivalBrushPlanner.Cell.EMPTY
                : SurvivalBrushPlanner.Cell.WRONG;
    }

    /** Verdadeiro se o botão direito deve ser engolido, para o vanilla não colocar o bloco por conta própria. */
    public boolean suppressesVanillaUse(PlayerEntity player) {
        return active && player.getMainHandStack().getItem() instanceof BlockItem;
    }

    private static String formatCount(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', '.');
    }
}

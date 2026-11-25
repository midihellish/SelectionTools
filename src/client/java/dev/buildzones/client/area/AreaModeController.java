package dev.buildzones.client.area;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import dev.buildzones.area.AreaBox;
import dev.buildzones.area.AreaLimits;
import dev.buildzones.area.AreaOperation;
import dev.buildzones.client.config.BuildZonesConfig;
import dev.buildzones.client.hud.SelectionHud;
import dev.buildzones.client.input.ModifierKeyState;
import dev.buildzones.client.radial.RadialOption;
import dev.buildzones.client.selection.SelectionManager;
import dev.buildzones.client.selection.SelectionRegion;
import dev.buildzones.network.AreaBreakRequest;
import dev.buildzones.network.AreaOpRequest;
import dev.buildzones.network.AreaPlaceRequest;
import dev.buildzones.network.SyncRegionPacket;
import dev.buildzones.replacement.ReplaceBlockValidator;
import dev.buildzones.replacement.ReplacementPolicy;
import dev.buildzones.util.ModIds;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Modo área (pincel). Ligado por Construir no menu radial, sobre as áreas já marcadas. Alvo = bloco
 * da mão, e a forma de cada célula vem do tipo da área dona dela. Dentro do modo:
 * botão direito coloca na célula livre mais próxima (a mira dá um desconto) e o esquerdo quebra o bloco mirado, os dois repetindo ao segurar, ALT+C e ALT+V fazem operação
 * em massa só no criativo, com confirmação em duas pressões. O toque em ALT desliga.
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
    private long lastActionMs = -1L;
    private BlockPos lastCell;
    /** Células de colocação já pedidas, à espera da atualização do servidor: não são escolhidas de novo. */
    private final Map<BlockPos, Long> pendingCells = new HashMap<>();
    /** Próxima célula a receber bloco (no máximo uma). */
    private List<BlockPos> missing = List.of();
    /** Bloco que o esquerdo quebraria agora (no máximo um). */
    private List<BlockPos> wrong = List.of();

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

    /** Bloco-alvo do modo ligado, ou nulo fora dele. */
    public BlockState target() {
        return target;
    }

    // ---- Ativação ----

    /**
     * Liga o pincel em todas as áreas da dimensão atual, cada uma com o próprio tipo.
     * O alvo é o bloco da mão principal, e acompanha a mão enquanto o pincel estiver ligado.
     * Sem o mod no servidor, avisa e não liga. Não cria área alguma.
     */
    public void activate(MinecraftClient client) {
        if (!ClientPlayNetworking.canSend(ModIds.SYNC_REGION)) {
            hud.show("buildzones.hud.no_server_support");
            return;
        }
        if (manager.regions().isEmpty()) {
            hud.show("buildzones.hud.area_need_region");
            return;
        }
        target = heldTarget(client);
        active = true;
        syncActive();
        String targetName = target == null
                ? Text.translatable("buildzones.hud.area_no_block").getString()
                : target.getBlock().getName().getString();
        hud.show("buildzones.hud.area_on", Text.translatable(RadialOption.BUILD.labelKey()).getString(),
                targetName, ModifierKeyState.name());
    }

    /** Bloco da mão principal como alvo, ou nulo se a mão não tem um bloco. */
    private static BlockState heldTarget(MinecraftClient client) {
        if (client.player != null && client.player.getMainHandStack().getItem() instanceof BlockItem item) {
            return item.getBlock().getDefaultState();
        }
        return null;
    }

    /** Sai do modo sem mensagem. Usado ao trocar de mundo, ao escolher outra ação ou quando não há mais região. */
    public void deactivate() {
        active = false;
        target = null;
        pending = null;
        pendingCells.clear();
        missing = List.of();
        wrong = List.of();
        syncActive();
    }

    /** Troca de mundo: o modo não atravessa dimensões, mas a região nova ainda precisa ser sincronizada. */
    public void resetForWorld() {
        active = false;
        target = null;
        pending = null;
        pendingCells.clear();
        missing = List.of();
        wrong = List.of();
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
        List<SyncRegionPacket.Entry> entries = new ArrayList<>();
        for (SelectionRegion region : syncedRegions()) {
            entries.add(new SyncRegionPacket.Entry(region.id(), region.dimension(), region.anchor(), region.opposite()));
        }
        boolean withTarget = active && target != null;
        SyncRegionPacket packet = new SyncRegionPacket(entries, withTarget,
                withTarget ? Block.getRawIdFromState(target) : 0);
        PacketByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        ClientPlayNetworking.send(ModIds.SYNC_REGION, buf);
    }

    /** Áreas da dimensão atual que o servidor deve conhecer: as mais recentes, até o limite do pacote. */
    private List<SelectionRegion> syncedRegions() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return List.of();
        }
        String dimension = world.getRegistryKey().getValue().toString();
        List<SelectionRegion> regions = manager.regions();
        List<SelectionRegion> out = new ArrayList<>();
        for (int i = regions.size() - 1; i >= 0 && out.size() < AreaLimits.MAX_SYNC_REGIONS; i--) {
            if (regions.get(i).dimension().equals(dimension)) {
                out.add(regions.get(i));
            }
        }
        return out;
    }

    /** Última região confirmada: é a que ALT+C e ALT+V usam. */
    public Optional<SelectionRegion> activeRegion() {
        List<SelectionRegion> regions = manager.regions();
        return regions.isEmpty() ? Optional.empty() : Optional.of(regions.get(regions.size() - 1));
    }

    // ---- Operação em massa (criativo) ----

    /**
     * ALT+C ou ALT+V, ou a opção Quebrar do menu. Primeira pressão mostra a contagem; a segunda,
     * dentro da janela, confirma e envia. Trocar de operação recomeça a contagem.
     * Preencher em massa só vale em área de volume inteiro: o servidor não conhece a forma.
     */
    public void requestBulk(MinecraftClient client, AreaOperation operation) {
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
        if (operation == AreaOperation.FILL && region.get().shape() != AreaShape.FILL) {
            hud.show("buildzones.hud.area_fill_full_only");
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
            hud.show("buildzones.hud.area_confirm_clear", formatCount(count), ModifierKeyState.name());
        } else {
            hud.show("buildzones.hud.area_confirm_fill", formatCount(count),
                    client.player.getMainHandStack().getName().getString(), ModifierKeyState.name());
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

    // ---- Tick: escolha da célula, fantasma e repetição ----

    /** Célula escolhida para colocar e a área dona dela. */
    private record Pick(BlockPos pos, SelectionRegion region) {
    }

    /**
     * Botão direito coloca onde mais perto do jogador houver uma célula livre de uma área; a mira só dá
     * um desconto na distância (ver {@link PlacementScore}). Botão esquerdo quebra o bloco mirado, como
     * no vanilla, dentro de uma área e no tipo dela. Segurar repete a cada tick. Sem camadas, sem ordem.
     * O fantasma azul é a próxima célula a receber bloco; o contorno vermelho, o bloco que o esquerdo quebraria.
     */
    public void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        PlayerEntity player = client.player;
        if (!active || world == null || player == null || client.currentScreen != null) {
            clearGhosts();
            return;
        }
        // O alvo acompanha a mão: trocar de bloco troca o alvo e avisa o servidor.
        BlockState held = heldTarget(client);
        if (!sameBlock(held, target)) {
            target = held;
            syncActive();
        }

        long now = Util.getMeasuringTimeMs();
        pendingCells.values().removeIf(sentAt -> now - sentAt >= BuildZonesConfig.AREA_PENDING_MS);

        Optional<Pick> place = target == null ? Optional.empty() : pickPlacement(world, player, now);
        BlockPos hitPos = null;
        Optional<SelectionRegion> breakIn = Optional.empty();
        HitResult hit = player.raycast(ReplaceBlockValidator.REACH, 1.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            hitPos = ((BlockHitResult) hit).getBlockPos();
            breakIn = world.getBlockState(hitPos).isAir() ? Optional.empty() : regionAt(hitPos);
        }

        missing = place.map(pick -> List.of(pick.pos())).orElse(List.of());
        wrong = breakIn.isPresent() ? List.of(hitPos) : List.of();

        if (now - lastActionMs < BuildZonesConfig.AREA_REPEAT_MS) {
            return;
        }
        if (client.options.useKey.isPressed() && place.isPresent()
                && player.getMainHandStack().isOf(target.getBlock().asItem())) {
            lastActionMs = now;
            pendingCells.put(place.get().pos().toImmutable(), now);
            PacketByteBuf buf = PacketByteBufs.create();
            new AreaPlaceRequest(place.get().region().id(), place.get().pos(), Hand.MAIN_HAND).write(buf);
            ClientPlayNetworking.send(ModIds.AREA_PLACE_REQUEST, buf);
        } else if (client.options.attackKey.isPressed() && breakIn.isPresent()
                && ClientPlayNetworking.canSend(ModIds.AREA_BREAK_REQUEST) && !isRepeat(hitPos, now)) {
            lastActionMs = now;
            lastCell = hitPos.toImmutable();
            PacketByteBuf buf = PacketByteBufs.create();
            new AreaBreakRequest(breakIn.get().id(), hitPos).write(buf);
            ClientPlayNetworking.send(ModIds.AREA_BREAK_REQUEST, buf);
        }
    }

    /**
     * Célula livre mais bem pontuada ao alcance: dentro de uma área e do tipo dela, vazia ou substituível,
     * fora do corpo do jogador e que o servidor aceitaria colocar. Células pedidas há pouco ficam de fora,
     * para o próximo pedido já ir à seguinte sem esperar a atualização do servidor.
     */
    private Optional<Pick> pickPlacement(ClientWorld world, PlayerEntity player, long now) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        double reach = ReplaceBlockValidator.REACH;
        int r = (int) Math.ceil(reach);
        BlockPos center = BlockPos.ofFloored(eye);
        int minX = center.getX() - r;
        int maxX = center.getX() + r;
        int minY = center.getY() - r;
        int maxY = center.getY() + r;
        int minZ = center.getZ() - r;
        int maxZ = center.getZ() + r;

        List<SelectionRegion> near = new ArrayList<>();
        for (SelectionRegion region : syncedRegions()) {
            AreaBox box = AreaBox.between(region.anchor(), region.opposite());
            if (box.maxX() >= minX && box.minX() <= maxX && box.maxY() >= minY && box.minY() <= maxY
                    && box.maxZ() >= minZ && box.minZ() <= maxZ) {
                near.add(region);
            }
        }
        if (near.isEmpty()) {
            return Optional.empty();
        }

        Box body = player.getBoundingBox();
        double reachSq = reach * reach;
        List<ScoredCell> candidates = new ArrayList<>();
        for (SelectionRegion region : near) {
            AreaBox box = AreaBox.between(region.anchor(), region.opposite());
            for (int y = Math.max(minY, box.minY()); y <= Math.min(maxY, box.maxY()); y++) {
                for (int z = Math.max(minZ, box.minZ()); z <= Math.min(maxZ, box.maxZ()); z++) {
                    for (int x = Math.max(minX, box.minX()); x <= Math.min(maxX, box.maxX()); x++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!region.shape().contains(box, pos)
                                || Vec3d.ofCenter(pos).squaredDistanceTo(eye) > reachSq
                                || pendingCells.containsKey(pos)
                                || !world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                            continue;
                        }
                        BlockState state = world.getBlockState(pos);
                        if ((!state.isAir() && !state.isReplaceable()) || body.intersects(new Box(pos))) {
                            continue;
                        }
                        candidates.add(new ScoredCell(pos, region,
                                PlacementScore.of(eye, look, pos, BuildZonesConfig.AREA_AIM_BONUS)));
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(ScoredCell::score));
        // Só as melhores passam pela checagem cara (colisão com entidades, suporte do bloco).
        for (int i = 0; i < Math.min(candidates.size(), 8); i++) {
            ScoredCell cell = candidates.get(i);
            if (target.canPlaceAt(world, cell.pos()) && world.canPlace(target, cell.pos(), ShapeContext.of(player))) {
                return Optional.of(new Pick(cell.pos(), cell.region()));
            }
        }
        return Optional.empty();
    }

    private record ScoredCell(BlockPos pos, SelectionRegion region, double score) {
    }

    /** Mesma célula pedida há pouco: o servidor ainda não devolveu a atualização do bloco. */
    private boolean isRepeat(BlockPos cell, long now) {
        return cell.equals(lastCell) && now - lastActionMs < BuildZonesConfig.AREA_SAME_CELL_MS;
    }

    /** Área da dimensão atual que contém a célula, no tipo dela (volume, casca ou arestas). Vazio se nenhuma. */
    private Optional<SelectionRegion> regionAt(BlockPos pos) {
        for (SelectionRegion region : syncedRegions()) {
            AreaBox box = AreaBox.between(region.anchor(), region.opposite());
            if (region.shape().contains(box, pos)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    private void clearGhosts() {
        missing = List.of();
        wrong = List.of();
    }

    private static boolean sameBlock(BlockState a, BlockState b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getBlock() == b.getBlock();
    }

    /** Verdadeiro se o botão direito deve ser engolido, para o vanilla não colocar o bloco por conta própria. */
    public boolean suppressesVanillaUse(PlayerEntity player) {
        return active && player.getMainHandStack().getItem() instanceof BlockItem;
    }

    private static String formatCount(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', '.');
    }
}

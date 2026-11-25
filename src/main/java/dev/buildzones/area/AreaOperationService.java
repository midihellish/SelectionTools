package dev.buildzones.area;

import dev.buildzones.network.AreaOpRequest;
import dev.buildzones.network.AreaPlaceRequest;
import dev.buildzones.network.AreaResultPacket;
import dev.buildzones.network.SyncRegionPacket;
import dev.buildzones.replacement.PlacementContextFactory;
import dev.buildzones.replacement.ReplaceBlockService;
import dev.buildzones.replacement.ReplaceBlockValidator;
import dev.buildzones.replacement.ReplacementPolicy;
import dev.buildzones.util.ModIds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servidor da área: região ativa por jogador, operações em massa em fatias de tick,
 * desfazer e colocação do pincel. Toda decisão passa pelo {@link AreaOperationValidator}
 * e pela {@link ReplacementPolicy}, nunca por regras próprias.
 *
 * Estado mutável só é tocado na thread do servidor (os pacotes são agendados para lá).
 */
public final class AreaOperationService {
    private static final Logger LOGGER = LoggerFactory.getLogger("BuildZones/Area");

    private static final ServerRegionRegistry REGIONS = new ServerRegionRegistry();
    /** Um trabalho ativo por jogador. */
    private static final Map<UUID, Job> JOBS = new HashMap<>();
    private static final Map<UUID, AreaUndoBuffer<CellSnapshot>> UNDO = new HashMap<>();
    private static final Map<UUID, Long> LAST_PLACE = new HashMap<>();

    /** Célula antes da alteração. Serve para desfazer. */
    public record CellSnapshot(BlockPos pos, BlockState before) {
    }

    /** Trabalho em fatias. {@code journal} guarda só as células que realmente mudaram. */
    private static final class Job {
        final UUID playerId;
        final ServerPlayerEntity player;
        final ServerWorld world;
        final AreaOperation operation;
        final boolean undo;
        final List<CellSnapshot> journal = new ArrayList<>();
        AreaJobRunner<?> runner;
        int changed;
        int skipped;

        Job(ServerPlayerEntity player, AreaOperation operation) {
            this.playerId = player.getUuid();
            this.player = player;
            this.world = player.getServerWorld();
            this.operation = operation;
            this.undo = operation == AreaOperation.UNDO;
        }
    }

    private AreaOperationService() {
    }

    /** Ligações de ciclo de vida. Chamado uma vez na inicialização comum. */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AreaOperationService::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> resetAll());
    }

    // ---- Região ativa ----

    /** Atualiza ou remove a região ativa do jogador, depois de validar os campos. */
    public static void sync(ServerPlayerEntity player, SyncRegionPacket packet) {
        UUID uuid = player.getUuid();
        if (!packet.present()) {
            REGIONS.remove(uuid);
            return;
        }
        BlockState target = null;
        if (packet.hasTarget()) {
            BlockState resolved = Block.getStateFromRawId(packet.targetRawId());
            if (resolved != null && !resolved.isAir()) {
                target = resolved;
            }
        }
        boolean accepted = REGIONS.put(uuid, packet.id(), packet.dimension(),
                AreaBox.between(packet.anchor(), packet.opposite()), target);
        if (!accepted) {
            REGIONS.remove(uuid);
            LOGGER.debug("Região recusada de {}: dimensão acima do limite", player.getName().getString());
        }
    }

    // ---- Operação em massa (criativo) ----

    /** Enfileira CLEAR, FILL ou UNDO. O resultado imediato vai de volta ao cliente. */
    public static AreaResult requestBulk(ServerPlayerEntity player, AreaOpRequest request) {
        if (request.operation() == AreaOperation.UNDO) {
            return requestUndo(player);
        }
        UUID uuid = player.getUuid();
        Optional<ServerRegionRegistry.ActiveRegion> region = REGIONS.get(uuid);
        boolean known = region.isPresent() && region.get().id().equals(request.regionId());
        boolean dimensionMatches = known && region.get().dimension().equals(dimensionOf(player.getWorld()));
        long volume = known ? region.get().box().volume() : 0L;

        ItemStack stack = player.getStackInHand(request.hand());
        boolean holdingBlock = stack.getItem() instanceof BlockItem;
        AreaOperationValidator.BulkFacts facts = new AreaOperationValidator.BulkFacts(
                isCreative(player), JOBS.containsKey(uuid), known, dimensionMatches, volume,
                request.operation() == AreaOperation.FILL, holdingBlock);
        Optional<AreaResult> refusal = AreaOperationValidator.checkBulk(facts);
        if (refusal.isPresent()) {
            return refusal.get();
        }

        Job job = new Job(player, request.operation());
        Block block = request.operation() == AreaOperation.FILL ? ((BlockItem) stack.getItem()).getBlock() : null;
        ItemStack heldCopy = stack.copy();
        Hand hand = request.hand();
        AreaBox box = region.get().box();
        job.runner = new AreaJobRunner<>(box.cells(), pos -> bulkStep(job, pos, block, heldCopy, hand));
        JOBS.put(uuid, job);
        return AreaResult.QUEUED;
    }

    /** Uma célula da operação em massa. Pula chunks descarregados e células protegidas, e conta. */
    private static void bulkStep(Job job, BlockPos pos, Block block, ItemStack heldCopy, Hand hand) {
        ServerWorld world = job.world;
        if (!world.isInBuildLimit(pos)) {
            job.skipped++;
            return;
        }
        if (!isChunkLoaded(world, pos)) {
            job.skipped++;
            return;
        }
        BlockState before = world.getBlockState(pos);
        BlockState after;
        if (job.operation == AreaOperation.CLEAR) {
            if (before.isAir()) {
                return;
            }
            if (!ReplacementPolicy.acceptsClear(world, pos, before)) {
                job.skipped++;
                return;
            }
            after = Blocks.AIR.getDefaultState();
        } else {
            // FILL só ocupa ar ou substituível. Célula sólida ocupada não conta como pulada.
            if (!ReplacementPolicy.acceptsFill(world, pos, before)) {
                return;
            }
            ItemPlacementContext context = PlacementContextFactory.create(
                    world, job.player, hand, heldCopy, pos, Direction.UP);
            after = block.getPlacementState(context);
            if (after == null || !after.canPlaceAt(world, pos)
                    || !world.canPlace(after, pos, ShapeContext.of(job.player))) {
                job.skipped++;
                return;
            }
        }
        if (after.equals(before)) {
            return;
        }
        if (!world.canPlayerModifyAt(job.player, pos)) {
            job.skipped++;
            return;
        }
        // NOTIFY_LISTENERS: só o cliente recebe a mudança. Sem vizinhança, sem drops.
        if (world.setBlockState(pos, after, Block.NOTIFY_LISTENERS)) {
            job.journal.add(new CellSnapshot(pos.toImmutable(), before));
            job.changed++;
        } else {
            job.skipped++;
        }
    }

    private static AreaResult requestUndo(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        AreaUndoBuffer<CellSnapshot> buffer = UNDO.get(uuid);
        boolean hasBatch = buffer != null && !buffer.isEmpty();
        Optional<AreaResult> refusal = AreaOperationValidator.checkUndo(
                isCreative(player), JOBS.containsKey(uuid), hasBatch);
        if (refusal.isPresent()) {
            return refusal.get();
        }
        List<CellSnapshot> batch = buffer.pop().orElseThrow();
        Job job = new Job(player, AreaOperation.UNDO);
        Iterator<CellSnapshot> cells = batch.iterator();
        job.runner = new AreaJobRunner<>(cells, snapshot -> undoStep(job, snapshot));
        JOBS.put(uuid, job);
        return AreaResult.QUEUED;
    }

    private static void undoStep(Job job, CellSnapshot snapshot) {
        ServerWorld world = job.world;
        if (!isChunkLoaded(world, snapshot.pos())) {
            job.skipped++;
            return;
        }
        if (world.setBlockState(snapshot.pos(), snapshot.before(), Block.NOTIFY_LISTENERS)) {
            job.changed++;
        } else {
            job.skipped++;
        }
    }

    // ---- Colocação do pincel (sobrevivência) ----

    /** Coloca o bloco-alvo em uma célula da região. Consome um item fora do criativo. */
    public static AreaResult place(ServerPlayerEntity player, AreaPlaceRequest request) {
        UUID uuid = player.getUuid();
        long now = Util.getMeasuringTimeMs();
        Long last = LAST_PLACE.get(uuid);
        boolean rateOk = AreaOperationValidator.placeRateOk(last == null ? -1L : last, now);
        if (!rateOk) {
            return AreaResult.RATE_LIMITED;
        }
        LAST_PLACE.put(uuid, now);

        Optional<ServerRegionRegistry.ActiveRegion> region = REGIONS.get(uuid);
        boolean known = region.isPresent() && region.get().id().equals(request.regionId());
        World world = player.getWorld();
        boolean dimensionMatches = known && region.get().dimension().equals(dimensionOf(world));
        BlockPos pos = request.pos();
        boolean insideRegion = dimensionMatches && region.get().box().contains(pos);
        BlockState target = dimensionMatches ? region.get().target() : null;
        ItemStack stack = player.getStackInHand(request.hand());
        boolean heldMatches = target != null && !stack.isEmpty() && stack.isOf(target.getBlock().asItem());
        Vec3d center = Vec3d.ofCenter(pos);
        double reachSq = ReplaceBlockValidator.REACH * ReplaceBlockValidator.REACH;

        AreaOperationValidator.PlaceFacts facts = new AreaOperationValidator.PlaceFacts(
                true, known, dimensionMatches, insideRegion, target != null, heldMatches,
                allowedToBuild(player), world.isInBuildLimit(pos), isChunkLoaded(world, pos),
                player.squaredDistanceTo(center), reachSq);
        Optional<AreaResult> refusal = AreaOperationValidator.checkPlacement(facts);
        if (refusal.isPresent()) {
            return refusal.get();
        }

        ServerWorld serverWorld = player.getServerWorld();
        BlockState before = serverWorld.getBlockState(pos);
        if (!(before.isAir() || before.isReplaceable()) || before.equals(target)) {
            return AreaResult.CELL_OCCUPIED;
        }
        if (ReplacementPolicy.evaluateAreaCell(serverWorld, pos, before).isPresent()) {
            return AreaResult.PROTECTED;
        }
        if (ReplacementPolicy.evaluateNewBlock(target.getBlock()).isPresent()) {
            return AreaResult.INVALID_PLACEMENT;
        }
        if (!target.canPlaceAt(serverWorld, pos) || !serverWorld.canPlace(target, pos, ShapeContext.of(player))) {
            return AreaResult.INVALID_PLACEMENT;
        }
        if (!serverWorld.canPlayerModifyAt(player, pos)) {
            return AreaResult.NO_PERMISSION;
        }
        // Ponto de não retorno: a colocação só acontece com tudo validado.
        if (!serverWorld.setBlockState(pos, target, Block.NOTIFY_ALL)) {
            return AreaResult.INVALID_PLACEMENT;
        }
        ReplaceBlockService.finishPlacement(player, serverWorld, pos, target, stack);
        return AreaResult.PLACED;
    }

    // ---- Tick e fatias ----

    private static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) {
            return;
        }
        Iterator<Job> it = JOBS.values().iterator();
        while (it.hasNext()) {
            Job job = it.next();
            try {
                job.runner.runSlice(AreaLimits.JOB_SLICE_CELLS);
            } catch (RuntimeException e) {
                // Um erro num trabalho não pode derrubar o tick do servidor: descarta o trabalho e registra.
                LOGGER.error("Falha em operação de área de {}", job.playerId, e);
                it.remove();
                reply(job.player, AreaResultPacket.of(AreaResult.INTERNAL_ERROR));
                continue;
            }
            if (job.runner.finished()) {
                it.remove();
                complete(job);
            }
        }
    }

    private static void complete(Job job) {
        if (!job.undo && !job.journal.isEmpty()) {
            AreaUndoBuffer<CellSnapshot> buffer = UNDO.computeIfAbsent(job.playerId,
                    id -> new AreaUndoBuffer<>(AreaLimits.UNDO_MAX_CELLS));
            if (!buffer.push(job.journal)) {
                LOGGER.warn("Lote de {} grande demais para desfazer", job.playerId);
            }
        }
        reply(job.player, new AreaResultPacket(AreaResult.DONE, job.changed, job.skipped));
    }

    /**
     * Desconexão: cancela o trabalho em andamento. As células já alteradas ficam como estão,
     * e o histórico de desfazer do jogador é descartado (ele vale só dentro da sessão).
     */
    private static void forget(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        JOBS.remove(uuid);
        UNDO.remove(uuid);
        LAST_PLACE.remove(uuid);
        REGIONS.remove(uuid);
    }

    private static void resetAll() {
        JOBS.clear();
        UNDO.clear();
        LAST_PLACE.clear();
        REGIONS.clear();
    }

    // ---- Envio e auxiliares ----

    /** Envia um resultado ao cliente. Usado pelos receptores e pelo fim de cada trabalho. */
    public static void reply(ServerPlayerEntity player, AreaResultPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        ServerPlayNetworking.send(player, ModIds.AREA_RESULT, buf);
    }

    private static boolean isCreative(PlayerEntity player) {
        return player.getAbilities().creativeMode;
    }

    private static boolean allowedToBuild(ServerPlayerEntity player) {
        return !player.isSpectator() && player.interactionManager.getGameMode() != GameMode.ADVENTURE;
    }

    private static boolean isChunkLoaded(World world, BlockPos pos) {
        return world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static String dimensionOf(World world) {
        return world.getRegistryKey().getValue().toString();
    }
}

package com.bluewire.config;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DynamicWireRenderer_argb {
    private static final Logger LOGGER = LoggerFactory.getLogger("bluewire-renderer");
    private static final LongSet WIRE_POSITIONS = new LongOpenHashSet();
    private static final long START_NANOS = System.nanoTime();
    private static boolean registered;
    private static boolean initialScanPending;

    private DynamicWireRenderer_argb() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> scanChunk(chunk));
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> removeChunk(chunk.getPos()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            WIRE_POSITIONS.clear();
            initialScanPending = false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!initialScanPending || client.world == null || client.player == null) return;
            initialScanPending = false;
            scanLoadedChunks(client);
            LOGGER.info("Dynamic ARGB cache initialized: {} redstone wires", WIRE_POSITIONS.size());
        });

        WorldRenderEvents.AFTER_ENTITIES.register(DynamicWireRenderer_argb::render);
        LOGGER.info("Dynamic ARGB renderer registered (vanilla block-model pass)");
    }

    public static void onBlockStateChanged(BlockPos pos, BlockState state) {
        if (state.isOf(Blocks.REDSTONE_WIRE)) {
            WIRE_POSITIONS.add(pos.asLong());
        } else {
            WIRE_POSITIONS.remove(pos.asLong());
        }
    }

    private static void reset() {
        WIRE_POSITIONS.clear();
        initialScanPending = true;
    }

    private static void scanLoadedChunks(MinecraftClient client) {
        int radius = client.options.getViewDistance().getValue();
        ChunkPos center = client.player.getChunkPos();
        for (int chunkX = center.x - radius; chunkX <= center.x + radius; chunkX++) {
            for (int chunkZ = center.z - radius; chunkZ <= center.z + radius; chunkZ++) {
                WorldChunk chunk = client.world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                if (chunk != null) scanChunk(chunk);
            }
        }
    }

    private static void scanChunk(WorldChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int baseX = chunkPos.getStartX();
        int baseZ = chunkPos.getStartZ();
        ChunkSection[] sections = chunk.getSectionArray();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            ChunkSection section = sections[sectionIndex];
            if (section == null || section.isEmpty()
                    || !section.hasAny(state -> state.isOf(Blocks.REDSTONE_WIRE))) {
                continue;
            }

            int baseY = ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(sectionIndex));
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        if (section.getBlockState(localX, localY, localZ).isOf(Blocks.REDSTONE_WIRE)) {
                            WIRE_POSITIONS.add(new BlockPos(
                                baseX + localX,
                                baseY + localY,
                                baseZ + localZ
                            ).asLong());
                        }
                    }
                }
            }
        }
    }

    private static void removeChunk(ChunkPos chunkPos) {
        LongIterator iterator = WIRE_POSITIONS.iterator();
        while (iterator.hasNext()) {
            long packed = iterator.nextLong();
            if ((BlockPos.unpackLongX(packed) >> 4) == chunkPos.x
                    && (BlockPos.unpackLongZ(packed) >> 4) == chunkPos.z) {
                iterator.remove();
            }
        }
    }

    private static void render(WorldRenderContext context) {
        BluewireConfig config = BluewireConfig.getInstance();
        if (!config.rainbow || WIRE_POSITIONS.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider consumers = context.consumers();
        if (client.world == null || client.player == null || consumers == null) return;

        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();
        BlockRenderManager blockRenderer = client.getBlockRenderManager();
        double renderRadius = client.options.getViewDistance().getValue() * 16.0 + 16.0;
        double renderRadiusSquared = renderRadius * renderRadius;
        double elapsedSeconds = (System.nanoTime() - START_NANOS) / 1_000_000_000.0;

        LongIterator iterator = WIRE_POSITIONS.iterator();
        while (iterator.hasNext()) {
            long packed = iterator.nextLong();
            int x = BlockPos.unpackLongX(packed);
            int y = BlockPos.unpackLongY(packed);
            int z = BlockPos.unpackLongZ(packed);

            double dx = x + 0.5 - cameraPos.x;
            double dy = y + 0.5 - cameraPos.y;
            double dz = z + 0.5 - cameraPos.z;
            if (dx * dx + dy * dy + dz * dz > renderRadiusSquared) continue;

            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = client.world.getBlockState(pos);
            if (!state.isOf(Blocks.REDSTONE_WIRE)) {
                iterator.remove();
                continue;
            }

            int color = config.getDynamicWireColor(state, pos, elapsedSeconds);
            int light = WorldRenderer.getLightmapCoordinates(client.world, state, pos);

            matrices.push();
            matrices.translate(x - cameraPos.x, y + 0.002 - cameraPos.y, z - cameraPos.z);
            BluewireConfig.setDynamicColorOverride(color);
            try {
                blockRenderer.renderBlockAsEntity(
                    state,
                    matrices,
                    consumers,
                    light,
                    OverlayTexture.DEFAULT_UV
                );
            } finally {
                BluewireConfig.clearDynamicColorOverride();
                matrices.pop();
            }
        }
    }
}

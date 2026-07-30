package com.example.config;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

public class RainbowOverlayRenderer {
    private static final LongSet wirePositions = new LongOpenHashSet();
    private static boolean registered;
    private static boolean scanned;

    public static void register() {
        if (registered) return;
        registered = true;

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk)   -> scanChunk(chunk, true));
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> scanChunk(chunk, false));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> scanned = false);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (scanned || client.world == null || client.player == null) return;
            scanned = true;
            wirePositions.clear();
            int r = client.options.getViewDistance().getValue();
            var cp = client.player.getChunkPos();
            for (int cx = cp.x - r; cx <= cp.x + r; cx++)
                for (int cz = cp.z - r; cz <= cp.z + r; cz++) {
                    var c = client.world.getChunkManager().getWorldChunk(cx, cz);
                    if (c != null) scanChunk(c, true);
                }
        });

        WorldRenderEvents.LAST.register(context -> {
            if (!BluewireConfig.getInstance().rainbow) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;
            if (wirePositions.isEmpty()) return;

            Vec3d camPos = context.camera().getPos();
            MatrixStack matrices = context.matrixStack();
            int viewDist = client.options.getViewDistance().getValue() * 16;

            VertexConsumerProvider.Immediate provider =
                VertexConsumerProvider.immediate(new BufferBuilder(131072));
            VertexConsumer vc = provider.getBuffer(RenderLayer.getTranslucent());

            for (long packed : wirePositions) {
                int bx = BlockPos.unpackLongX(packed);
                int by = BlockPos.unpackLongY(packed);
                int bz = BlockPos.unpackLongZ(packed);

                if (Math.abs(bx - client.player.getX()) > viewDist ||
                    Math.abs(bz - client.player.getZ()) > viewDist) continue;

                BlockState state = client.world.getBlockState(new BlockPos(bx, by, bz));
                if (!state.isOf(Blocks.REDSTONE_WIRE)) continue;

                int power = state.get(net.minecraft.block.RedstoneWireBlock.POWER);
                int color = BluewireConfig.getWireColor(power);
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8)  & 0xFF) / 255f;
                float b = ( color        & 0xFF) / 255f;

                float x = (float)(bx - camPos.x);
                float y = (float)(by + 0.022f - camPos.y);
                float z = (float)(bz - camPos.z);

                matrices.push();
                matrices.translate(x, y, z);
                Matrix4f mat = matrices.peek().getPositionMatrix();
                vc.vertex(mat, 0, 0, 0).color(r, g, b, 0.55f).next();
                vc.vertex(mat, 1, 0, 0).color(r, g, b, 0.55f).next();
                vc.vertex(mat, 1, 0, 1).color(r, g, b, 0.55f).next();
                vc.vertex(mat, 0, 0, 1).color(r, g, b, 0.55f).next();
                matrices.pop();
            }

            provider.draw();
        });
    }

    private static void scanChunk(WorldChunk chunk, boolean add) {
        ChunkPos cp = chunk.getPos();
        int bottom = chunk.getBottomY();
        int top = Math.min(chunk.getTopY(), 64);
        for (int bx = 0; bx < 16; bx++)
            for (int bz = 0; bz < 16; bz++)
                for (int by = bottom; by < top; by++) {
                    BlockPos pos = new BlockPos((cp.x << 4) + bx, by, (cp.z << 4) + bz);
                    if (chunk.getBlockState(pos).isOf(Blocks.REDSTONE_WIRE)) {
                        if (add) wirePositions.add(pos.asLong());
                        else     wirePositions.remove(pos.asLong());
                    }
                }
    }
}

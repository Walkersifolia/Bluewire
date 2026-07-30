package com.example.config;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
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

/**
 * 在红石线上绘制完全不透明的动态颜色覆盖层，
 * 通过区块加载事件维护位置缓存，无定期全量扫描（无卡顿）。
 */
public class RainbowOverlayRenderer {
    private static final LongSet wirePositions = new LongOpenHashSet();
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;

        // 区块加载 → 扫描红石线位置
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) ->
            scanChunk(chunk, true));

        // 区块卸载 → 移除位置
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
            scanChunk(chunk, false));

        // 每帧渲染：不透明覆盖层，直接盖住原版颜色
        WorldRenderEvents.LAST.register(context -> {
            if (!BluewireConfig.getInstance().rainbow) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;

            Vec3d camPos = context.camera().getPos();
            MatrixStack matrices = context.matrixStack();
            int viewDist = client.options.getViewDistance().getValue() * 16;

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);

            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

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
                float y = (float)(by + 0.02 - camPos.y);
                float z = (float)(bz - camPos.z);

                matrices.push();
                matrices.translate(x, y, z);
                Matrix4f mat = matrices.peek().getPositionMatrix();
                buffer.vertex(mat, 0, 0, 0).color(r, g, b, 1f).next();
                buffer.vertex(mat, 1, 0, 0).color(r, g, b, 1f).next();
                buffer.vertex(mat, 1, 0, 1).color(r, g, b, 1f).next();
                buffer.vertex(mat, 0, 0, 1).color(r, g, b, 1f).next();
                matrices.pop();
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
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
                        else wirePositions.remove(pos.asLong());
                    }
                }
    }
}

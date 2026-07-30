package com.example.config;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

/**
 * 在红石线方块上方直接绘制动态颜色覆盖层，每帧更新，不依赖区块重建。
 */
public class RainbowOverlayRenderer {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;

        WorldRenderEvents.LAST.register(context -> {
            if (!BluewireConfig.getInstance().rainbow) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null || client.cameraEntity == null) return;

            Camera camera = context.camera();
            Vec3d camPos = camera.getPos();
            MatrixStack matrices = context.matrixStack();

            BlockPos playerPos = client.player.getBlockPos();
            int chunkRadius = client.options.getViewDistance().getValue();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.depthMask(false);

            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            int rendered = 0;
            for (int cx = playerPos.getX() / 16 - chunkRadius; cx <= playerPos.getX() / 16 + chunkRadius && rendered < 5000; cx++) {
                for (int cz = playerPos.getZ() / 16 - chunkRadius; cz <= playerPos.getZ() / 16 + chunkRadius && rendered < 5000; cz++) {
                    WorldChunk chunk = client.world.getChunkManager().getWorldChunk(cx, cz);
                    if (chunk == null) continue;
                    for (int bx = 0; bx < 16 && rendered < 5000; bx++) {
                        for (int bz = 0; bz < 16 && rendered < 5000; bz++) {
                            for (int by = client.world.getBottomY(); by < 64 && rendered < 5000; by++) {
                                BlockPos pos = new BlockPos((cx * 16) + bx, by, (cz * 16) + bz);
                                if (!chunk.getBlockState(pos).isOf(Blocks.REDSTONE_WIRE)) continue;

                                int power = chunk.getBlockState(pos).get(net.minecraft.block.RedstoneWireBlock.POWER);
                                int color = BluewireConfig.getWireColor(power);
                                float r = ((color >> 16) & 0xFF) / 255f;
                                float g = ((color >> 8) & 0xFF) / 255f;
                                float b = (color & 0xFF) / 255f;
                                float a = 0.65f;

                                float x = (float)(pos.getX() - camPos.x);
                                float y = (float)(pos.getY() + 0.02 - camPos.y);
                                float z = (float)(pos.getZ() - camPos.z);

                                matrices.push();
                                matrices.translate(x, y, z);
                                Matrix4f mat = matrices.peek().getPositionMatrix();
                                buffer.vertex(mat, 0, 0, 0).color(r, g, b, a).next();
                                buffer.vertex(mat, 1, 0, 0).color(r, g, b, a).next();
                                buffer.vertex(mat, 1, 0, 1).color(r, g, b, a).next();
                                buffer.vertex(mat, 0, 0, 1).color(r, g, b, a).next();
                                matrices.pop();
                                rendered++;
                            }
                        }
                    }
                }
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        });
    }
}

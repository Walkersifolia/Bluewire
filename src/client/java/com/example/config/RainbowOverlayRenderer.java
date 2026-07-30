package com.example.config;

import com.mojang.blaze3d.systems.RenderSystem;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ARGB 炫彩覆盖渲染器，含诊断模式。
 *
 * 调用链：
 *   ExampleModClient → register()
 *     ├─ ClientChunkEvents → wirePositions
 *     ├─ ClientTickEvents → 初始扫描
 *     └─ WorldRenderEvents.LAST → 每帧绘制
 */
public class RainbowOverlayRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bluewire-renderer");
    private static final LongSet wirePositions = new LongOpenHashSet();
    private static final long START_NANOS = System.nanoTime();
    private static boolean registered, scanned;
    private static long frameCounter;
    private static int consecutiveErrors;

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
            frameCounter++;
            BluewireConfig cfg = BluewireConfig.getInstance();
            MinecraftClient client = MinecraftClient.getInstance();
            boolean worldOk = client.world != null && client.player != null;
            Camera camera = client.gameRenderer != null ? client.gameRenderer.getCamera() : null;
            Vec3d camPos = camera != null ? camera.getPos() : Vec3d.ZERO;

            // ── 诊断日志：每60帧输出一次，无论模式 ──
            if (cfg.rainbowDebugMode && frameCounter % 60 == 0) {
                double elapsed = (System.nanoTime() - START_NANOS) / 1_000_000_000.0;
                double hue = (elapsed * cfg.rainbowSpeed) % 1.0;
                int argb = cfg.rainbow ? BluewireConfig.getWireColor(8) : 0;
                LOGGER.info("[RainbowDebug] frame={} elapsed={} hue={} argb=#{} rainbow={} speed={} wires={} world={} camPos={}",
                    frameCounter,
                    String.format("%.3f", elapsed),
                    String.format("%.6f", hue),
                    Integer.toHexString(argb),
                    cfg.rainbow, cfg.rainbowSpeed, wirePositions.size(), worldOk,
                    String.format("%.1f,%.1f,%.1f", camPos.x, camPos.y, camPos.z));
            }

            if (!cfg.rainbow) return;
            if (!worldOk) return;
            if (camera == null) return;

            // ── 诊断覆盖层 ──
            if (cfg.rainbowDebugOverlay) {
                try {
                    drawDebugOverlay(camera, camPos, client);
                    consecutiveErrors = 0;
                } catch (Exception e) {
                    consecutiveErrors++;
                    if (consecutiveErrors <= 3)
                        LOGGER.error("[RainbowDebug] 诊断覆盖层异常", e);
                }
                return;
            }

            // ── 正常 ARGB ──
            if (wirePositions.isEmpty()) return;
            try {
                drawRainbowOverlay(camera, camPos, client);
                consecutiveErrors = 0;
            } catch (Exception e) {
                consecutiveErrors++;
                if (consecutiveErrors <= 3 || consecutiveErrors % 100 == 0)
                    LOGGER.error("[RainbowDebug] 渲染异常 #{}", consecutiveErrors, e);
            }
        });
    }

    // ── 诊断覆盖层：相机前方 3 格处画品红/青色交替方块 ──
    private static void drawDebugOverlay(Camera camera, Vec3d camPos, MinecraftClient client) {
        Vec3d look = camera.getPos().add(
            camera.getHorizontalPlane().x * 3,
            camera.getHorizontalPlane().y * 3,
            camera.getHorizontalPlane().z * 3);
        // 简化：用玩家朝向的前方3格
        float yaw = client.player != null ? client.player.getYaw() : 0;
        double rad = Math.toRadians(-yaw);
        double lookX = camPos.x - Math.sin(rad) * 3;
        double lookZ = camPos.z - Math.cos(rad) * 3;
        double lookY = camPos.y + 1.5;

        float sx = (float)(lookX - camPos.x);
        float sy = (float)(lookY - camPos.y);
        float sz = (float)(lookZ - camPos.z);

        boolean phase = (frameCounter / 15) % 2 == 0;
        float r = phase ? 1 : 0, g = 0, b = phase ? 0 : 1, a = 1;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        MatrixStack ms = new MatrixStack();
        ms.push();
        ms.translate(sx, sy, sz);
        Matrix4f mat = ms.peek().getPositionMatrix();
        buf.vertex(mat, -0.5f,  0.5f, 0).color(r, g, b, a).next();
        buf.vertex(mat,  0.5f,  0.5f, 0).color(r, g, b, a).next();
        buf.vertex(mat,  0.5f, -0.5f, 0).color(r, g, b, a).next();
        buf.vertex(mat, -0.5f, -0.5f, 0).color(r, g, b, a).next();
        ms.pop();

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    // ── 正常 ARGB 覆盖层 ──
    private static void drawRainbowOverlay(Camera camera, Vec3d camPos, MinecraftClient client) {
        int viewDist = client.options.getViewDistance().getValue() * 16;
        MatrixStack ms = new MatrixStack();

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

            ms.push();
            ms.translate(x, y, z);
            Matrix4f mat = ms.peek().getPositionMatrix();
            org.joml.Matrix3f nmat = ms.peek().getNormalMatrix();
            vc.vertex(mat, 0, 0, 0).color(r, g, b, 0.55f).texture(0, 0).overlay(0).light(15728880).normal(nmat, 0, 1, 0).next();
            vc.vertex(mat, 1, 0, 0).color(r, g, b, 0.55f).texture(0, 1).overlay(0).light(15728880).normal(nmat, 0, 1, 0).next();
            vc.vertex(mat, 1, 0, 1).color(r, g, b, 0.55f).texture(1, 1).overlay(0).light(15728880).normal(nmat, 0, 1, 0).next();
            vc.vertex(mat, 0, 0, 1).color(r, g, b, 0.55f).texture(1, 0).overlay(0).light(15728880).normal(nmat, 0, 1, 0).next();
            ms.pop();
        }

        provider.draw();
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

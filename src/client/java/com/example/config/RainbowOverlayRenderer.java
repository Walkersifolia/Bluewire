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

public class RainbowOverlayRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bluewire-rainbow");
    private static final LongSet wirePositions = new LongOpenHashSet();
    private static boolean registered;
    private static boolean scanned;

    private static final long START_NANOS = System.nanoTime();
    private static long frameCounter;
    private static int consecutiveErrors;

    public static void register() {
        if (registered) return;
        registered = true;

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk)   -> scanChunk(chunk, true));
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> scanChunk(chunk, false));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            scanned = false;
            frameCounter = 0;
            consecutiveErrors = 0;
        });
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
            BluewireConfig cfg = BluewireConfig.getInstance();
            MinecraftClient client = MinecraftClient.getInstance();
            boolean worldOk = client.world != null && client.player != null;

            frameCounter++;

            if (!cfg.rainbow) return;
            if (!worldOk) return;

            if (cfg.rainbowDebugOverlay) {
                drawDebugOverlay(client);
                return;
            }

            if (wirePositions.isEmpty()) return;

            try {
                drawRainbowOverlay(client);
                consecutiveErrors = 0;
            } catch (Exception e) {
                consecutiveErrors++;
                if (consecutiveErrors <= 3 || consecutiveErrors % 100 == 0) {
                    LOGGER.error("[RainbowDebug] render error #" + consecutiveErrors, e);
                }
            }
        });
    }

    // ═══════════════════════════════════════
    // 诊断覆盖层：相机前 3 格品红/青闪烁
    // ═══════════════════════════════════════
    private static void drawDebugOverlay(MinecraftClient client) {
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = new MatrixStack();

        // 相机前方 3 格
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        double lookX = camPos.x - Math.sin(Math.toRadians(yaw)) * 3.0;
        double lookY = camPos.y - Math.tan(Math.toRadians(pitch)) * 3.0;
        double lookZ = camPos.z + Math.cos(Math.toRadians(yaw)) * 3.0;

        float sx = (float)(lookX - camPos.x);
        float sy = (float)(lookY - camPos.y);
        float sz = (float)(lookZ - camPos.z);

        // 每 15 帧切换品红/青色
        boolean phase = (frameCounter / 15) % 2 == 0;
        int color = phase ? 0xFFFF00FF : 0xFF00FFFF;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;

        // 用 Tessellator + POSITION_COLOR（不使用纹理/法线/光照层）
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // 画 1×1×1 的方块在相机前方
        matrices.push();
        matrices.translate(sx, sy, sz);
        Matrix4f mat = matrices.peek().getPositionMatrix();
        buffer.vertex(mat,  0,  0,  0).color(r, g, b, 1f).next();
        buffer.vertex(mat,  1,  0,  0).color(r, g, b, 1f).next();
        buffer.vertex(mat,  1,  1,  0).color(r, g, b, 1f).next();
        buffer.vertex(mat,  0,  1,  0).color(r, g, b, 1f).next();
        matrices.pop();

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        // 调试日志
        if (cfg.rainbowDebugMode && frameCounter % 60 == 0) {
            double elapsed = (System.nanoTime() - START_NANOS) / 1_000_000_000.0;
            double hue = elapsed * cfg.rainbowSpeed;
            hue = hue - Math.floor(hue);
            int argb = BluewireConfig.hsvToRgb((float)(hue * 360), 1.0F, 1.0F);
            LOGGER.info("[RainbowDebug] frame={} elapsed={}s hue={} argb=#{} rainbow={} speed={} wires={} mode=debugOverlay phase={} cam=({},{},{})",
                frameCounter, String.format("%.3f", elapsed), String.format("%.4f", hue),
                Integer.toHexString(argb), cfg.rainbow, cfg.rainbowSpeed, wirePositions.size(),
                phase, String.format("%.1f", camPos.x), String.format("%.1f", camPos.y), String.format("%.1f", camPos.z));
        }
    }

    // ═══════════════════════════════════════
    // 正常 ARGB 覆盖层
    // ═══════════════════════════════════════
    private static void drawRainbowOverlay(MinecraftClient client) {
        BluewireConfig cfg = BluewireConfig.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = new MatrixStack();
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
            float y = (float)(by + 0.06f - camPos.y);
            float z = (float)(bz - camPos.z);

            matrices.push();
            matrices.translate(x, y, z);
            Matrix4f mat = matrices.peek().getPositionMatrix();
            org.joml.Matrix3f nMat = matrices.peek().getNormalMatrix();
            vc.vertex(mat, 0, 0, 0).color(r, g, b, 0.5f).texture(0, 0).overlay(0).light(15728880).normal(nMat, 0, 1, 0).next();
            vc.vertex(mat, 1, 0, 0).color(r, g, b, 0.5f).texture(0, 1).overlay(0).light(15728880).normal(nMat, 0, 1, 0).next();
            vc.vertex(mat, 1, 0, 1).color(r, g, b, 0.5f).texture(1, 1).overlay(0).light(15728880).normal(nMat, 0, 1, 0).next();
            vc.vertex(mat, 0, 0, 1).color(r, g, b, 0.5f).texture(1, 0).overlay(0).light(15728880).normal(nMat, 0, 1, 0).next();
            matrices.pop();
        }

        provider.draw();

        // 调试日志
        if (cfg.rainbowDebugMode && frameCounter % 60 == 0) {
            double elapsed = (System.nanoTime() - START_NANOS) / 1_000_000_000.0;
            double hue = elapsed * cfg.rainbowSpeed;
            hue = hue - Math.floor(hue);
            long first = wirePositions.isEmpty() ? 0 : wirePositions.iterator().nextLong();
            int bx = BlockPos.unpackLongX(first), by = BlockPos.unpackLongY(first), bz = BlockPos.unpackLongZ(first);
            BlockState st = first != 0 ? client.world.getBlockState(new BlockPos(bx, by, bz)) : null;
            int pw = st != null && st.isOf(Blocks.REDSTONE_WIRE) ? st.get(net.minecraft.block.RedstoneWireBlock.POWER) : -1;
            int argb = pw >= 0 ? BluewireConfig.getWireColor(pw) : 0;
            LOGGER.info("[RainbowDebug] frame={} elapsed={}s hue={} argb=#{} rainbow={} speed={} wires={} power={} cam=({},{},{})",
                frameCounter, String.format("%.3f", elapsed), String.format("%.4f", hue),
                Integer.toHexString(argb), cfg.rainbow, cfg.rainbowSpeed, wirePositions.size(), pw,
                String.format("%.1f", camPos.x), String.format("%.1f", camPos.y), String.format("%.1f", camPos.z));
        }
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

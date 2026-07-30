package com.example.config;

import com.example.mixin.WorldRendererAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

/**
 * ARGB 渐变驱动：直接通过 BuiltChunkStorage.scheduleRebuild 标记含红石线的区块重建。
 */
public class RainbowAnimator {
    private static int tickCounter;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!BluewireConfig.getInstance().rainbow) return;
            if (client.world == null || client.player == null || client.worldRenderer == null) return;

            tickCounter++;
            float speed = BluewireConfig.getInstance().rainbowSpeed;
            int interval = Math.max(1, Math.round(4.0F / speed));
            if (tickCounter % interval != 0) return;

            BuiltChunkStorage chunks = ((WorldRendererAccessor) client.worldRenderer).getChunks();
            if (chunks == null) return;

            BlockPos playerPos = client.player.getBlockPos();
            int r = client.options.getViewDistance().getValue();
            for (int cx = (playerPos.getX() >> 4) - r; cx <= (playerPos.getX() >> 4) + r; cx++) {
                for (int cz = (playerPos.getZ() >> 4) - r; cz <= (playerPos.getZ() >> 4) + r; cz++) {
                    WorldChunk chunk = client.world.getChunkManager().getWorldChunk(cx, cz);
                    if (chunk == null) continue;
                    int bottom = client.world.getBottomSectionCoord();
                    int top = client.world.getTopSectionCoord();
                    for (int sy = bottom; sy < top && sy * 16 < 64; sy++) {
                        if (chunk.getSection(chunk.getSectionIndex(sy * 16)) == null) continue;
                        chunks.scheduleRebuild((cx << 4) + 8, sy * 16 + 8, (cz << 4) + 8, true);
                    }
                }
            }
        });
    }
}

package com.example.config;

import com.example.config.BluewireConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * 当 ARGB 渐变模式开启时，周期性将玩家视野内的区块标记为需要重绘，
 * 使每个功率等级的红石线颜色随时间动态变化。
 */
public class RainbowAnimator {
    private static int tickCounter;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!BluewireConfig.getInstance().rainbow) return;
            if (client.world == null || client.player == null || client.worldRenderer == null) return;

            // 根据速度决定刷新间隔：速度越快，刷新越频繁
            // 速度 1.0 → 每 4 tick 调度一次（每秒约 5 次）
            // 速度 5.0 → 每 1 tick 调度一次（每秒 20 次）
            tickCounter++;
            float speed = BluewireConfig.getInstance().rainbowSpeed;
            int interval = Math.max(1, Math.round(4.0F / speed));
            if (tickCounter % interval != 0) return;

            // 将玩家周围所有已加载区块标记为重绘
            BlockPos playerPos = client.player.getBlockPos();
            int radius = client.options.getViewDistance().getValue() * 16;
            client.worldRenderer.scheduleBlockRenders(
                playerPos.getX() - radius, playerPos.getY() - 16, playerPos.getZ() - radius,
                playerPos.getX() + radius, playerPos.getY() + 16, playerPos.getZ() + radius
            );
        });
    }
}

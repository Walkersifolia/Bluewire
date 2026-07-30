package com.example.config;

import com.example.config.BluewireConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * 当 ARGB 渐变模式开启时，周期性触发热区块重绘，
 * 使每种功率的红石线颜色持续动态变化。
 */
public class RainbowAnimator {
    private static int tickCounter;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!BluewireConfig.getInstance().rainbow) return;
            if (client.worldRenderer == null) return;

            // 根据速度决定刷新间隔：速度越快，刷新越频繁
            // 速度 1.0 → 每 4 tick 刷新（每秒约 5 次）
            // 速度 5.0 → 每 1 tick 刷新（每秒 20 次）
            tickCounter++;
            float speed = BluewireConfig.getInstance().rainbowSpeed;
            int interval = Math.max(1, Math.round(4.0F / speed));
            if (tickCounter % interval == 0) {
                client.worldRenderer.reload();
            }
        });
    }
}

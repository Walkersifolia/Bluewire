package com.example.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class BluewireConfigScreen {
    private BluewireConfigScreen() {}

    public static Screen create(Screen parent) {
        BluewireConfig cfg = BluewireConfig.getInstance();
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("Bluewire 设置"))
            .setSavingRunnable(() -> {
                cfg.save();
                BluewireConfig.updateColors();
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.worldRenderer != null) client.worldRenderer.reload();
            });

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory catStatic = builder.getOrCreateCategory(Text.literal("静态颜色"));
        catStatic.addEntry(eb.startColorField(Text.literal("高功率颜色"), BluewireConfig.floatRgbToInt(cfg.highRed, cfg.highGreen, cfg.highBlue))
            .setDefaultValue(BluewireConfig.floatRgbToInt(0,0.5f,1))
            .setTooltip(Text.literal("红石信号强度为 15 时的线缆颜色"))
            .setSaveConsumer(v -> { float[] rgb = BluewireConfig.intToFloatRgb(v); cfg.highRed=rgb[0]; cfg.highGreen=rgb[1]; cfg.highBlue=rgb[2]; })
            .build());
        catStatic.addEntry(eb.startColorField(Text.literal("低功率颜色"), BluewireConfig.floatRgbToInt(cfg.lowRed, cfg.lowGreen, cfg.lowBlue))
            .setDefaultValue(BluewireConfig.floatRgbToInt(0,0,0.3f))
            .setTooltip(Text.literal("红石信号强度为 0 时的线缆颜色"))
            .setSaveConsumer(v -> { float[] rgb = BluewireConfig.intToFloatRgb(v); cfg.lowRed=rgb[0]; cfg.lowGreen=rgb[1]; cfg.lowBlue=rgb[2]; })
            .build());
        catStatic.addEntry(eb.startBooleanToggle(Text.literal("反转方向"), cfg.reverse)
            .setDefaultValue(false)
            .setTooltip(Text.literal("开启后，最亮的颜色出现在功率 0 而非功率 15"))
            .setSaveConsumer(v -> cfg.reverse = v)
            .build());

        ConfigCategory catRainbow = builder.getOrCreateCategory(Text.literal("ARGB 渐变"));
        catRainbow.addEntry(eb.startBooleanToggle(Text.literal("启用自动渐变"), cfg.rainbow)
            .setDefaultValue(false)
            .setTooltip(Text.literal("开启后红石线颜色会自动循环渐变，类似电脑 ARGB 灯效。"))
            .setSaveConsumer(v -> cfg.rainbow = v)
            .build());
        catRainbow.addEntry(eb.startFloatField(Text.literal("渐变速度"), cfg.rainbowSpeed)
            .setDefaultValue(1.0F)
            .setMin(0.1F).setMax(5.0F)
            .setTooltip(Text.literal("1.0=默认，越大越快"))
            .setSaveConsumer(v -> cfg.rainbowSpeed = v)
            .build());

        // ── 诊断开关 ──
        ConfigCategory catDebug = builder.getOrCreateCategory(Text.literal("诊断"));
        catDebug.addEntry(eb.startBooleanToggle(Text.literal("调试日志"), cfg.rainbowDebugMode)
            .setDefaultValue(false)
            .setTooltip(Text.literal("开启后每约60帧输出渲染状态日志"))
            .setSaveConsumer(v -> cfg.rainbowDebugMode = v)
            .build());
        catDebug.addEntry(eb.startBooleanToggle(Text.literal("诊断覆盖层"), cfg.rainbowDebugOverlay)
            .setDefaultValue(false)
            .setTooltip(Text.literal("品红/青闪烁四边形+红石tint置黑，用于诊断渲染管线"))
            .setSaveConsumer(v -> cfg.rainbowDebugOverlay = v)
            .build());

        return builder.build();
    }
}

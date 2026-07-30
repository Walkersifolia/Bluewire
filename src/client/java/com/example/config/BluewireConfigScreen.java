package com.example.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * 客户端 GUI 构建器。由于 splitEnvironmentSourceSets() 会阻止客户端类出现在 main 源集中，
 * 因此与 BluewireConfig 分开放置。
 */
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
                // 立即刷新世界渲染，使颜色变更无需退出存档即可看到效果
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.worldRenderer != null) {
                    client.worldRenderer.reload();
                }
            });

        ConfigCategory category = builder.getOrCreateCategory(Text.literal("红石线颜色"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        int defaultHigh = BluewireConfig.floatRgbToInt(0.0F, 0.5F, 1.0F);
        int defaultLow  = BluewireConfig.floatRgbToInt(0.0F, 0.0F, 0.3F);

        category.addEntry(eb.startColorField(
                Text.literal("高功率颜色"),
                BluewireConfig.floatRgbToInt(cfg.highRed, cfg.highGreen, cfg.highBlue)
            )
            .setDefaultValue(defaultHigh)
            .setTooltip(Text.literal("红石信号强度为 15 时的线缆颜色"))
            .setSaveConsumer(value -> {
                float[] rgb = BluewireConfig.intToFloatRgb(value);
                cfg.highRed   = rgb[0];
                cfg.highGreen = rgb[1];
                cfg.highBlue  = rgb[2];
            })
            .build()
        );

        category.addEntry(eb.startColorField(
                Text.literal("低功率颜色"),
                BluewireConfig.floatRgbToInt(cfg.lowRed, cfg.lowGreen, cfg.lowBlue)
            )
            .setDefaultValue(defaultLow)
            .setTooltip(Text.literal("红石信号强度为 0 时的线缆颜色"))
            .setSaveConsumer(value -> {
                float[] rgb = BluewireConfig.intToFloatRgb(value);
                cfg.lowRed   = rgb[0];
                cfg.lowGreen = rgb[1];
                cfg.lowBlue  = rgb[2];
            })
            .build()
        );

        category.addEntry(eb.startBooleanToggle(
                Text.literal("反转方向"),
                cfg.reverse
            )
            .setDefaultValue(false)
            .setTooltip(Text.literal("开启后，最亮的颜色出现在功率 0 而非功率 15"))
            .setSaveConsumer(value -> cfg.reverse = value)
            .build()
        );

        return builder.build();
    }
}

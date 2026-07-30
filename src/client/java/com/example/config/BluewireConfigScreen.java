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

        ConfigCategory cat = builder.getOrCreateCategory(Text.literal("红石线颜色"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        cat.addEntry(eb.startColorField(Text.literal("高功率颜色"), BluewireConfig.floatRgbToInt(cfg.highRed, cfg.highGreen, cfg.highBlue))
            .setDefaultValue(BluewireConfig.floatRgbToInt(0,0.5f,1))
            .setTooltip(Text.literal("红石信号强度为 15 时的线缆颜色"))
            .setSaveConsumer(v -> { float[] rgb = BluewireConfig.intToFloatRgb(v); cfg.highRed=rgb[0]; cfg.highGreen=rgb[1]; cfg.highBlue=rgb[2]; })
            .build());

        cat.addEntry(eb.startColorField(Text.literal("低功率颜色"), BluewireConfig.floatRgbToInt(cfg.lowRed, cfg.lowGreen, cfg.lowBlue))
            .setDefaultValue(BluewireConfig.floatRgbToInt(0,0,0.3f))
            .setTooltip(Text.literal("红石信号强度为 0 时的线缆颜色"))
            .setSaveConsumer(v -> { float[] rgb = BluewireConfig.intToFloatRgb(v); cfg.lowRed=rgb[0]; cfg.lowGreen=rgb[1]; cfg.lowBlue=rgb[2]; })
            .build());

        cat.addEntry(eb.startBooleanToggle(Text.literal("反转方向"), cfg.reverse)
            .setDefaultValue(false)
            .setTooltip(Text.literal("开启后，最亮的颜色出现在功率 0 而非功率 15"))
            .setSaveConsumer(v -> cfg.reverse = v)
            .build());

        return builder.build();
    }
}

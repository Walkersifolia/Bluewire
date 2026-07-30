package com.example.config;

import com.example.mixin.ExampleMixin;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Client-only GUI builder.  Separated from BluewireConfig because
 * splitEnvironmentSourceSets() keeps client classes out of the main source set.
 */
public final class BluewireConfigScreen {

    private BluewireConfigScreen() {}

    public static Screen create(Screen parent) {
        BluewireConfig cfg = BluewireConfig.getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("Bluewire Settings"))
            .setSavingRunnable(() -> {
                cfg.save();
                ExampleMixin.updateColors();
            });

        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Wire Colour"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        int defaultHigh = BluewireConfig.floatRgbToInt(0.0F, 0.5F, 1.0F);
        int defaultLow  = BluewireConfig.floatRgbToInt(0.0F, 0.0F, 0.3F);

        category.addEntry(eb.startColorField(
                Text.literal("High Power Colour"),
                BluewireConfig.floatRgbToInt(cfg.highRed, cfg.highGreen, cfg.highBlue)
            )
            .setDefaultValue(defaultHigh)
            .setTooltip(Text.literal("Colour of the wire at redstone power level 15"))
            .setSaveConsumer(value -> {
                float[] rgb = BluewireConfig.intToFloatRgb(value);
                cfg.highRed   = rgb[0];
                cfg.highGreen = rgb[1];
                cfg.highBlue  = rgb[2];
            })
            .build()
        );

        category.addEntry(eb.startColorField(
                Text.literal("Low Power Colour"),
                BluewireConfig.floatRgbToInt(cfg.lowRed, cfg.lowGreen, cfg.lowBlue)
            )
            .setDefaultValue(defaultLow)
            .setTooltip(Text.literal("Colour of the wire at redstone power level 0"))
            .setSaveConsumer(value -> {
                float[] rgb = BluewireConfig.intToFloatRgb(value);
                cfg.lowRed   = rgb[0];
                cfg.lowGreen = rgb[1];
                cfg.lowBlue  = rgb[2];
            })
            .build()
        );

        category.addEntry(eb.startBooleanToggle(
                Text.literal("Reverse Direction"),
                cfg.reverse
            )
            .setDefaultValue(false)
            .setTooltip(Text.literal("When enabled, the brightest colour appears at power 0 instead of power 15"))
            .setSaveConsumer(value -> cfg.reverse = value)
            .build()
        );

        return builder.build();
    }
}

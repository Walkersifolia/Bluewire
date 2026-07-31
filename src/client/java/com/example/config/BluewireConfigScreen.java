package com.example.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class BluewireConfigScreen extends Screen {
    private static final int PREVIEW_W = 48, PREVIEW_H = 20;
    private static final int HIGH_DEFAULT = 0x0080FF;
    private static final int LOW_DEFAULT  = 0x00004D;

    private final Screen parent;
    private final BluewireConfig cfg;

    private int highRgb, lowRgb;
    private TextFieldWidget highHexField, lowHexField;
    private CheckboxWidget revBox;
    private ButtonWidget highResetBtn, lowResetBtn;

    public BluewireConfigScreen(Screen parent) {
        super(Text.literal("Bluewire 设置"));
        this.parent = parent;
        this.cfg = BluewireConfig.getInstance();
        this.highRgb = BluewireConfig.floatRgbToInt(cfg.highRed, cfg.highGreen, cfg.highBlue);
        this.lowRgb  = BluewireConfig.floatRgbToInt(cfg.lowRed,  cfg.lowGreen,  cfg.lowBlue);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int left = cx - 120;
        int y = 42;

        highHexField = hexField(left + PREVIEW_W + 18, y + 22, 70, toHex6(highRgb));
        highHexField.setChangedListener(s -> onHexChanged(true));
        this.addDrawableChild(highHexField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("选择颜色..."), btn -> {
            highRgb = parseHexSafe(highHexField.getText(), highRgb);
            this.client.setScreen(new ColorPickerScreen(this, highRgb, c -> {
                this.highRgb = c;
                rebuild(false);
            }));
        }).dimensions(left + PREVIEW_W + 94, y + 20, 72, 20).build());

        highResetBtn = ButtonWidget.builder(Text.literal("重置"), btn -> {
            this.highRgb = HIGH_DEFAULT;
            rebuild(false);
        }).dimensions(left + PREVIEW_W + 170, y + 20, 36, 20).build();
        this.addDrawableChild(highResetBtn);

        y += 54;
        lowHexField = hexField(left + PREVIEW_W + 18, y + 22, 70, toHex6(lowRgb));
        lowHexField.setChangedListener(s -> onHexChanged(false));
        this.addDrawableChild(lowHexField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("选择颜色..."), btn -> {
            lowRgb = parseHexSafe(lowHexField.getText(), lowRgb);
            this.client.setScreen(new ColorPickerScreen(this, lowRgb, c -> {
                this.lowRgb = c;
                rebuild(false);
            }));
        }).dimensions(left + PREVIEW_W + 94, y + 20, 72, 20).build());

        lowResetBtn = ButtonWidget.builder(Text.literal("重置"), btn -> {
            this.lowRgb = LOW_DEFAULT;
            rebuild(false);
        }).dimensions(left + PREVIEW_W + 170, y + 20, 36, 20).build();
        this.addDrawableChild(lowResetBtn);

        y += 48;
        revBox = new CheckboxWidget(cx - 100, y + 2, 200, 20,
            Text.literal("反转方向（低功率用高颜色）"), cfg.reverse);
        this.addDrawableChild(revBox);

        int btnY = this.height - 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("保存"), btn -> doSave())
            .dimensions(cx - 105, btnY, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("完成"), btn -> { doSave(); close(); })
            .dimensions(cx + 5, btnY, 100, 20).build());

        updateResetButtons();
        validateHex(highHexField);
        validateHex(lowHexField);
    }

    private void rebuild(boolean fromFieldChange) {
        if (fromFieldChange) {
            highRgb = parseHexSafe(highHexField.getText(), highRgb);
            lowRgb  = parseHexSafe(lowHexField.getText(), lowRgb);
        }
        this.clearChildren();
        this.init();
    }

    private void onHexChanged(boolean high) {
        TextFieldWidget field = high ? highHexField : lowHexField;
        validateHex(field);
        updateResetButtons();
    }

    private void validateHex(TextFieldWidget field) {
        String text = field.getText();
        if (isValidHex6(text)) {
            field.setEditableColor(0xE0E0E0);
        } else {
            field.setEditableColor(0xFF5555);
        }
    }

    private void updateResetButtons() {
        if (highResetBtn != null) highResetBtn.active = (highRgb != HIGH_DEFAULT);
        if (lowResetBtn  != null) lowResetBtn.active  = (lowRgb  != LOW_DEFAULT);
    }

    private void doSave() {
        highRgb = parseHexSafe(highHexField.getText(), highRgb);
        lowRgb  = parseHexSafe(lowHexField.getText(), lowRgb);
        cfg.reverse = revBox.isChecked();
        float[] h = BluewireConfig.intToFloatRgb(highRgb);
        cfg.highRed = h[0]; cfg.highGreen = h[1]; cfg.highBlue = h[2];
        float[] l = BluewireConfig.intToFloatRgb(lowRgb);
        cfg.lowRed = l[0]; cfg.lowGreen = l[1]; cfg.lowBlue = l[2];
        cfg.save();
        BluewireConfig.updateColors();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.worldRenderer != null) client.worldRenderer.reload();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x99000000);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFAAAAFF);

        int cx = this.width / 2;
        int left = cx - 120;
        int y = 36;
        ctx.drawTextWithShadow(this.textRenderer, "高功率颜色 (信号强度 15)", left, y, 0xCCCCCC);
        ctx.fill(left, y + 20, left + PREVIEW_W, y + 20 + PREVIEW_H, 0xFF000000 | (highRgb & 0xFFFFFF));
        ctx.drawTextWithShadow(this.textRenderer, "#", left + PREVIEW_W + 10, y + 22 + 4, 0xAAAAAA);

        y += 54;
        ctx.drawTextWithShadow(this.textRenderer, "低功率颜色  (信号强度 0)", left, y, 0xCCCCCC);
        ctx.fill(left, y + 20, left + PREVIEW_W, y + 20 + PREVIEW_H, 0xFF000000 | (lowRgb & 0xFFFFFF));
        ctx.drawTextWithShadow(this.textRenderer, "#", left + PREVIEW_W + 10, y + 22 + 4, 0xAAAAAA);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private static TextFieldWidget hexField(int x, int y, int w, String initial) {
        var f = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, x, y, w, 18, Text.empty());
        f.setText(initial);
        f.setMaxLength(6);
        return f;
    }

    private static String toHex6(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }

    private static boolean isValidHex6(String text) {
        if (text == null || text.isEmpty()) return false;
        String hex = text.replace("#", "").replace("0x", "").trim();
        return hex.length() == 6 && hex.matches("[0-9A-Fa-f]{6}");
    }

    private static int parseHexSafe(String text, int fallback) {
        if (text == null || text.isEmpty()) return fallback;
        try {
            String hex = text.replace("#", "").replace("0x", "").trim();
            if (hex.length() != 6) return fallback;
            return (int)(Long.parseLong(hex, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}

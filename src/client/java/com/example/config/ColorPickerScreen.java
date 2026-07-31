package com.example.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {
    private static final int PICKER_X = 24, PICKER_Y = 30, PICKER_W = 256, PICKER_H = 128;

    private final Screen parent;
    private final Consumer<Integer> callback;
    private int currentRgb;

    private float hue, brightness;
    private TextFieldWidget rField, gField, bField;
    private boolean updatingFields;

    public ColorPickerScreen(Screen parent, int initialRgb, Consumer<Integer> callback) {
        super(Text.literal("选择颜色"));
        this.parent = parent;
        this.callback = callback;
        this.currentRgb = initialRgb;
        float[] hsv = rgbToHsv(((initialRgb >> 16) & 0xFF) / 255f, ((initialRgb >> 8) & 0xFF) / 255f, (initialRgb & 0xFF) / 255f);
        this.hue = hsv[0];
        this.brightness = hsv[2];
    }

    @Override
    protected void init() {
        int px = PICKER_X + PICKER_W + 20;
        int py = PICKER_Y + 10;

        this.addDrawableChild(rField = rgbField(px + 20, py, 50, (currentRgb >> 16) & 0xFF));
        this.addDrawableChild(gField = rgbField(px + 20, py + 22, 50, (currentRgb >> 8) & 0xFF));
        this.addDrawableChild(bField = rgbField(px + 20, py + 44, 50, currentRgb & 0xFF));

        rField.setChangedListener(s -> { if (!updatingFields) { readFields(); updatePicker(); } });
        gField.setChangedListener(s -> { if (!updatingFields) { readFields(); updatePicker(); } });
        bField.setChangedListener(s -> { if (!updatingFields) { readFields(); updatePicker(); } });

        int btnY = PICKER_Y + PICKER_H + 20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("确认"), btn -> {
            callback.accept(currentRgb);
            this.client.setScreen(parent);
        }).dimensions(this.width / 2 - 105, btnY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("取消"), btn -> {
            this.client.setScreen(parent);
        }).dimensions(this.width / 2 + 5, btnY, 100, 20).build());
    }

    private void readFields() {
        try {
            int r = clamp(Integer.parseInt(rField.getText()), 0, 255);
            int g = clamp(Integer.parseInt(gField.getText()), 0, 255);
            int b = clamp(Integer.parseInt(bField.getText()), 0, 255);
            currentRgb = (r << 16) | (g << 8) | b;
        } catch (NumberFormatException ignored) {}
    }

    private void updatePicker() {
        float[] hsv = rgbToHsv(
            ((currentRgb >> 16) & 0xFF) / 255f,
            ((currentRgb >> 8) & 0xFF) / 255f,
            (currentRgb & 0xFF) / 255f
        );
        this.hue = hsv[0];
        this.brightness = hsv[2];
    }

    private void updateFields() {
        updatingFields = true;
        rField.setText(Integer.toString((currentRgb >> 16) & 0xFF));
        gField.setText(Integer.toString((currentRgb >> 8) & 0xFF));
        bField.setText(Integer.toString(currentRgb & 0xFF));
        updatingFields = false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xCC000000);

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        renderGradient(ctx);

        int cursorX = PICKER_X + (int)(hue / 360f * PICKER_W);
        int cursorY = PICKER_Y + (int)((1f - brightness) * PICKER_H);
        ctx.fill(cursorX - 2, cursorY - 2, cursorX + 3, cursorY + 3, 0xFFFFFFFF);
        ctx.fill(cursorX - 1, cursorY - 1, cursorX + 2, cursorY + 2, 0xFF000000);

        int px = PICKER_X + PICKER_W + 20;
        int py = PICKER_Y + 370;
        ctx.drawTextWithShadow(this.textRenderer, "当前", px, PICKER_Y, 0xAAAAAA);
        ctx.fill(px, PICKER_Y + 12, px + 40, PICKER_Y + 32, 0xFF000000 | currentRgb);

        ctx.drawTextWithShadow(this.textRenderer, "R", px, py + 4, 0xFF5555);
        ctx.drawTextWithShadow(this.textRenderer, "G", px, py + 26, 0x55FF55);
        ctx.drawTextWithShadow(this.textRenderer, "B", px, py + 48, 0x5555FF);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (mouseX >= PICKER_X && mouseX < PICKER_X + PICKER_W &&
            mouseY >= PICKER_Y && mouseY < PICKER_Y + PICKER_H) {
            pickColor((float)mouseX, (float)mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        if (mouseX >= PICKER_X && mouseX < PICKER_X + PICKER_W &&
            mouseY >= PICKER_Y && mouseY < PICKER_Y + PICKER_H) {
            pickColor((float)mouseX, (float)mouseY);
            return true;
        }
        return false;
    }

    private void pickColor(float mx, float my) {
        hue = MathHelper.clamp((mx - PICKER_X) / PICKER_W * 360f, 0f, 360f);
        brightness = MathHelper.clamp(1f - (my - PICKER_Y) / PICKER_H, 0f, 1f);
        currentRgb = hsvToRgb(hue, 1f, brightness);
        updateFields();
    }

    private void renderGradient(DrawContext ctx) {
        int cols = 32;
        int rows = 16;
        int cellW = PICKER_W / cols;
        int cellH = PICKER_H / rows;
        for (int ci = 0; ci < cols; ci++) {
            float h = ci / (float)cols * 360f;
            int x0 = PICKER_X + ci * cellW;
            int x1 = ci == cols - 1 ? PICKER_X + PICKER_W : x0 + cellW;
            for (int ri = 0; ri < rows; ri++) {
                float v = 1f - ri / (float)rows;
                int y0 = PICKER_Y + ri * cellH;
                int y1 = ri == rows - 1 ? PICKER_Y + PICKER_H : y0 + cellH;
                int rgb = hsvToRgb(h, 1f, v) | 0xFF000000;
                ctx.fill(x0, y0, x1, y1, rgb);
            }
        }
    }

    private static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float hp = h / 60f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));
        float m = v - c;
        float r, g, b;
        if      (hp < 1) { r = c; g = x; b = 0; }
        else if (hp < 2) { r = x; g = c; b = 0; }
        else if (hp < 3) { r = 0; g = c; b = x; }
        else if (hp < 4) { r = 0; g = x; b = c; }
        else if (hp < 5) { r = x; g = 0; b = c; }
        else             { r = c; g = 0; b = x; }
        return MathHelper.packRgb(r + m, g + m, b + m);
    }

    private static float[] rgbToHsv(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d > 0.0001f) {
            if (max == r)      h = ((g - b) / d) % 6f;
            else if (max == g) h = (b - r) / d + 2f;
            else               h = (r - g) / d + 4f;
        }
        h = (h * 60f + 360f) % 360f;
        float s = max < 0.0001f ? 0 : d / max;
        return new float[]{h, s, max};
    }

    private static TextFieldWidget rgbField(int x, int y, int w, int v) {
        var f = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, x, y, w, 18, Text.empty());
        f.setText(Integer.toString(v));
        f.setMaxLength(3);
        return f;
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : Math.min(v, hi); }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}

package com.example.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class BluewireConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bluewire-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
        FabricLoader.getInstance().getConfigDir().toFile(), "bluewire.json"
    );
    private static BluewireConfig instance;

    public float highRed   = 0.0F;
    public float highGreen = 0.5F;
    public float highBlue  = 1.0F;
    public float lowRed    = 0.0F;
    public float lowGreen  = 0.0F;
    public float lowBlue   = 0.3F;
    public boolean reverse = false;
    public boolean rainbow = false;
    public float rainbowSpeed = 1.0F;

    private static final Vec3d[] COLORS = new Vec3d[16];
    static { updateColors(); }

    public static void updateColors() {
        BluewireConfig config = getInstance();
        for (int i = 0; i <= 15; ++i) {
            float f = config.reverse ? 1.0F - (float) i / 15.0F : (float) i / 15.0F;
            float r = MathHelper.clamp(config.lowRed   + (config.highRed   - config.lowRed)   * f, 0.0F, 1.0F);
            float g = MathHelper.clamp(config.lowGreen + (config.highGreen - config.lowGreen) * f, 0.0F, 1.0F);
            float b = MathHelper.clamp(config.lowBlue  + (config.highBlue  - config.lowBlue)  * f, 0.0F, 1.0F);
            COLORS[i] = new Vec3d(r, g, b);
        }
    }

    public static int getWireColor(int powerLevel) {
        BluewireConfig config = getInstance();
        if (config.rainbow) {
            float speed = config.rainbowSpeed <= 0 ? 1.0F : config.rainbowSpeed;
            float hue = (System.currentTimeMillis() / 1000.0F * speed * 360.0F) % 360.0F;
            hue = (hue + powerLevel * 12.0F) % 360.0F;
            return hsvToRgb(hue, 1.0F, 1.0F);
        }
        Vec3d v = COLORS[powerLevel];
        return MathHelper.packRgb((float)v.getX(), (float)v.getY(), (float)v.getZ());
    }

    private static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float hp = h / 60.0F;
        float x = c * (1.0F - Math.abs(hp % 2.0F - 1.0F));
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

    public static BluewireConfig getInstance() { if (instance == null) instance = new BluewireConfig(); return instance; }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (Reader r = new FileReader(CONFIG_FILE)) { instance = GSON.fromJson(r, BluewireConfig.class); }
            catch (Exception e) { LOGGER.error("配置加载失败，使用默认值", e); instance = new BluewireConfig(); }
        } else { instance = new BluewireConfig(); instance.save(); }
        if (instance.rainbowSpeed <= 0) instance.rainbowSpeed = 1.0F;
    }

    public void save() {
        try (Writer w = new FileWriter(CONFIG_FILE)) { GSON.toJson(this, w); }
        catch (Exception e) { LOGGER.error("配置保存失败", e); }
    }

    public static int floatRgbToInt(float r, float g, float b) {
        return (MathHelper.clamp((int)(r*255+0.5f),0,255)<<16)|(MathHelper.clamp((int)(g*255+0.5f),0,255)<<8)|MathHelper.clamp((int)(b*255+0.5f),0,255);
    }

    public static float[] intToFloatRgb(int c) {
        return new float[]{((c>>16)&0xFF)/255f, ((c>>8)&0xFF)/255f, (c&0xFF)/255f};
    }
}

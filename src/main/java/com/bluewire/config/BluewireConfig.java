package com.bluewire.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
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
    private static final ThreadLocal<Integer> DYNAMIC_COLOR_OVERRIDE = new ThreadLocal<>();

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
        Integer override = DYNAMIC_COLOR_OVERRIDE.get();
        if (override != null) return override;

        BluewireConfig config = getInstance();
        if (config.rainbow) {
            return 0x888888;
        }
        Vec3d v = COLORS[powerLevel];
        return MathHelper.packRgb((float)v.getX(), (float)v.getY(), (float)v.getZ());
    }

    public int getDynamicWireColor(BlockState state, BlockPos pos, double elapsedSeconds) {
        int power = state.get(RedstoneWireBlock.POWER);
        float speed = rainbowSpeed <= 0.0F ? 1.0F : rainbowSpeed;
        double phase = elapsedSeconds * speed
            + (pos.getX() + pos.getZ()) * 0.025
            + pos.getY() * 0.04
            + power / 16.0;
        float hue = (float)(phase - Math.floor(phase));
        return MathHelper.hsvToRgb(hue, 1.0F, 1.0F);
    }

    public static void setDynamicColorOverride(int color) {
        DYNAMIC_COLOR_OVERRIDE.set(color);
    }

    public static void clearDynamicColorOverride() {
        DYNAMIC_COLOR_OVERRIDE.remove();
    }

    public static BluewireConfig getInstance() {
        if (instance == null) instance = new BluewireConfig();
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (Reader r = new FileReader(CONFIG_FILE)) { instance = GSON.fromJson(r, BluewireConfig.class); }
            catch (Exception e) { LOGGER.error("配置加载失败", e); instance = new BluewireConfig(); }
        } else { instance = new BluewireConfig(); instance.save(); }
        if (instance.rainbowSpeed <= 0) instance.rainbowSpeed = 1.0F;
    }

    public void save() {
        try (Writer w = new FileWriter(CONFIG_FILE)) { GSON.toJson(this, w); }
        catch (Exception e) { LOGGER.error("配置保存失败", e); }
    }

    public static int floatRgbToInt(float r, float g, float b) {
        return (MathHelper.clamp((int)(r*255+0.5f),0,255)<<16)
             | (MathHelper.clamp((int)(g*255+0.5f),0,255)<<8)
             |  MathHelper.clamp((int)(b*255+0.5f),0,255);
    }

    public static float[] intToFloatRgb(int c) {
        return new float[]{((c>>16)&0xFF)/255f, ((c>>8)&0xFF)/255f, (c&0xFF)/255f};
    }
}

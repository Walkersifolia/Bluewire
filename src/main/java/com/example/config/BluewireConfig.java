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

    // Default colour values — current blue parameters:
    // At power 15: R=0.0, G=0.5, B=1.0
    // At power  0: R=0.0, G=0.0, B=0.3
    public float highRed   = 0.0F;
    public float highGreen = 0.5F;
    public float highBlue  = 1.0F;
    public float lowRed    = 0.0F;
    public float lowGreen  = 0.0F;
    public float lowBlue   = 0.3F;

    /** When true the brightest colour appears at power 0 instead of power 15. */
    public boolean reverse = false;

    // ---- colour cache ----

    private static final Vec3d[] COLORS = new Vec3d[16];

    static {
        updateColors();
    }

    /**
     * Rebuild the cached colour table from the current config values.
     * Called on mod init and every time the user saves the config GUI.
     */
    public static void updateColors() {
        BluewireConfig config = getInstance();
        for (int i = 0; i <= 15; ++i) {
            float f = (float) i / 15.0F;

            // When reversed, power 0 is brightest and power 15 is darkest.
            if (config.reverse) f = 1.0F - f;

            float r = MathHelper.clamp(
                config.lowRed   + (config.highRed   - config.lowRed)   * f, 0.0F, 1.0F);
            float g = MathHelper.clamp(
                config.lowGreen + (config.highGreen - config.lowGreen) * f, 0.0F, 1.0F);
            float b = MathHelper.clamp(
                config.lowBlue  + (config.highBlue  - config.lowBlue)  * f, 0.0F, 1.0F);

            COLORS[i] = new Vec3d((double) r, (double) g, (double) b);
        }
    }

    public static int getWireColor(int powerLevel) {
        Vec3d vec3d = COLORS[powerLevel];
        return MathHelper.packRgb(
            (float) vec3d.getX(), (float) vec3d.getY(), (float) vec3d.getZ());
    }

    // ---- singleton ----

    public static BluewireConfig getInstance() {
        if (instance == null) {
            instance = new BluewireConfig();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (Reader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, BluewireConfig.class);
                LOGGER.info("Config loaded from {}", CONFIG_FILE.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("Failed to load config, using defaults", e);
                instance = new BluewireConfig();
            }
        } else {
            instance = new BluewireConfig();
            instance.save();
            LOGGER.info("Default config saved to {}", CONFIG_FILE.getAbsolutePath());
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    // ---- utility (used by the client-side config screen) ----

    public static int floatRgbToInt(float r, float g, float b) {
        int ir = MathHelper.clamp((int) (r * 255.0F + 0.5F), 0, 255);
        int ig = MathHelper.clamp((int) (g * 255.0F + 0.5F), 0, 255);
        int ib = MathHelper.clamp((int) (b * 255.0F + 0.5F), 0, 255);
        return (ir << 16) | (ig << 8) | ib;
    }

    public static float[] intToFloatRgb(int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8)  & 0xFF) / 255.0F;
        float b =  (color        & 0xFF) / 255.0F;
        return new float[] { r, g, b };
    }
}
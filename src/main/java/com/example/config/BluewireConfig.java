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

    // 默认颜色值 — 当前蓝色配色参数：
    // 功率 15 时: R=0.0, G=0.5, B=1.0
    // 功率  0 时: R=0.0, G=0.0, B=0.3
    public float highRed   = 0.0F;
    public float highGreen = 0.5F;
    public float highBlue  = 1.0F;
    public float lowRed    = 0.0F;
    public float lowGreen  = 0.0F;
    public float lowBlue   = 0.3F;

    /** 开启后最亮颜色出现在功率 0 而非功率 15 */
    public boolean reverse = false;

    // ---- 颜色缓存 ----

    private static final Vec3d[] COLORS = new Vec3d[16];

    static {
        updateColors();
    }

    /**
     * 根据当前配置重建缓存的颜色表。
     * 模组初始化时调用，配置页面保存时也会调用。
     */
    public static void updateColors() {
        BluewireConfig config = getInstance();
        for (int i = 0; i <= 15; ++i) {
            float f = (float) i / 15.0F;

            // 反转模式：功率 0 最亮，功率 15 最暗
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

    // ---- 单例 ----

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
                LOGGER.info("已从 {} 加载配置", CONFIG_FILE.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("配置加载失败，使用默认值", e);
                instance = new BluewireConfig();
            }
        } else {
            instance = new BluewireConfig();
            instance.save();
            LOGGER.info("默认配置已保存至 {}", CONFIG_FILE.getAbsolutePath());
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            LOGGER.error("配置保存失败", e);
        }
    }

    // ---- 工具方法（供客户端配置页面使用） ----

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

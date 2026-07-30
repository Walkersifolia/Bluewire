package com.example.mixin;

import com.example.config.BluewireConfig;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.block.RedstoneWireBlock;

@Mixin(RedstoneWireBlock.class)
public class ExampleMixin {
    private static final Vec3d[] COLORS = new Vec3d[16];

    static {
        updateColors();
    }

    /**
     * Rebuild the cached colour table from the current config values.
     * Called on mod init and every time the user saves the config GUI.
     */
    public static void updateColors() {
        BluewireConfig config = BluewireConfig.getInstance();
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

    /**
     * @param powerLevel redstone signal strength (0-15)
     * @return packed RGB colour for the wire at this power level
     * @author WalkerTian
     * @reason Replace vanilla red colour with a configurable hue
     */
    @Overwrite
    public static int getWireColor(int powerLevel) {
        Vec3d vec3d = COLORS[powerLevel];
        return MathHelper.packRgb(
            (float) vec3d.getX(), (float) vec3d.getY(), (float) vec3d.getZ());
    }
}
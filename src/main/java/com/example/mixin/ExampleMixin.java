package com.example.mixin;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.block.RedstoneWireBlock;

@Mixin(RedstoneWireBlock.class)
public class ExampleMixin {
    private static final Vec3d[] COLORS = new Vec3d[16];

    static {
        for (int i = 0; i <= 15; ++i) {
            float f = (float) i / 15.0F;
            float g = f * 0.6F + (f > 0.0F ? 0.4F : 0.3F);
            float h = MathHelper.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
            float j = MathHelper.clamp(f * f * 0.6F - 0.7F, 0.0F, 1.0F);
            COLORS[i] = new Vec3d((double) j, (double) h, (double) g);
        }
    }

    /**
     *
     * @param powerLevel
     * @return
     * @author
     * @reason
     */
    @Overwrite
    public static int getWireColor(int powerLevel) {
        Vec3d vec3d = COLORS[powerLevel];
        return MathHelper.packRgb((float) vec3d.getX(), (float) vec3d.getY(), (float) vec3d.getZ());
    }
}
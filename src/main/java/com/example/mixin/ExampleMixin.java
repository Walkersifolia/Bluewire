package com.example.mixin;

import com.example.config.BluewireConfig;
import net.minecraft.block.RedstoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(RedstoneWireBlock.class)
public class ExampleMixin {
    @Overwrite
    public static int getWireColor(int powerLevel) {
        return BluewireConfig.getWireColor(powerLevel);
    }
}

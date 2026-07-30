package com.example.mixin;

import com.example.config.BluewireConfig;
import net.minecraft.block.RedstoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(RedstoneWireBlock.class)
public class ExampleMixin {

    /**
     * @param powerLevel redstone signal strength (0-15)
     * @return packed RGB colour for the wire at this power level
     * @author WalkerTian
     * @reason Replace vanilla red colour with a configurable hue
     */
    @Overwrite
    public static int getWireColor(int powerLevel) {
        return BluewireConfig.getWireColor(powerLevel);
    }
}

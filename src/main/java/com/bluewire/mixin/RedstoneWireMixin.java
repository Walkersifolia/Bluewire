package com.bluewire.mixin;

import com.bluewire.config.BluewireConfig;
import net.minecraft.block.RedstoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireMixin {
    /**
     * @param powerLevel 红石信号强度 (0-15)
     * @return 该功率等级对应的 RGB 颜色值
     * @author WalkerTian
     * @reason 用可配置的颜色替换原版红色
     */
    @Overwrite
    public static int getWireColor(int powerLevel) {
        return BluewireConfig.getWireColor(powerLevel);
    }
}

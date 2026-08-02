package com.bluewire.mixin;

import com.bluewire.config.DynamicWireRenderer_argb;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin_argb {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void bluewire$trackRedstoneWire(
            BlockPos pos,
            BlockState state,
            int flags,
            int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            DynamicWireRenderer_argb.onBlockStateChanged(pos, state);
        }
    }
}

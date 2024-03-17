package com.mattmx.autotrade.mixin.client;

import com.mattmx.autotrade.client.AutoTradeClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mattmx.autotrade.ClientUtil.getAddress;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(
            method = "onDisconnected",
            at = @At("HEAD")
    )
    private void onDisconnected(CallbackInfo ci) {
        getAddress().ifPresent(AutoTradeClient.INSTANCE::saveSavedTrades);
    }


}

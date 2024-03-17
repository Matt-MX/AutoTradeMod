package com.mattmx.autotrade.mixin.client;

import com.mattmx.autotrade.client.AutoTradeClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ServerMetadataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mattmx.autotrade.ClientUtil.getAddress;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(
            method = "onServerMetadata",
            at = @At("TAIL")
    )
    private void onServerMetadata(ServerMetadataS2CPacket packet, CallbackInfo ci) {
        getAddress().ifPresent(AutoTradeClient.INSTANCE::loadSavedTrades);
    }

}

package com.mattmx.autotrade;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;

import java.util.Optional;

public class ClientUtil {

    public static Optional<String> getAddress() {
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return Optional.empty();
        ServerInfo info = handler.getServerInfo();
        if (info == null) return Optional.empty();

        return Optional.ofNullable(info.address);
    }

}

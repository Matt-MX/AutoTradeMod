package com.mattmx.autotrade

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil

class KeyBindWrapper(
    val name: String,
    val default: Int = InputUtil.UNKNOWN_KEY.code,
    val type: InputUtil.Type = InputUtil.Type.KEYSYM,
) {
    val binding = KeyBinding(
        "key.auto-trade.$name",
        type,
        default,
        "key.auto-trade.category"
    )

    fun register() = apply {
        KeyBindingHelper.registerKeyBinding(binding)
    }
}
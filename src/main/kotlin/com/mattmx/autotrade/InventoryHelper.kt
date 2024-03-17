package com.mattmx.autotrade

import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack


object InventoryHelper {
    fun hasSpace(inv: PlayerInventory, outStack: ItemStack): Boolean {
        return outStack.isEmpty || inv.emptySlot >= 0 || inv.getOccupiedSlotWithRoomForStack(outStack) >= 0
    }
}
package com.mattmx.autotrade.client

import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
import com.mattmx.autotrade.KeyBindWrapper
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.command.argument.EntityAnchorArgumentType
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.math.Box
import java.io.File
import java.time.Duration
import java.util.*
import kotlin.io.path.pathString

private val gson = GsonBuilder()
    .setPrettyPrinting()
    .enableComplexMapKeySerialization()
    .create()

private val storage = File(FabricLoader.getInstance().configDir.pathString + "/auto-trade/")
    .apply { mkdirs() }

fun init() {
    AutoTradeClient
}

object AutoTradeClient {
    var lastVillager: VillagerEntity? = null
    private val tasks = Collections.synchronizedList(arrayListOf<() -> Boolean>())
    private val tradeAuraRecent = Collections.synchronizedList(arrayListOf<UUID>())
    val savedTrades: MutableMap<String, Int> = Collections.synchronizedMap(hashMapOf<String, Int>())

    val bindAutoTradeKey = KeyBindWrapper("bind-auto-trade")
    val quickTradeKey = KeyBindWrapper("quick-trade")
    val tradeAuraKey = KeyBindWrapper("trade-aura")

    init {

        bindAutoTradeKey.register()
        quickTradeKey.register()
        tradeAuraKey.register()

        var lastTradeAuraAttempt = 0L
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvents.StartTick { client: MinecraftClient ->
            for (task in tasks.toList()) {
                if (!task.invoke()) {
                    tasks.remove(task)
                }
            }

            // Check if key is held
            if (tradeAuraKey.binding.isPressed && Screen.hasControlDown()) {
                val player = client.player ?: return@StartTick
                val camera = client.cameraEntity ?: return@StartTick

                player.sendMessage(Text.of("Trade-Aura is active!"), true)

                // trade-aura cooldown
                val timeSinceLastAuraAttempt = System.currentTimeMillis() - lastTradeAuraAttempt
                if (timeSinceLastAuraAttempt < 100) {
                    return@StartTick
                }

                val box = Box.from(player.pos).expand(4.0)

                val nearbyVillagers = player
                    .world
                    .getEntitiesByClass(VillagerEntity::class.java, box) { villager ->
                        camera.isInRange(villager, 3.0) && !tradeAuraRecent.contains(villager.uuid)
                    }

                if (nearbyVillagers.isNotEmpty()) {
                    val villager = nearbyVillagers.first()
                    tradeAuraRecent.add(villager.uuid)

                    lastTradeAuraAttempt = System.currentTimeMillis()
                    villager.interactMob(player, Hand.MAIN_HAND)
                    player.networkHandler.sendPacket(
                        PlayerInteractEntityC2SPacket.interact(
                            villager,
                            false,
                            Hand.MAIN_HAND
                        )
                    )

                    // todo replace this with scheduler
                    val timeInteracted = System.currentTimeMillis()
                    runUntilFalse {
                        val timeSinceInteracted = System.currentTimeMillis() - timeInteracted

                        if (timeSinceInteracted >= Duration.ofSeconds(4).toMillis()) {
                            // Break look and remove
                            tradeAuraRecent.remove(villager.uuid)
                            return@runUntilFalse false
                        }

                        true
                    }
                }
            }
        })
        MinecraftClient.getInstance().player
    }

    fun runUntilFalse(task: () -> Boolean) {
        tasks.add(task)
    }

    fun saveTrade(entityId: Optional<UUID>, tradeSlot: Int) =
        entityId.ifPresent { saveTrade(it, tradeSlot) }

    fun saveTrade(entityId: UUID, tradeSlot: Int) {
        savedTrades[entityId.toString()] = tradeSlot
    }

    fun getSavedTrade(entityId: UUID) = Optional.ofNullable(savedTrades[entityId.toString()])

    fun saveSavedTrades(serverIp: String) {
        val file = File("$storage/$serverIp")

        if (!file.exists()) file.createNewFile()
        file.writeText(gson.toJson(this.savedTrades))

        savedTrades.clear()
    }

    fun loadSavedTrades(serverIp: String) {
        val file = File("$storage/$serverIp")

        if (file.exists()) {
            val trades =
                gson.fromJson<HashMap<String, Int>>(file.readText(), object : TypeToken<HashMap<String, Int>>() {}.type)
            this.savedTrades.putAll(trades)
        }
    }
}
@file:Suppress("UnusedImport", "LocalVariableName", "UnusedVariable")
package com.lualngcon

import com.lualngcon.libraries.LuaPlayerBridge
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue

object LuaEventBridge {
    private val joinCallBacks = ArrayList<LuaFunction>()
    private val handheldCallBacks = ArrayList<LuaFunction>()
    private val groundCallBacks = ArrayList<LuaFunction>()
    private val interactionCallBacks = ArrayList<LuaFunction>()

    private val OnFinishedUseCallbacks = ArrayList<LuaFunction>()

    fun registerEventsApi(minecraftTable: LuaValue) {
        val event_table = LuaValue.tableOf()
        val on_table = LuaValue.tableOf()
        val metatable = LuaValue.tableOf()

        metatable.set("__newindex", object : org.luaj.vm2.lib.ThreeArgFunction() {
            override fun call(table: LuaValue, key: LuaValue, value: LuaValue): LuaValue {
                if (key.isstring() && value.isfunction()) {
                    val event_name = key.tojstring()
                    val callback = value.checkfunction()

                    when (event_name) {
                        "join" -> joinCallBacks.add(callback)
                        "handheld" -> handheldCallBacks.add(callback)
                        "ground" -> groundCallBacks.add(callback)
                        "interaction" -> interactionCallBacks.add(callback)
                        "finished_use" -> OnFinishedUseCallbacks.add(callback)
                        else -> println("[LuaLangConnector]: [Events-API]: [Warning]: Unknown event 'on.$event_name' ._.")
                    }
                }
                return LuaValue.NIL
            }
        })

        on_table.setmetatable(metatable)
        event_table.set("on", on_table)
        minecraftTable.set("events", event_table)
        initFabricListeners()
    }


    private fun initFabricListeners() {
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            val wrapped_player = LuaPlayerBridge.wrapPlayer(player)
            joinCallBacks.forEach { it.call(wrapped_player) }
        }

        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerList.players) {
                val wrapped_player by lazy { LuaPlayerBridge.wrapPlayer(player) }

                if (handheldCallBacks.isNotEmpty()) {
                    val heldStack = player.mainHandItem
                    if (!heldStack.isEmpty) {
                        val itemId = BuiltInRegistries.ITEM.getKey(heldStack.item).toString()
                        val luaItemId = LuaValue.valueOf(itemId)
                        handheldCallBacks.forEach { it.call(wrapped_player, luaItemId) }
                    }
                }

                if (groundCallBacks.isNotEmpty()) {
                    val below_pos = player.blockPosition().below()
                    val blockstate = player.level().getBlockState(below_pos)

                    if (!blockstate.isAir) {
                        val blockId = BuiltInRegistries.BLOCK.getKey(blockstate.block).toString()
                        val luaBlockId = LuaValue.valueOf(blockId)
                        groundCallBacks.forEach { it.call(wrapped_player, luaBlockId) }
                    }
                }
            }
        }

        // Trigger on_interaction (Right-click in air with item)
        UseItemCallback.EVENT.register { player, world, hand ->
            if (!world.isClientSide && interactionCallBacks.isNotEmpty()) {
                val stack = player.getItemInHand(hand)
                val itemId =
                    if (!stack.isEmpty) BuiltInRegistries.ITEM.getKey(stack.item).toString() else "minecraft:air"
                val wrapped_player = LuaPlayerBridge.wrapPlayer(player)

                interactionCallBacks.forEach {
                    it.call(wrapped_player, LuaValue.valueOf(itemId), LuaValue.NIL)
                }
            }
            InteractionResultHolder.pass(player.getItemInHand(hand))
        }

        // Trigger on_interaction (Right-click on a block)
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            if (!world.isClientSide && interactionCallBacks.isNotEmpty()) {
                val stack = player.getItemInHand(hand)
                val itemId =
                    if (!stack.isEmpty) BuiltInRegistries.ITEM.getKey(stack.item).toString() else "minecraft:air"
                val blockState = world.getBlockState(hitResult.blockPos)
                val blockId = BuiltInRegistries.BLOCK.getKey(blockState.block).toString()
                val wrapped_player = LuaPlayerBridge.wrapPlayer(player)

                interactionCallBacks.forEach {
                    it.call(wrapped_player, LuaValue.valueOf(itemId), LuaValue.valueOf(blockId))
                }
            }
            InteractionResult.PASS
        }
    }

        fun triggerOnFinishedUse(player: net.minecraft.world.entity.player.Player,  stack: net.minecraft.world.item.ItemStack) {
            if (OnFinishedUseCallbacks.isEmpty()) {
                return
            }

            val item_id = BuiltInRegistries.ITEM.getKey(stack.item).toString()
            val wrapped_player = LuaPlayerBridge.wrapPlayer(player)

            OnFinishedUseCallbacks.forEach {
                it.call(wrapped_player, LuaValue.valueOf(item_id))
            }
    }
}
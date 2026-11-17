@file:Suppress("unused", "LocalVariableName")
package com.lualngcon.libraries

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import java.util.Optional
object LuaPlayerBridge {

      // ================================== //
     // This is the player library ^^      //
    // ================================== //
    fun wrapPlayer(player: Player): LuaTable {
        val player_table = LuaValue.tableOf()

            // ============================================================ //
           // Here, we extract the name and hearts available of the player //
          // ============================================================ //
        player_table.set("name", LuaValue.valueOf(player.name.string))
        player_table.set("hearts", LuaValue.valueOf((player.health / 2.0f).toDouble()))
          player_table.set("_raw", LuaValue.userdataOf(player))

        // 2. Health Manipulation Method: player.setHearts(amount)
        player_table.set("set_hearts", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                if (!player.level().isClientSide && arg.isnumber()) {
                    val targetHealth = arg.tofloat() * 2.0f
                    player.health = targetHealth
                }
                return NIL
            }
        })

        // ================================================================ //
        // Sub-Library: player.inventory                                    //
        // ================================================================ //
        val inventoryTable = LuaValue.tableOf()

        // player.inventory.add("minecraft:diamond", 5)
        inventoryTable.set("add", object : TwoArgFunction() {
            override fun call(itemArg: LuaValue, countArg: LuaValue): LuaValue {
                if (!player.level().isClientSide && itemArg.isstring()) {
                    val itemId = itemArg.tojstring()
                    val count = if (countArg.isint()) countArg.toint() else 1
                    val loc = ResourceLocation.tryParse(itemId)

                    if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                        val item = BuiltInRegistries.ITEM.get(loc)
                        val stack = ItemStack(item, count)
                        player.inventory.add(stack)
                    } else {
                        println("[LuaLangConnector]: [Player-API]: [Warning]: Could not find item '$itemId' to add ._.")
                    }
                }
                @Suppress("RemoveRedundantQualifierName")
                return NIL
            }
        })

        // player.inventory.elim("minecraft:dirt", 10)
        inventoryTable.set("elim", object : TwoArgFunction() {
            override fun call(itemArg: LuaValue, countArg: LuaValue): LuaValue {
                if (!player.level().isClientSide && itemArg.isstring()) {
                    val itemId = itemArg.tojstring()
                    var toRemove = if (countArg.isint()) countArg.toint() else 1
                    val loc = ResourceLocation.tryParse(itemId)

                    if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                        val targetItem = BuiltInRegistries.ITEM.get(loc)
                        val inv = player.inventory

                        for (slot in 0 until inv.containerSize) {
                            if (toRemove <= 0) break
                            val stack = inv.getItem(slot)
                            if (!stack.isEmpty && stack.item == targetItem) {
                                val removed = minOf(stack.count, toRemove)
                                stack.shrink(removed)
                                toRemove -= removed
                            }
                        }
                    }
                }
                @Suppress("RemoveRedundantQualifierName")
                return NIL
            }
        })

          inventoryTable.set("has", object : OneArgFunction() {
              override fun call(itemArg: LuaValue): LuaValue {
                  if (itemArg.isstring()) {
                      val itemID = itemArg.tojstring()
                      val location = ResourceLocation.tryParse(itemID)

                      if (location != null && BuiltInRegistries.ITEM.containsKey(location)) {
                          val targetItem = BuiltInRegistries.ITEM.get(location)
                          val inventory = player.inventory

                          for (slot in 0 until inventory.containerSize) {
                              val stack = inventory.getItem(slot)
                              if (!stack.isEmpty && stack.item == targetItem) {
                                  return TRUE
                              }
                          }
                      }
                  }
                  return FALSE
              }
          })

          player_table.set("inventory", inventoryTable)

          val effectTable = LuaValue.tableOf()

          // player.effect.grant("minecraft:speed", duration, amplifier)
          effectTable.set("grant", object : VarArgFunction() {
              override fun invoke(args: Varargs): Varargs {
                  if (!player.level().isClientSide && args.narg() >= 1) {
                      val effectId = args.checkjstring(1)
                      val duration = if (args.narg() >= 2 && args.isnumber(2)) args.toint(2) else 200
                      val amplifier = if (args.narg() >= 3 && args.isnumber(3)) args.toint(3) else 0

                      val loc = ResourceLocation.tryParse(effectId)
                      if (loc != null) {
                          val registry = BuiltInRegistries.MOB_EFFECT
                          val key = ResourceKey.create(registry.key(), loc)
                          val entry: Optional<Holder.Reference<MobEffect>> = registry.getHolder(key)

                          if (entry.isPresent) {
                              player.addEffect(MobEffectInstance(entry.get(), duration, amplifier))
                          } else {
                              println("[LuaLangConnector]: [Player-API]: [Warning]: Magic effect '$effectId' not found ._.")
                          }
                      }
                  }
                  return NIL
              }
          })

          // player.effect.sub("minecraft:speed")
          effectTable.set("sub", object : OneArgFunction() {
              override fun call(effectArg: LuaValue): LuaValue {
                  if (!player.level().isClientSide && effectArg.isstring()) {
                      val effectId = effectArg.tojstring()
                      val loc = ResourceLocation.tryParse(effectId)

                      if (loc != null) {
                          val registry = BuiltInRegistries.MOB_EFFECT
                          val key = ResourceKey.create(registry.key(), loc)
                          val entry: Optional<Holder.Reference<MobEffect>> = registry.getHolder(key)

                          if (entry.isPresent) {
                              player.removeEffect(entry.get())
                          }
                      }
                  }
                  return NIL
              }
          })

          player_table.set("effect", effectTable)
        return player_table
    }
}

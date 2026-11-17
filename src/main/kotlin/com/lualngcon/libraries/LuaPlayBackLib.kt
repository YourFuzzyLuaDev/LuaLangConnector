@file:Suppress("unused", "RemoveRedundantQualifierName", "LocalVariableName")

package com.lualngcon.libraries

import net.minecraft.world.entity.player.Player
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

object LuaPlayBackLib {
    private fun getHeldGeoItem(player_table: LuaValue): LuaGeoLib? {
        val raw_user_data = player_table.get("_raw")
        if (raw_user_data.isuserdata(Player::class.java)) {
            val player = raw_user_data.touserdata(Player::class.java) as Player
            val held_item = player.mainHandItem.item
            if (held_item is LuaGeoLib) {
                return held_item
            }
        }
        return null
    }

    fun registerPlaybackLibraries(globals: LuaValue) {
        val play_table = LuaValue.tableOf()
        val switch_table = LuaValue.tableOf()

        play_table.set("animation", object : TwoArgFunction() {
            override fun call(playerarg: LuaValue, animationarg: LuaValue): LuaValue {
                val geckoItem = getHeldGeoItem(playerarg)
                if (geckoItem != null && animationarg.isstring()) {
                    val animation_name = animationarg.tojstring().substringAfterLast(":")
                    geckoItem.current_animation = animation_name
                    geckoItem.is_paused = false
                }
                return LuaValue.NIL
            }
        })

        switch_table.set("animation", object : TwoArgFunction() {
            override fun call(playerarg: LuaValue, animArg: LuaValue): LuaValue {
                val geckoItem = getHeldGeoItem(playerarg)
                if (geckoItem != null && animArg.isstring()) {
                    geckoItem.current_animation = animArg.tojstring().substringAfterLast(":")
                    geckoItem.is_paused = false
                }
                return LuaValue.NIL
            }
        })

        switch_table.set("texture", object : TwoArgFunction() {
            override fun call(playerarg: LuaValue, texArg: LuaValue): LuaValue {
                val geckoItem = getHeldGeoItem(playerarg)
                if (geckoItem != null && texArg.isstring()) {
                    geckoItem.texture_path = texArg.tojstring()
                }
                return LuaValue.NIL
            }
        })

        play_table.set("switch", switch_table)

        val pause_table = LuaValue.tableOf()

        pause_table.set("animation", object : OneArgFunction() {
            override fun call(playerarg: LuaValue): LuaValue {
                val geckoItem = getHeldGeoItem(playerarg)
                if (geckoItem != null) {
                    geckoItem.is_paused = true
                }
                return LuaValue.NIL
            }
        })

        pause_table.set("texture", object : OneArgFunction() {
            override fun call(playerarg: LuaValue): LuaValue {
                val geckoItem = getHeldGeoItem(playerarg)
                if (geckoItem != null) {
                    geckoItem.is_paused = true
                }
                return LuaValue.NIL
            }
        })

        globals.set("play", play_table)
        globals.set("pause", pause_table)
    }
}
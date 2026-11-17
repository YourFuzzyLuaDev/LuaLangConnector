@file:Suppress("NonJavaMixin")
package com.lualngcon.mixin

import com.lualngcon.LuaEventBridge
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(LivingEntity::class)
abstract class OnFinishedUse {
    @Inject(method = ["finishUsingItem"], at = [At("RETURN")])
    private fun onFinishedUse(cir: CallbackInfoReturnable<ItemStack>) {
        @Suppress("KotlinConstantConditions")
        val entity = this as Any as LivingEntity
        if (entity is Player && !entity.level().isClientSide) {
            val consumedStack = entity.useItem
            if (!consumedStack.isEmpty) {
                LuaEventBridge.triggerOnFinishedUse(entity, consumedStack)
            }
        }
    }
}
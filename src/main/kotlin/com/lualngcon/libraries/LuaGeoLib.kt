@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "INACCESSIBLE_TYPE", "MISSING_DEPENDENCY_SUPERCLASS_WARNING",
    "PropertyName"
)
package com.lualngcon.libraries

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoItemRenderer
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class LuaGeoLib(
    properties: Properties,
    var model_path: String,
    var texture_path: String,
    var animation_path: String,
    var default_animation: String,
    var current_animation: String,
    var is_paused: Boolean = false
) : Item(properties), GeoItem {

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    init {
        // Registers this item for server-to-client animation syncing
        SingletonGeoAnimatable.registerSyncedAnimatable(this)
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(AnimationController(this, "main_controller", 0) { state ->
            if (is_paused) {
                return@AnimationController PlayState.STOP
            }
            if (current_animation.isNotEmpty()) {
                return@AnimationController state.setAndContinue(RawAnimation.begin().thenLoop(current_animation))
            }
            PlayState.STOP
        })
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
        consumer.accept(object : GeoRenderProvider {
            private var renderer: GeoItemRenderer<LuaGeoLib>? = null

            override fun getGeoItemRenderer(): GeoItemRenderer<LuaGeoLib> {
                if (renderer == null) {
                    renderer = GeoItemRenderer(object : GeoModel<LuaGeoLib>() {
                        override fun getModelResource(animatable: LuaGeoLib): ResourceLocation =
                            ResourceLocation.parse(animatable.model_path)

                        override fun getTextureResource(animatable: LuaGeoLib): ResourceLocation =
                            ResourceLocation.parse(animatable.texture_path)

                        override fun getAnimationResource(animatable: LuaGeoLib): ResourceLocation =
                            ResourceLocation.parse(animatable.animation_path)
                    })
                }
                return renderer!!
            }
        })
    }
}



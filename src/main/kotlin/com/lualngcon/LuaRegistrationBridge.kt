@file:Suppress("RemoveRedundantQualifierName", "VariableInitializerIsRedundant", "LocalVariableName")

package com.lualngcon

import com.lualngcon.libraries.LuaGeoLib
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.component.Tool
import net.minecraft.tags.BlockTags
import org.luaj.vm2.LuaValue
import java.io.File
import java.util.Optional

object LuaRegistrationBridge {

    private val baseItemsToGroup = ArrayList<Item>()
    private val blockItemsToGroup = ArrayList<BlockItem>()
    private val foodItemsToGroup = ArrayList<Item>()

      // ======================== //
     // Tooltip Processor        //
    // ======================== //
    private fun applyTooltipComponent(properties: Item.Properties, tooltipText: String): Item.Properties {
        if (tooltipText.isEmpty() || tooltipText == "nil") return properties
        val component = net.minecraft.network.chat.Component.literal(tooltipText)
        val itemLore = net.minecraft.world.item.component.ItemLore(listOf(component))
        return properties.component(net.minecraft.core.component.DataComponents.LORE, itemLore)
    }

      // ================================ //
     // Custom Name Processor            //
    // ================================ //
    private fun applyCustomNameComponent(properties: Item.Properties, luaTable: LuaValue): Item.Properties {
        val displayName = if (!luaTable.get("displayName").isnil()) {
            luaTable.get("displayName")
        } else {
            luaTable.get("displayname")
        }

        if (!displayName.isnil()) {
            return properties.component(DataComponents.CUSTOM_NAME, Component.literal(displayName.tojstring()))
        }
        return properties
    }
      // ============================== //
     // 3D / 2D Item Factory           //
    // ============================== //

    private fun createItemFromScriptFactory(name: String, itemProps: Item.Properties, luaTable: LuaValue): Item {
        val textures = luaTable.get("textures")
        val model_table = if (textures.istable()) {
            textures.get("model")
        } else {
            LuaValue.NIL
        }

        if (!model_table.isnil() && model_table.istable()) {
            val geo_path = if (!model_table.get("model").isnil()) {
                model_table.get("model").tojstring()
            } else {
                "${LuaLangConnector.MOD_ID}:geo/$name.geo.json"
            }

            val texture_path = if (!model_table.get("texture").isnil()) {
                model_table.get("texture").tojstring()
            } else {
                "${LuaLangConnector.MOD_ID}:textures/item/$name.png"
            }

            val animation_file_path = if (!model_table.get("animation_file").isnil()) {
                model_table.get("animation_file").tojstring()
            } else {
                "${LuaLangConnector.MOD_ID}:animations/$name.animation.json"
            }

            val animations_table = model_table.get("animations")
            val default_animation_name = if (animations_table.istable()) {
                when {
                    !animations_table.get("default").isnil() -> animations_table.get("default").tojstring().substringAfterLast(":")
                    !animations_table.get("idle").isnil() -> animations_table.get("idle").tojstring().substringAfterLast(":")
                    else -> ""
                }
            } else ""

            println("[LuaLangConnector]: [Bridge] [GeckoLib]: [Lua --> Kotlin]: Unifying $name's 3D Model from its Gecko-Path ($geo_path) ;D")
            return LuaGeoLib(itemProps, geo_path, texture_path, animation_file_path, default_animation_name, default_animation_name)
        }

        return Item(itemProps)
    }

      // ======================== //
     // Creative Tabs            //
    // ======================== //
    fun registerToCreativeTabs() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
            .register(ItemGroupEvents.ModifyEntries { entries ->
                baseItemsToGroup.forEach { entries.accept(it) }
            })

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS)
            .register(ItemGroupEvents.ModifyEntries { entries ->
                blockItemsToGroup.forEach { entries.accept(it) }
            })

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS)
            .register(ItemGroupEvents.ModifyEntries { entries ->
                foodItemsToGroup.forEach { entries.accept(it) }
            })

        println("[LuaLangConnector]: [Bridge]: [Lua -> Kotlin]: All components have been added to the creative mode tab ^^")
    }
      // ======================== //
     // JSON-Packaging           //
    // ======================== //

    private fun jsonAssetGenerator(name: String, isBlock: Boolean, texturePath: String, luaTable: LuaValue) {
        val directories = listOf(
            File("../src/main/resources/assets/${LuaLangConnector.MOD_ID}"),
            File("../build/resources/main/assets/${LuaLangConnector.MOD_ID}")
        )
          // ================================================================== //
         // Texture definitions live at the root of the Lua definition table.  //
        // ================================================================== //
        val textures = luaTable.get("textures")

          // ================================================================== //
         // Vertical Strip Animation Writer (.mcmeta Generation) or VSAW       //
        // ================================================================== //
        val animation = if (textures.istable()) textures.get("animation") else LuaValue.NIL

        if (!animation.isnil() && animation.istable()) {
            val frametime = if (!animation.get("frametime").isnil()) animation.get("frametime").toint() else 1
            val interpolate = if (!animation.get("interpolate").isnil()) animation.get("interpolate").toboolean() else false
            val framesTable = animation.get("frames")

            var framesJson = ""
            if (framesTable.istable()) {
                val frameList = ArrayList<Int>()
                var idx = 1
                while (true) {
                    val fVal = framesTable.get(idx)
                    if (fVal.isnil()) break
                    frameList.add(fVal.toint())
                    idx++
                }
                if (frameList.isNotEmpty()) {
                    @Suppress("ConvertToStringTemplate")
                    framesJson = ",\n    \"frames\": " + frameList.toString()
                }
            }

            directories.forEach { directory ->
                val subFolder = if (isBlock) "block" else "item"
                val textureDir = File(directory, "textures/$subFolder")
                textureDir.mkdirs()

                val actualTextureName = texturePath.substringAfterLast("/")
                val mcmetaFile = File(textureDir, "$actualTextureName.png.mcmeta")
                mcmetaFile.writeText(
                    """
                    {
                      "animation": {
                        "frametime": $frametime,
                        "interpolate": $interpolate$framesJson
                      }
                    }
                    """.trimIndent()
                )
            }
            println("[LuaLangConnector]: [Bridge]: [Animation-Packaging]: [Lua -> Kotlin]: $name's flipbook .mcmeta generated ^^")
        }

        directories.forEach { directory ->
            val itemModels = File(directory, "models/item")
            itemModels.mkdirs()

            if (isBlock) {
                val blockModels = File(directory, "models/block")
                val blockstates = File(directory, "blockstates")

                blockModels.mkdirs()
                blockstates.mkdirs()

                // Any block face without an explicit texture falls back to top.
                val topTexture = texturePath
                val downTexture = if (textures.istable() && !textures.get("down").isnil()) {
                    textures.get("down").tojstring()
                } else topTexture

                val frontTexture = if (textures.istable() && !textures.get("front").isnil()) {
                    textures.get("front").tojstring()
                } else topTexture

                val leftTexture = if (textures.istable() && !textures.get("left_side").isnil()) {
                    textures.get("left_side").tojstring()
                } else topTexture

                val rightTexture = if (textures.istable() && !textures.get("right_side").isnil()) {
                    textures.get("right_side").tojstring()
                } else topTexture

                val backTexture = if (textures.istable() && !textures.get("back").isnil()) {
                    textures.get("back").tojstring()
                } else topTexture

                File(blockstates, "$name.json").writeText(
                    """
                    {
                      "variants": {
                        "": {
                          "model": "${LuaLangConnector.MOD_ID}:block/$name"
                        }
                      }
                    }
                    """.trimIndent()
                )

                File(blockModels, "$name.json").writeText(
                    """
                    {
                      "parent": "minecraft:block/cube",
                      "textures": {
                        "particle": "$topTexture",
                        "up": "$topTexture",
                        "down": "$downTexture",
                        "north": "$frontTexture",
                        "south": "$backTexture",
                        "west": "$leftTexture",
                        "east": "$rightTexture"
                      }
                    }
                    """.trimIndent()
                )

                File(itemModels, "$name.json").writeText(
                    """
                    {
                      "parent": "${LuaLangConnector.MOD_ID}:block/$name"
                    }
                    """.trimIndent()
                )
            } else {
                val modelTable = if (textures.istable()) textures.get("model") else LuaValue.NIL

                if (!modelTable.isnil() && modelTable.istable()) {
                    // 3D GeckoLib items use "builtin/entity" so the custom 3D renderer takes over
                    File(itemModels, "$name.json").writeText(
                        """
                        {
                          "parent": "builtin/entity"
                        }
                        """.trimIndent()
                    )
                } else {
                    // Items only use the top texture (resolved by default)
                    val itemTexture = texturePath
                    File(itemModels, "$name.json").writeText(
                        """
                        {
                          "parent": "minecraft:item/generated",
                          "textures": {
                            "layer0": "$itemTexture"
                          }
                        }
                        """.trimIndent()
                    )
                }
            }
        }

        println("[LuaLangConnector]: [Bridge]: [JSON-Packaging]: [Lua -> Kotlin]: $name's JSON files and advanced models in source and build directories :D")
    }

    // ======================== //
    // JSON Recipe Generation   //
    // ======================== //

    private fun jsonRecipeGenerator(name: String, recipe: org.luaj.vm2.LuaValue) {
        if (!recipe.istable() || recipe.length() != 9) {
            println("[LuaLangConnector]: [Recipe-Error]: La tabla no mide 9 elementos o no es una tabla válida ._.")
            return
        }

        val directories = listOf(
            File("../src/main/resources/data/${LuaLangConnector.MOD_ID}/recipes"),
            File("../build/resources/main/data/${LuaLangConnector.MOD_ID}/recipes")
        )

        val keymap = HashMap<String, String>()
        val character_list = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I")
        var character_index = 0

        val slots = ArrayList<String>()

        for (i in 1..9) {
            val item_name = recipe.get(i).tojstring()
            slots.add(item_name)

            if (item_name != "minecraft:air" && !keymap.containsKey(item_name)) {
                if (character_index < character_list.size) {
                    keymap[item_name] = character_list[character_index]
                    character_index++
                }
            }
        }

        // =========================================================== //
        // Here, we safely store empty slots if they're air, or empty  //
        // =========================================================== //
        val c0 = if (slots[0] == "minecraft:air") " " else keymap[slots[0]] ?: " "
        val c1 = if (slots[1] == "minecraft:air") " " else keymap[slots[1]] ?: " "
        val c2 = if (slots[2] == "minecraft:air") " " else keymap[slots[2]] ?: " "
        val c3 = if (slots[3] == "minecraft:air") " " else keymap[slots[3]] ?: " "
        val c4 = if (slots[4] == "minecraft:air") " " else keymap[slots[4]] ?: " "
        val c5 = if (slots[5] == "minecraft:air") " " else keymap[slots[5]] ?: " "
        val c6 = if (slots[6] == "minecraft:air") " " else keymap[slots[6]] ?: " "
        val c7 = if (slots[7] == "minecraft:air") " " else keymap[slots[7]] ?: " "
        val c8 = if (slots[8] == "minecraft:air") " " else keymap[slots[8]] ?: " "

        val first_row = "$c0$c1$c2"
        val second_row = "$c3$c4$c5"
        val third_row = "$c6$c7$c8"

        val key_json = keymap.map { (item, character) ->
            "\"$character\": { \"item\": \"$item\" }"
        }.joinToString(",\n                    ")

        directories.forEach { directory ->
            try {
                directory.mkdirs()
                val recipe_file = File(directory, "${name}_recipe.json")

                recipe_file.writeText(
                    """
                   {
                     "type": "minecraft:crafting_shaped",
                     "category": "equipment",
                     "pattern": [
                       "$first_row",
                       "$second_row",
                       "$third_row"
                     ],
                     "key": {
                       $key_json
                     },
                     "result": {
                       "id": "${LuaLangConnector.MOD_ID}:$name",
                       "count": 1
                     }
                   }
                   """.trimIndent()
                )
                println("[LuaLangConnector]: [Bridge]: [JSON-Recipe-Packaging]: $name's recipe generated successfully 'u.")
            } catch (_: Exception) {
                println("[LuaLangConnector]: [Bridge]: [JSON-Recipe-Packaging]: $name's recipe generated un-successfully -_.")
            }
        }
        println("[LuaLangConnector]: [Bridge]: [JSON-Recipe-Packaging]: [Lua --> Kotlin]: $name's recipe has been fully generated '_.!")
    }


      // ======================== //
     // Lua Registry Processing  //
    // ======================== //

    fun processLuaRegistry(luaTable: LuaValue, registrationType: String) {
        if (!luaTable.istable()) return

        val name = luaTable.get("name").tojstring()
        val toolTip = if (!luaTable.get("toolTip").isnil()) {
            luaTable.get("toolTip").tojstring()
        } else {
            luaTable.get("tooltip").tojstring()
        }

        val properties = luaTable.get("properties")
        if (properties.isnil()) return

        var isBlockType = false

        when (registrationType) {
            "block" -> {
                isBlockType = true
                registerBlockAPI(name, properties, toolTip, luaTable)
                println("[LuaLangConnector]: [Bridge]: [Block API]: Registered block item successfully ^^")
            }

            "food" -> {
                registerFoodAPI(name, properties, toolTip, luaTable)
                println("[LuaLangConnector]: [Bridge]: [Food API]: Registered food item successfully ^^")
            }

            "tool" -> {
                registerToolAPI(name, properties, toolTip, luaTable)
                println("[LuaLangConnector]: [Bridge]: [Tool API]: Registered tool item successfully ^^")
            }

            "item" -> {
                registerBaseItemAPI(name, toolTip, luaTable)
                println("[LuaLangConnector]: [Bridge]: [Item API]: Registered base item successfully ^^")
            }

            "universal" -> {
                isBlockType = properties.get("isBlock").toboolean()
                when {
                    isBlockType -> registerBlockAPI(name, properties, toolTip, luaTable)
                    properties.get("isFood").toboolean() -> registerFoodAPI(name, properties, toolTip, luaTable)
                    properties.get("isTool").toboolean() -> registerToolAPI(name, properties, toolTip, luaTable)
                    else -> registerBaseItemAPI(name, toolTip, luaTable)
                }
                println("[LuaLangConnector]: [Bridge]: Processed universal registry entry ^^")
            }
        }

        val texturePath = resolveTexture(luaTable, name, isBlockType)
        jsonAssetGenerator(name, isBlockType, texturePath, luaTable)
    }

    private fun resolveTexture(luaTable: LuaValue, name: String, isBlock: Boolean): String {
        val defaultTexture = if (isBlock) {
            "${LuaLangConnector.MOD_ID}:block/$name"
        } else {
            "${LuaLangConnector.MOD_ID}:item/$name"
        }

        val textures = luaTable.get("textures")
        if (!textures.istable()) return defaultTexture

        val top = textures.get("top")
        val texturePath = if (!top.isnil()) top.tojstring() else defaultTexture

        if (!top.isnil()) {
            println("[LuaLangConnector]: [Bridge]: [Texture-Packager]: [Lua -> Kotlin]: $name's texture was successfully loaded ^^")
        }

        return texturePath
    }
      // ======================== //
     // Item Registration APIs   //
    // ======================== //

    private fun registerBaseItemAPI(name: String, toolTip: String, luaTable: LuaValue) {
        println("[LuaLangConnector]: [Bridge]: [Lua -> Kotlin]: Registering $name, with a tooltip of $toolTip :D")

        val id = ResourceLocation.fromNamespaceAndPath(LuaLangConnector.MOD_ID, name)

        var itemProps = applyTooltipComponent(Item.Properties(), toolTip)
        itemProps = applyCustomNameComponent(itemProps, luaTable)

        val item = createItemFromScriptFactory(name, itemProps, luaTable)

        Registry.register(BuiltInRegistries.ITEM, id, item)
        baseItemsToGroup.add(item)
    }
      // ======================== //
     // Block Registration API   //
    // ======================== //

    private fun registerBlockAPI(name: String, properties: LuaValue, toolTip: String, luaTable: LuaValue) {
        val hardness = if (!properties.get("hardness").isnil()) properties.get("hardness").tofloat() else 1.0f
        val resistance = if (!properties.get("explosionResistance").isnil()) properties.get("explosionResistance").tofloat() else hardness
        val lightEmission = if (!properties.get("lightEmission").isnil()) properties.get("lightEmission").toint() else 0

        println("[LuaLangConnector]: [Bridge]: [Block API]: [Lua -> Kotlin]: Registering your $name block, with a hardness of $hardness :D")

        val id = ResourceLocation.fromNamespaceAndPath(LuaLangConnector.MOD_ID, name)
        val block = Block(
            BlockBehaviour.Properties.of()
                .destroyTime(hardness)
                .explosionResistance(resistance)
                .lightLevel { lightEmission }
        )

        Registry.register(BuiltInRegistries.BLOCK, id, block)

        // Apply Lore and Custom Name to the BlockItem
        var blockItemProps = applyTooltipComponent(Item.Properties(), toolTip)
        blockItemProps = applyCustomNameComponent(blockItemProps, luaTable)

        val blockItem = BlockItem(block, blockItemProps)
        Registry.register(BuiltInRegistries.ITEM, id, blockItem)

        blockItemsToGroup.add(blockItem)
    }

    private fun registerFoodAPI(name: String, properties: LuaValue, toolTip: String, luaTable: LuaValue) {
        val nutrition = properties.get("nutrition").toint()
        val saturation = properties.get("saturation").tofloat()

        println("[LuaLangConnector]: [Bridge]: [Food API]: [Lua -> Kotlin]: Registering your $name food item with a nutrition of $nutrition and a saturation of $saturation :D")

        val id = ResourceLocation.fromNamespaceAndPath(LuaLangConnector.MOD_ID, name)
        val foodBuilder = FoodProperties.Builder()
            .nutrition(nutrition)
            .saturationModifier(saturation)

        registerFoodEffects(name, properties, foodBuilder)

        var foodProps = applyTooltipComponent(Item.Properties().food(foodBuilder.build()), toolTip)
        foodProps = applyCustomNameComponent(foodProps, luaTable)

        val food = Item(foodProps)
        Registry.register(BuiltInRegistries.ITEM, id, food)
        foodItemsToGroup.add(food)
    }
      // ======================== //
     // Food Effects             //
    // ======================== //

    private fun registerFoodEffects(name: String, properties: LuaValue, foodBuilder: FoodProperties.Builder) {
        val effects = properties.get("builtInEffects").get("effects")
        if (!effects.istable()) return

        var index = 1

        while (true) {
            val effectEntry = effects.get(index)
            if (effectEntry.isnil()) break

            var effectName = ""  // ==================================================================================== //
            var duration = 200  // If you don't have a specific duration, the default one will be of about 10 seconds ;D//
            var amplifier = 0  // This is our effect's amplifier.                                                      //
                              // ==================================================================================== //

            // Check if the user passed a detailed table or just a simple string
            if (effectEntry.istable()) {
                effectName = effectEntry.get("id").tojstring()
                if (!effectEntry.get("duration").isnil()) duration = effectEntry.get("duration").toint()
                if (!effectEntry.get("amplifier").isnil()) amplifier = effectEntry.get("amplifier").toint()
            } else {
                effectName = effectEntry.tojstring()
            }

            val location = ResourceLocation.tryParse(effectName)

            if (location != null) {
                val registry = BuiltInRegistries.MOB_EFFECT
                val key = ResourceKey.create(registry.key(), location)
                val entry: Optional<Holder.Reference<MobEffect>> = registry.getHolder(key)

                if (entry.isPresent) {
                    val effect = MobEffectInstance(entry.get(), duration, amplifier)
                    foodBuilder.effect(effect, 1.0f)
                    println("[LuaLangConnector]: [Bridge]: [Food API]: Effect $effectName ($duration ticks) added to $name ^^")
                } else {
                    println("[LuaLangConnector]: [Bridge]: [Food API]: [Error]: Magic effect '$effectName' not found ._.")
                }
            }
            index++
        }
    }

      // ======================== // ================================================================================================= //
     // Tool Registration API    // This, is the Tool API, and it's and API for tool registry, and an API for tools to use when made. //
    // ======================== // ================================================================================================= //
    private fun registerToolAPI(name: String, properties: LuaValue, toolTip: String, luaTable: LuaValue) {
        val toolType = properties.get("toolType").tojstring()
        val uses = if (!properties.get("uses").isnil()) properties.get("uses").toint() else 100
        println("[LuaLangConnector]: [Bridge]: [Tool API]: [Lua -> Kotlin]: Forging your $name tool with $uses and a tool type of $toolType ^^")

        val id = ResourceLocation.fromNamespaceAndPath(LuaLangConnector.MOD_ID, name)
        val miningRules = ArrayList<Tool.Rule>()
        val canbreak = properties.get("canbreak")

        if (!canbreak.isnil() && canbreak.istable()) {
            val blocksElement = canbreak.get("blocks")
            if (blocksElement.istable()) {
                var i = 1
                val exclusiveBlocks = ArrayList<Block>()
                while (true) {
                    val blockValue = blocksElement.get(i)
                    if (blockValue.isnil()) break

                    val blockName = blockValue.tojstring()
                    val blockLoc = ResourceLocation.tryParse(blockName)
                    if (blockLoc != null) {
                        val blockEntry = BuiltInRegistries.BLOCK.getOptional(blockLoc)
                        if (blockEntry.isPresent) {
                            exclusiveBlocks.add(blockEntry.get())
                        }
                    }
                    i++
                }
                if (exclusiveBlocks.isNotEmpty()) {
                    exclusiveBlocks.forEach { blockObject ->
                        miningRules.add(Tool.Rule.minesAndDrops(listOf(blockObject), 8.0f))
                    }
                }
            } else if (blocksElement.isstring() || blocksElement.isint()) {
                val ruleText = blocksElement.tojstring()
                val miningLevel = ruleText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 3
                val tagKey = when (miningLevel) {
                    0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL
                    1 -> BlockTags.INCORRECT_FOR_STONE_TOOL
                    2 -> BlockTags.INCORRECT_FOR_IRON_TOOL
                    3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL
                    else -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL
                }
                miningRules.add(Tool.Rule.deniesDrops(tagKey))
            }
        }

        val baseTag = when (toolType) {
            "axe" -> BlockTags.MINEABLE_WITH_AXE
            "shovel" -> BlockTags.MINEABLE_WITH_SHOVEL
            "hoe" -> BlockTags.MINEABLE_WITH_HOE
            else -> BlockTags.MINEABLE_WITH_PICKAXE
        }
        miningRules.add(Tool.Rule.minesAndDrops(baseTag, 8.0f))

          val toolComponent = Tool(miningRules, 1.0f, 1)

          val damage = if (!properties.get("damage").isnil()) {
              properties.get("damage").todouble()
          } else {
              1.0
          }

          val knockback = if (!properties.get("knockback").isnil()) properties.get("knockback").todouble() else 0.0

          val finalDamage = if (damage > 1.0) damage - 1.0 else 0.0

          val modifierBuilder = ItemAttributeModifiers.builder()
              .add(
                  Attributes.ATTACK_DAMAGE,
                  AttributeModifier(
                      Item.BASE_ATTACK_DAMAGE_ID,
                      finalDamage,
                      AttributeModifier.Operation.ADD_VALUE
                  ),
                  EquipmentSlotGroup.MAINHAND
              )

          // Inject Attack Knockback if specified in Lua
          if (knockback > 0.0) {
              modifierBuilder.add(
                  Attributes.ATTACK_KNOCKBACK,
                  AttributeModifier(
                      ResourceLocation.fromNamespaceAndPath(LuaLangConnector.MOD_ID, "base_attack_knockback"),
                      knockback,
                      AttributeModifier.Operation.ADD_VALUE
                  ),
                  EquipmentSlotGroup.MAINHAND
              )
          }

          val attributeModifiers = modifierBuilder.build()

          var toolProps = applyTooltipComponent(
              Item.Properties()
                  .durability(uses)
                  .component(DataComponents.TOOL, toolComponent)
                  .component(DataComponents.ATTRIBUTE_MODIFIERS, attributeModifiers),
              toolTip
          )

          toolProps = applyCustomNameComponent(toolProps, luaTable) // Idk how this works, I better NOT touch it

          val toolItem = createItemFromScriptFactory(name, toolProps, luaTable)
          Registry.register(BuiltInRegistries.ITEM, id, toolItem)
          baseItemsToGroup.add(toolItem)

        val recipe = properties.get("recipe")
        if (!recipe.isnil() && recipe.istable()) {
            jsonRecipeGenerator(name, recipe)
        }
    }
}

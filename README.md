# Fuzzy's - LuaLangConnector
LuaLangConnector is an easy-to-use framework to develop Minecraft mods using the Lua programming language, yeah, kind of ironic considering we've been using Java for so long ^^

# Features
```
Automatic JSON Generation for both recipes and models
Automated .mcmetas
GeckoLib 3D Model & Animation support (Still in the works, to make sure it works, isn't it obvious)
```

# Here's the current API as a Lua table!
```lua
local minecraft = {
    register = {
        item = function(item) end, -- Registers an item
        block = function(block) end, -- Registers a block
        food = function(food) end, -- Registers food
        tool = function(tool) end -- Registers tools
    },

    events = {
        on = {
            join = function(player) end, -- When the player joins, do something
            ground = function(player, block) end, -- When this block is below the player, do something
            finished_use = function(player, item) end, -- When the item has its use complete, do something
            handheld = function(player, item) end -- When this item is being handheld, do something
        }
    }
}

local player = {
    name, -- The player's name
    inventory = {
        add = function(item, quantity) end, -- Adds something to the inventory
        elim = function(item, quantity) end, -- Removes something from the inventory
        has = function(item) end -- If the player has this, do something
    },

    effects = {
        grant = function(effect, duration, amplifier) -- Grants an effect to the player
    }
}
```

# So now, how do I make a custom block, item, tool or food?
Let's look at it like this:
All items, blocks, foods and tools share the same universal need: name, displayname, tooltip and textures.

```name```: the internal name of your block, tool, food or item

```displayname```: the actual displayed name in-game

```tooltip```: the tooltip of your block, tool, food or item

and the ```textures``` table: a table where it has all the textures it needs with a given path. while the blocks have 6 faces, items only need one: the ```top``` face


But what about the properties? well, they also share a table: the properties table. each one has it's own API implemented ^^

# The block API:
```lua
-- Your block
local block = {
    name = "block",
    displayname = "Block",
    tooltip = "A Block",

    textures = {
        top = "modid:block/block_top",
        down = "modid:block/block_down",
        front = "modid:block/block_front",
        back = "modid:block/block_back", -- these names are just examples '_.
        left_side = "modid:block/block_left_side",
        right_side = "modid:block/block_right_side"
    },

    properties = {
        hardness = 3.0, -- 1: The hardness of the block
        explosionResistance = 12.0, -- 2: The blast resistance of the block
        lightEmission = 12.0 -- The light emission level of the block (Using Minecraft's ligthning levels)
    }
}

minecraft.register.block(block) -- Here, we register the block
```
That's how you make & register a block! the API is almost always the same, with the only changes being the ```properties``` API & The ```textures``` table, since all 2D Items only need one thing: the ```top``` texture.

# Food API:

```nutrition```: the nutrition of your edible

```saturation```: the saturation given by your edible

```builtInEffects```: a table only for edibles that grants effects once the edible is eaten.

```builtInEffects.effects```: this is where you'll place all of the effects, like ```"minecraft:haste"```

# Tool API:

```toolType```: the type of your tool, if its ```"axe"``` or ```"sword"```, you can modify the ```damage``` of it.

```damage```: the damage of your tool.

If you're wondering where the ```knockback``` or ```speed``` properties are, I'll implement them later.

## Section for pickaxes, axes and hoes

```canbreak``` This is a special table to specify which blocks you want to break, for example:

```lua
canbreak = {
    blocks = {
        -- your blocks
    }
}

```

Here, ```canbreak.blocks``` has two possible receivers, you can either specify that any block <= number is breakable, with four tiers: stone, iron, diamond and netherite. or, you can treat it as a list of rules like *"This tool can only break these blocks, or this block"*, for example:

```lua
canbreak = {
    blocks = {
        "any <= 3" -- The tier of a diamond tool
    }
}
```

or:

```lua
canbreak = {
    blocks = {
        "minecraft:iron_ore", "minecraft:gold_ore", "minecraft:diamond_ore", "minecraft:ancient_debris" -- An exclusive list of blocks this tool can break
    }
}
```

# Item API:

The thing with making an item is that, it shares the same basic needs: name, displayname. tooltip, & textures.top, and is the only type that doesn't need it's own properties, here's an example:

```lua
local item = {
    name = "item",
    displayname = "Item",
    tooltip = "An item",

    textures = {
        top = "modid:item/item"
    }
}

minecraft.register.item(item)
```

And they also share one more need: the optional ```recipe``` table. here, you'll add a recipe to anything you want, for example:

```lua

-- Your item / food / block / tool, I'm not going to make a long example again, let's pretend like you didn't read this... I'm sure you did anyways

recipe = {
    "minecraft:x" , "minecraft:x", "minecraft:x",
    "minecraft:x", "minecraft:x", "minecraft:x",
    "minecraft:x", "minecraft:x", "minecraft:x"
}
```
For empty slots, use ```"minecraft:air"```.

# Events
## Wanted to give your own items their own personalized behaviours? 
that's what the ```events``` table is for!

as you saw in the API Table, the ```events``` table has the following table: ```on```, which is the table you'll use the most (for now), with the ```on``` table you can detect an item being handheld, when an item has its use complete, when the player joins & when a block is below the player. since you already read the API table (which if you haven't, read it. I SAID READ IT.), I'll just place examples ^^

# 1: When a player joins

```lua
minecraft.events.on.join = function(player)
    player.inventory.add("minecraft:iron_ingot", 32) -- Or whatever else you want to do, I don't know, give him a kit or something
end
```

# 2: When an item is being handheld

```lua
minecraft.events.on.handheld = function(item)
    if item == "minecraft:iron_ingot" or item == "modid:item" then
        player.inventory.elim("minecraft:iron_ingot", 64)
    end
end
```

# 3: When a block is below the player

```lua
minecraft.events.on.ground = function(player, block)
    if block == "minecraft:grass" then
        player.inventory.add("minecraft:grass", 1)
    end
end
```

Now... let's say you want an item to have its own 3D model, you may ask. well, I have good news, it has (experimental) 3D model support using GeckoLib! an example:

```lua
textures = {
    model = {
        model = "modid:geo/item.geo.json",
        texture = "modid:model/item.png",
        animation_file = "modid:animations/item.animation.json",
        animations = {
            idle = "idle",
            walk = "walk",
            run = "run"
            -- ect
        }
    }
}
```

```model```: the model of your model

```texture```: the textures of your model

```animation_file```: the file that contains all of your animations

```animations```: here, you'll place your animations. they must have the same name as the animations you created in BlockBench.

For GeckoLib models, there's an special library called play, switch and pause. ```play.animation()``` plays an animation, for example:

```lua
minecraft.events.on.handheld = function(item)
    if item == "modid:item" then
        play.animation("idle")
    end
end
```

The same thing for both pause and switch.

# Getting started
## How do I use it?
First, is the project structure.
## Project structure

You'd probably think that you would need to give LuaLangConnector a folder, and I thought the same thing! but, LuaLangConnector reads all the .jar mods in your ```mods``` folder, finds the scripts, and registers them to Minecraft. so, you can still have your .jar mod with all the scripts in it!

in your project template, we all know it has the ```java``` and ```kotlin``` folders. so? create a new one called ```lua``` inside src/main, and a ```scripts``` folder inside it. here, you'll place your scripts, which can be anything: a block, an item, an event manager, anything you imagine is there. LuaLangConnector will read those scripts and register them to Minecraft.

## Structure
I'll be releasing an example project, but for now, I'd reccommend you make your projects like this:

```src/

|-- main/
|    ├─ kotlin
|    ├─ java
|    |─ lua/
|    |   ├─ scripts/
|    |   |    ├─ your scripts
|    ├─ resources/
|    |       ├─ assets/
|    |       |     ├─ textures/
|    |       |     |      ├─ item/
|    |       |     |      ├─ block
|    |       |     ├─ geo/
|    |       |     |    ├─ your-model.geo.json
|    |       |     ├─ animations/
|    |       |     |    ├─ your-models-animation.animation.json
|    |       |     ├─ blockstates/
|    |       |     |      ├─ here, the blockstates are automated, you don't have to do anything here.
|    |       |     ├─ model/
|    |       |     |    ├─ your-models-textures.png
```

Remember: your recipes and models are automatically generated, so don't worry.

# Installation
You may be wondering: "How can I implement it in my project?"

The thing is... you don't have to.

Yes, it sounds weird, incorrect, almost wrong, but no.

LuaLangConnector only needs to be inside your mods folder, as we saw previously. That's literally the dependency.

If you want to make sure everything feels right, just place the following comment in your build.gradle.kts (or Java, depends on your setup):
```
// Dependencies
// modImplementation("LuaLangConnector")
```
# Overall, LuaLangConnector is an API that I made because I wanted to program in Lua, easily.
because, I thought: "What if Minecraft modding could be made with Lua?", and here we are.
The mod isn't published yet, this is obviously just a repository, but I'll leave it anyways for you to read.
thank you for reading this.

Is it really coding when you aren't coding in the language you truly love? - YourFuzzyLuaDev, Lua 5.1.5 ^^.



This is only the start.

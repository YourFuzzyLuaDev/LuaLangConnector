# Fuzzy's - LuaLangConnector
LuaLangConnector is an easy-to-use framework to develop Minecraft mods using the Lua programming language, yeah, kind of ironic considering we've been using Java for so long ^^

# Features
Automatic JSON Generation for both recipes and models

Automated .mcmetas

GeckoLib 3D Model & Animation support (Still in the works, to make sure it works, isn't it obvious)

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
            finished_use = function(player, item) end -- When the item has it's use complete, do something
        }
    }
}

local player = {
    name, -- The player's name
    inventory = {
        add = function(item, quantity) end, -- Adds something to the inventory
        elim = function(item, quantity) end, -- Removes something from the inventory
        has = function(item) end -- If the player has this, do something
    }
}
```

# So now, how do I make a custom block, item, tool or food?
Let's look at it like this:
All items, blocks, foods and tools share the same universal need: name, displayname, tooltip and textures.

```name```: the internal name of your block, tool, food or item,

```displayname```: the actual displayed name in-game,

```tooltip```: the tooltip of your block, tool, food or item,

and the ```textures``` table: a table where it has all the textures it needs with a given path. while the blocks have 6 faces, items only need one: the ```top``` face.

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
food API:

```nutrition```: the nutrition of your edible

```saturation```: the saturation given by your edible

```builtInEffects```: a table only for edibles that grants effects once the edible is eaten.

```builtInEffects.effects```: this is where you'll place all of the effects, like ```"minecraft:haste"```
tool API:

```toolType```: the type of your tool, if its ```"axe"``` or ```"sword"```, you can modify the damage of it.

```

package com.lualngcon

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readText

object LuaLangConnector : ModInitializer {
	const val MOD_ID: String = "lualangconnector"
	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("[LuaLangConnector]: [Bridge]: [Initializing]: Hi :]")

		val globals = JsePlatform.standardGlobals()
		val minecraftTable = LuaValue.tableOf()
		val registerTable = LuaValue.tableOf()
		com.lualngcon.libraries.LuaPlayBackLib.registerPlaybackLibraries(globals)

		registerTable.set("item", object : OneArgFunction() {
			override fun call(arg: LuaValue): LuaValue {
				LuaRegistrationBridge.processLuaRegistry(arg, "item")
				return LuaValue.NIL
			}
		})

		registerTable.set("block", object : OneArgFunction() {
			override fun call(arg: LuaValue): LuaValue {
				LuaRegistrationBridge.processLuaRegistry(arg, "block")
				return LuaValue.NIL
			}
		})

		registerTable.set("food", object : OneArgFunction() {
			override fun call(arg: LuaValue): LuaValue {
				LuaRegistrationBridge.processLuaRegistry(arg, "food")
				return LuaValue.NIL
			}
		})

		registerTable.set("tool", object : OneArgFunction() {
			override fun call(arg: LuaValue): LuaValue {
				LuaRegistrationBridge.processLuaRegistry(arg, "tool")
				return LuaValue.NIL
			}
		})

		registerTable.set("universal", object : OneArgFunction() {
			override fun call(arg: LuaValue): LuaValue {
				LuaRegistrationBridge.processLuaRegistry(arg, "universal")
				return LuaValue.NIL
			}
		})

		minecraftTable.set("register", registerTable)
		LuaEventBridge.registerEventsApi(minecraftTable)
		globals.set("minecraft", minecraftTable)

		  // ======================== //
		 // Hybrid Compiling System  //
		// ======================== //
		var loadedfiles = false

		  // ====================================================== //
		 // Here, we look for the lua/scripts folder in our source //
		// ====================================================== //
		val scriptsfolder = File("../src/main/lua/scripts")

		if (scriptsfolder.exists() && scriptsfolder.isDirectory) {
			val luafiles = scriptsfolder.listFiles()
			if (luafiles != null) {
				for (file in luafiles) {
					if (file.isFile && file.name.endsWith(".lua")) {
						LOGGER.info("[LuaLangConnector]: [Bridge]: [Scripts-Loader]: [Lua --> Kotlin] Loading local scripts ${file.name} '_.")
						val chunk = globals.loadfile(file.absolutePath)
						chunk.call()
						loadedfiles = true
					}
				}
			}
		}

		  // ========================================================================================== //
		 // Here, we look into all the ModContainers using Fabric's virtual file system architecture.  //
		// ========================================================================================== //
		if (!loadedfiles) {
            for (modContainer in net.fabricmc.loader.api.FabricLoader.getInstance().allMods) {
                val scriptsPath = modContainer.findPath("lua/scripts")
                if (scriptsPath.isPresent && java.nio.file.Files.exists(scriptsPath.get())) {
                    try {
                        java.nio.file.Files.walk(scriptsPath.get()).use { stream ->
                            stream.filter { java.nio.file.Files.isRegularFile(it) && it.toString().endsWith(".lua") }
                                .forEach { path ->
                                    LOGGER.info("[LuaLangConnector]: [Jar-Load]: De-packaging '${path.fileName}' virtually from mod container: ${modContainer.metadata.id} ^^")
                                    val scriptContent = java.nio.file.Files.readString(path)
                                    val chunk = globals.load(scriptContent, path.fileName.toString())
                                    chunk.call()
                                    loadedfiles = true
                                }
                        }
                    } catch (e: Exception) {
                        LOGGER.error("[LuaLangConnector]: [Bridge]: [Script-Management]: [Lua ~~ Kotlin] Failed to virtually load from container ${modContainer.metadata.id}: ${e.message} ;_;")
                    }
                }
            }
        }

		  // ==================================================================== //
		 // Here, we execute the injection of both the recipe and JSON Packagers //
		// ==================================================================== //
        LuaRegistrationBridge.registerToCreativeTabs()

		if (!loadedfiles) {
			LOGGER.warn("[LuaLangConnector]: [Bridge]: [Warning]: Could not locate any script files in local sources or virtual mod environments ._.")
			}
		}

	fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}

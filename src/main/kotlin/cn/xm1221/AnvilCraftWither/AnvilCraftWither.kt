package cn.xm1221.AnvilCraftWither

import com.mojang.logging.LogUtils
import cn.xm1221.AnvilCraftWither.data.AddonDatagen
import cn.xm1221.AnvilCraftWither.init.AddonBlocks
import cn.xm1221.AnvilCraftWither.init.AddonItemGroups
import cn.xm1221.AnvilCraftWither.init.AddonItems
import cn.xm1221.AnvilCraftWither.init.ModRecipeTypes
import dev.anvilcraft.lib.v2.config.ConfigManager
import dev.anvilcraft.lib.v2.registrum.Registrum
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger

@Mod(AnvilCraftWither.MOD_ID)
class AnvilCraftWither(modEventBus: IEventBus, modContainer: ModContainer) {
    companion object {
        const val MOD_ID: String = "anvilcraft_wither"
        val LOGGER: Logger = LogUtils.getLogger()
        val CONFIG: AddonConfig = ConfigManager.register(MOD_ID, ::AddonConfig)
        val REGISTRUM: Registrum = Registrum.create(MOD_ID)

        @NotNull
        fun of(path: String): ResourceLocation {
            return ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
        }
    }

    init {
        AddonItemGroups.register(modEventBus)
        ModRecipeTypes.register(modEventBus)
        AddonBlocks.register()
        AddonItems.register()
        AddonDatagen.init()
    }
}
package cn.xm1221.AnvilCraftWither.data

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.data.lang.AddonLangHandler
import cn.xm1221.AnvilCraftWither.data.recipe.WitherTransformationRecipeHandler
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.data.event.GatherDataEvent

@EventBusSubscriber(modid = AnvilCraftWither.MOD_ID)
class WitherDataGen {
    companion object {
        @SubscribeEvent
        @JvmStatic
        fun gatherData(event: GatherDataEvent) {
        }

        /**
         * 初始化生成器
         */
        fun init() {
            AnvilCraftWither.REGISTRUM.addDataGenerator(ProviderType.LANG, AddonLangHandler::init)
            AnvilCraftWither.REGISTRUM.addDataGenerator(ProviderType.RECIPE, WitherTransformationRecipeHandler::init)
        }
    }
}
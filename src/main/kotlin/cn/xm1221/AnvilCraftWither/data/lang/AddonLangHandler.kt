package cn.xm1221.AnvilCraftWither.data.lang

import cn.xm1221.AnvilCraftWither.AddonConfig
import dev.anvilcraft.lib.v2.config.ConfigData
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider

class AddonLangHandler {
    companion object {
        /**
         * 语言文件初始化（Registrum 仅生成 en_us / en_ud；中文见 assets/anvilcraft_wither/lang/zh_cn.json）
         *
         * @param provider 提供器
         */
        fun init(provider: RegistrumLangProvider) {
            ConfigData.readConfigClass(provider, AddonConfig::class.java)

            // JEI 类别
            provider.add(
                "gui.anvilcraft_wither.category.wither_transformation",
                "Wither Transformation",
            )
            provider.add(
                "gui.anvilcraft_wither.category.wither_transformation.mode.anchor",
                "Mode: Anchor (converts the hit block, explosion unchanged)",
            )
            provider.add(
                "gui.anvilcraft_wither.category.wither_transformation.mode.anchor_no_explosion",
                "Mode: Anchor, no explosion (suppresses the explosion)",
            )
            provider.add(
                "gui.anvilcraft_wither.category.wither_transformation.mode.area",
                "Mode: Area (converts all matching blocks in range, explosion unchanged)",
            )
            provider.add(
                "gui.anvilcraft_wither.category.wither_transformation.radius",
                "Radius: %s",
            )
            provider.add(
                "gui.anvilcraft_wither.category.wither_transformation.pattern",
                "Multiblock pattern (bottom to top, space = any)",
            )
        }
    }
}

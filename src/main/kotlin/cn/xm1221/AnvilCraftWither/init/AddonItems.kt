package cn.xm1221.AnvilCraftWither.init

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.AnvilCraftWither.Companion.REGISTRUM
import cn.xm1221.AnvilCraftWither.items.WitherStaff
import cn.xm1221.AnvilCraftWither.items.WitherStaffConfig
import cn.xm1221.AnvilCraftWither.items.WitherStaffInteractionConfig
import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry
import net.minecraft.world.item.Item

class AddonItems {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    companion object {
        init {
            REGISTRUM.defaultCreativeTab(AddonItemGroups.ADDON_ITEMS.key)
            // 物品名由 Registrum LANG 自动生成（item.anvilcraft_wither.wither_staff），勿手动 addLang 以免重复
        }

        val WITHER_STAFF: ItemEntry<WitherStaff> = REGISTRUM
            .item("wither_staff") {
                WitherStaff(
                    Item.Properties().stacksTo(1)
                        .durability(500)
                        .component(ModDataComponents.WITHER_STAFF_CONFIG.get(), WitherStaffConfig())
                        .component(ModDataComponents.WITHER_STAFF_INTERACTION.get(), WitherStaffInteractionConfig()),
                )
            }
            .register()

        fun register() {
        }
    }
}

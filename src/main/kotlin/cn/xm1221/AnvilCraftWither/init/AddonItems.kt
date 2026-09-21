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
            REGISTRUM.addLang("item", AnvilCraftWither.of("wither_staff"), "Wither Staff")
        }

        val WITHER_STAFF: ItemEntry<WitherStaff> = REGISTRUM
            .item("wither_staff") {
                WitherStaff(
                    Item.Properties().stacksTo(1)
                        .component(ModDataComponents.WITHER_STAFF_CONFIG.get(), WitherStaffConfig())
                        .component(ModDataComponents.WITHER_STAFF_INTERACTION.get(), WitherStaffInteractionConfig()),
                )
            }
            .register()

        fun register() {
        }
    }
}

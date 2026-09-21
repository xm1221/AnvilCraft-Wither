package cn.xm1221.AnvilCraftWither.init

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.items.WitherStaffConfig
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModDataComponents {
    private val DATA_COMPONENT_TYPES: DeferredRegister<DataComponentType<*>> =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AnvilCraftWither.MOD_ID)

    /** 凋灵法杖行为配置 */
    val WITHER_STAFF_CONFIG: DeferredHolder<DataComponentType<*>, DataComponentType<WitherStaffConfig>> =
        DATA_COMPONENT_TYPES.register(
            "wither_staff_config",
            Supplier {
                DataComponentType.builder<WitherStaffConfig>()
                    .persistent(WitherStaffConfig.CODEC)
                    .networkSynchronized(WitherStaffConfig.STREAM_CODEC)
                    .build()
            },
        )

    fun register(modEventBus: IEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus)
    }
}

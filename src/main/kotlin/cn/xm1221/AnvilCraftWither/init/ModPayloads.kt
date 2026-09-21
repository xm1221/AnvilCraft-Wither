package cn.xm1221.AnvilCraftWither.init

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.event.WitherStaffInteractHandler
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

/**
 * 客户端→服务端：法杖长按左键瞄准实体，上报转化意图
 *
 * @param entityId 目标实体 id
 */
class StaffTransformEntityPayload(val entityId: Int) : CustomPacketPayload {
    companion object {
        val TYPE: CustomPacketPayload.Type<StaffTransformEntityPayload> =
            CustomPacketPayload.Type(AnvilCraftWither.of("staff_transform_entity"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, StaffTransformEntityPayload> =
            StreamCodec.composite(
                ByteBufCodecs.INT, { it.entityId },
                ::StaffTransformEntityPayload,
            )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

object ModPayloads {
    fun register(modEventBus: IEventBus) {
        modEventBus.addListener(::onRegisterPayloads)
    }

    private fun onRegisterPayloads(event: RegisterPayloadHandlersEvent) {
        event.registrar("1").playToServer(
            StaffTransformEntityPayload.TYPE,
            StaffTransformEntityPayload.STREAM_CODEC,
        ) { payload, context ->
            context.enqueueWork { WitherStaffInteractHandler.handleTransformEntity(payload, context) }
        }
    }
}

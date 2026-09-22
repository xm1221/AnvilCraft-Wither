package cn.xm1221.AnvilCraftWither.init

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.client.StaffBeamRenderer
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadHandler
import net.neoforged.neoforge.network.registration.PayloadRegistrar

/**
 * 法杖时移成功后的服务端→客户端通知：让客户端在时移位置渲染光柱。
 * 时移结果由服务端权威判定，光柱显示跟随服务端判定（不做客户端配方预测）。
 */
object ModPayloads {
    data class StaffTimewarpBeamPayload(val pos: BlockPos) : CustomPacketPayload {
        companion object {
            val TYPE: CustomPacketPayload.Type<StaffTimewarpBeamPayload> =
                CustomPacketPayload.Type(AnvilCraftWither.of("staff_timewarp_beam"))
            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, StaffTimewarpBeamPayload> =
                StreamCodec.composite(
                    BlockPos.STREAM_CODEC, StaffTimewarpBeamPayload::pos,
                    ::StaffTimewarpBeamPayload,
                )
        }

        override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
    }

    private val TIME_WARP_BEAM_HANDLER: IPayloadHandler<StaffTimewarpBeamPayload> =
        IPayloadHandler { payload, context ->
            context.enqueueWork { StaffBeamRenderer.showTimewarpBeam(payload.pos) }
        }

    fun register(modEventBus: IEventBus) {
        modEventBus.addListener(::registerPayloadHandlers)
    }

    private fun registerPayloadHandlers(event: RegisterPayloadHandlersEvent) {
        val registrar: PayloadRegistrar = event.registrar(AnvilCraftWither.MOD_ID)
        registrar.playToClient(
            StaffTimewarpBeamPayload.TYPE,
            StaffTimewarpBeamPayload.STREAM_CODEC,
            TIME_WARP_BEAM_HANDLER,
        )
    }

    fun sendTimewarpBeam(player: ServerPlayer, pos: BlockPos) {
        PacketDistributor.sendToPlayer(player, StaffTimewarpBeamPayload(pos))
    }
}

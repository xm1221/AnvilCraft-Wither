package cn.xm1221.AnvilCraftWither.client

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.init.AddonItems
import cn.xm1221.AnvilCraftWither.init.ModDataComponents
import cn.xm1221.AnvilCraftWither.init.StaffTransformEntityPayload
import cn.xm1221.AnvilCraftWither.items.WitherStaffInteractionConfig
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.EntityHitResult
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.network.PacketDistributor

/**
 * 法杖 Shift+右键蓄力（瞄准实体）：
 * 按住右键并潜行 0.5 秒（10 tick）后开始，每 0.5 秒上报一次转化意图（频率受服务端冷却限制）。
 */
@EventBusSubscriber(modid = AnvilCraftWither.MOD_ID, value = [Dist.CLIENT])
object StaffChargeHandler {
    private var holdTicks = 0

    @SubscribeEvent
    @JvmStatic
    fun onClientTick(event: ClientTickEvent.Post) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (mc.screen != null || !mc.options.keyShift.isDown || !mc.options.keyUse.isDown) {
            holdTicks = 0
            return
        }
        if (!player.mainHandItem.`is`(AddonItems.WITHER_STAFF.asItem())) {
            holdTicks = 0
            return
        }
        val hit = mc.hitResult
        if (hit !is EntityHitResult || !hit.entity.isAlive) {
            holdTicks = 0
            return
        }
        val config = player.mainHandItem.get(ModDataComponents.WITHER_STAFF_INTERACTION.get())
            ?: WitherStaffInteractionConfig()
        val delay = config.chargeDelay.coerceAtLeast(1)
        val interval = config.chargeInterval.coerceAtLeast(1)
        holdTicks++
        if (holdTicks >= delay && holdTicks % interval == 0) {
            PacketDistributor.sendToServer(StaffTransformEntityPayload(hit.entity.id))
        }
    }

    /** 蓄力期间阻止对实体的右键交互（喂食/骑乘等） */
    @SubscribeEvent
    @JvmStatic
    fun onEntityInteract(event: PlayerInteractEvent.EntityInteract) {
        val player = event.entity
        if (!player.level().isClientSide) return
        if (!player.mainHandItem.`is`(AddonItems.WITHER_STAFF.asItem())) return
        if (player.isShiftKeyDown) {
            event.setCanceled(true)
        }
    }
}

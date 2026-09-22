package cn.xm1221.AnvilCraftWither.client

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.init.ModDataComponents
import cn.xm1221.AnvilCraftWither.items.WitherStaff
import dev.dubhe.anvilcraft.client.init.ModRenderTypes
import dev.dubhe.anvilcraft.client.renderer.blockentity.CorruptedBeaconRenderer
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent

/**
 * 法杖光柱（客户端）：
 * - Shift+右键蓄力转化时，在目标实体位置渲染腐化信标同款光柱（持续整个蓄力过程）
 * - 时移成功后，在时移位置渲染同款光柱（短暂闪现，位置由服务端 S2C 通知，见 [cn.xm1221.AnvilCraftWither.init.ModPayloads]）
 */
@EventBusSubscriber(modid = AnvilCraftWither.MOD_ID, value = [Dist.CLIENT])
object StaffBeamRenderer {
    /** 光柱高度（格） */
    private const val BEAM_LENGTH = 32.0f

    /** 时移光柱持续时间（tick） */
    private const val TIMEWARP_BEAM_TICKS = 20L

    /** 待渲染的时移光柱：（方块位置，客户端世界 tick 到期时间） */
    private val timewarpBeams = ArrayList<Pair<BlockPos, Long>>()

    /** 服务端时移成功时调用（S2C payload handler）：记录一个短暂光柱 */
    @JvmStatic
    fun showTimewarpBeam(pos: BlockPos) {
        val level = Minecraft.getInstance().level ?: return
        timewarpBeams.add(pos to level.gameTime + TIMEWARP_BEAM_TICKS)
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return
        val camera = event.camera
        val pose = event.poseStack
        val buffers = mc.renderBuffers().bufferSource()
        val vc = buffers.getBuffer(ModRenderTypes.CORRUPTED_BEACON_BEAM)

        // 转化光柱：目标实体位置（蓄力期间）
        if (player.isUsingItem) {
            val stack = player.useItem
            if (stack.has(ModDataComponents.WITHER_STAFF_INTERACTION.get())) {
                val targetId = WitherStaff.transformTargets[player.uuid]
                val target = targetId?.let { level.getEntity(it) } as? LivingEntity
                if (target != null && target.isAlive) {
                    val pos = target.position()
                    CorruptedBeaconRenderer.renderBeam(
                        vc,
                        pose.last(),
                        (pos.x - camera.position.x).toFloat(),
                        (pos.y - camera.position.y).toFloat(),
                        (pos.z - camera.position.z).toFloat(),
                        BEAM_LENGTH,
                    )
                }
            }
        }

        // 时移光柱：时移位置（短暂闪现）
        val now = level.gameTime
        val iterator = timewarpBeams.iterator()
        while (iterator.hasNext()) {
            val (pos, until) = iterator.next()
            if (now > until) {
                iterator.remove()
                continue
            }
            CorruptedBeaconRenderer.renderBeam(
                vc,
                pose.last(),
                (pos.x + 0.5 - camera.position.x).toFloat(),
                (pos.y - camera.position.y).toFloat(),
                (pos.z + 0.5 - camera.position.z).toFloat(),
                BEAM_LENGTH,
            )
        }

        buffers.endBatch(ModRenderTypes.CORRUPTED_BEACON_BEAM)
    }
}

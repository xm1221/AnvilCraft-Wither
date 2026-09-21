package cn.xm1221.AnvilCraftWither.event

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.init.AddonItems
import cn.xm1221.AnvilCraftWither.init.ModDataComponents
import cn.xm1221.AnvilCraftWither.init.StaffTransformEntityPayload
import cn.xm1221.AnvilCraftWither.items.WitherStaffInteractionConfig
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes
import dev.dubhe.anvilcraft.recipe.transform.MobTransformInput
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.horse.AbstractHorse
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 法杖交互（服务端）：
 * - 单击左键方块：对目标方块执行「时移」（anvilcraft:block_processing 方块处理配方，目标方块下方逐层匹配 inputs）
 * - 长按左键实体（客户端上报）：按 anvilcraft:mob_transform / mob_transform_with_item 配方转化实体
 */
@EventBusSubscriber(modid = AnvilCraftWither.MOD_ID)
object WitherStaffInteractHandler {
    /** 玩家上次时移 / 转化时刻（world tick） */
    private val timewarpLast = ConcurrentHashMap<UUID, Long>()
    private val transformLast = ConcurrentHashMap<UUID, Long>()

    private fun interactionConfig(player: Player): WitherStaffInteractionConfig =
        player.mainHandItem.get(ModDataComponents.WITHER_STAFF_INTERACTION.get()) ?: WitherStaffInteractionConfig()

    /**
     * 方块时移：左键方块时尝试配方。
     * 不取消挖掘——物品 canAttackBlock=false 已保证方块不会被破坏，
     * 客户端挖掘包照常发出，服务端事件得以触发。
     */
    @SubscribeEvent
    @JvmStatic
    fun onLeftClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
        val player: Player = event.entity
        if (!player.mainHandItem.`is`(AddonItems.WITHER_STAFF.asItem())) return
        if (event.action != PlayerInteractEvent.LeftClickBlock.Action.START) return
        val level = player.level()
        if (level !is ServerLevel) return

        val now = level.gameTime
        if (now - (timewarpLast[player.uuid] ?: 0L) < interactionConfig(player).timewarpCooldown) return

        val pos = event.pos
        for (holder in level.server.recipeManager.getAllRecipesFor(ModRecipeTypes.BLOCK_PROCESSING_TYPE.get())) {
            val recipe = holder.value()
            var match = true
            for ((i, pred) in recipe.inputBlocks.withIndex()) {
                if (!pred.testWithoutEntity(level.getBlockState(pos.below(i)))) {
                    match = false
                    break
                }
            }
            if (!match) continue
            val entry = recipe.firstResultBlock.getResult(level) ?: continue
            val state = entry.key
            level.setBlock(pos, state, 3)
            entry.value?.let { nbt ->
                level.getBlockEntity(pos)?.loadWithComponents(nbt, level.registryAccess())
            }
            level.levelEvent(2001, pos, Block.getId(state))
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f)
            timewarpLast[player.uuid] = now
            event.setCanceled(true)
            return
        }
    }

    /** 实体转化：由客户端长按包触发 */
    fun handleTransformEntity(payload: StaffTransformEntityPayload, context: IPayloadContext) {
        val player = context.player() as? ServerPlayer ?: return
        val level = player.serverLevel()
        val entity = level.getEntity(payload.entityId) as? LivingEntity ?: return
        if (player.distanceToSqr(entity) > 64.0) return
        if (!player.mainHandItem.`is`(AddonItems.WITHER_STAFF.asItem())) return

        val now = level.gameTime
        if (now - (transformLast[player.uuid] ?: 0L) < interactionConfig(player).transformCooldown) return
        transformLast[player.uuid] = now

        transformEntity(entity, level)
    }

    private fun transformEntity(livingEntity: LivingEntity, level: ServerLevel) {
        val manager = level.server.recipeManager
        val withItem = manager.getRecipeFor(
            ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM_TYPE.get(),
            MobTransformWithItemRecipe.Input.of(livingEntity),
            level,
        )
        val plain = manager.getRecipeFor(
            ModRecipeTypes.MOB_TRANSFORM_TYPE.get(),
            MobTransformInput(livingEntity),
            level,
        )
        var result: Entity? = null
        var noItem = true
        if (withItem.isPresent) {
            result = withItem.get().value().apply(level.random, livingEntity, level)
            if (result != null) noItem = false
        }
        if (noItem && plain.isPresent) {
            result = plain.get().value().apply(level.random, livingEntity, level)
        }
        val resultEntity = result ?: return
        val vehicle = if (livingEntity.isPassenger) livingEntity.vehicle else null
        livingEntity.discard()
        if (resultEntity is AbstractHorse) resultEntity.setTamed(true)
        if (level.tryAddFreshEntityWithPassengers(resultEntity) && vehicle != null) {
            resultEntity.startRiding(vehicle)
        }
    }
}

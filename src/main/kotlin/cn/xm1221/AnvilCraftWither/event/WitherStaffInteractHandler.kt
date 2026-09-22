package cn.xm1221.AnvilCraftWither.event

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.init.ModDataComponents
import cn.xm1221.AnvilCraftWither.init.ModPayloads
import cn.xm1221.AnvilCraftWither.items.WitherStaffInteractionConfig
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity
import dev.dubhe.anvilcraft.init.block.ModBlockEntities
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * 法杖时移（服务端）：
 * - 左键炼药锅：锅内 / 上方物品实体走 anvilcraft:timewarp 配方（物品→物品），整组换算产出并掉落
 * - 左键鱼缸（anvilcraft:fish_tank）：鱼缸输入槽物品走 timewarp 配方，整组换算产出进鱼缸输出槽
 *
 * 时移成功由服务端权威判定，并向客户端发送 S2C 通知渲染光柱。
 */
@EventBusSubscriber(modid = AnvilCraftWither.MOD_ID)
object WitherStaffInteractHandler {
    /** 玩家上次时移时刻（world tick） */
    private val timewarpLast = ConcurrentHashMap<UUID, Long>()

    private fun interactionConfig(stack: ItemStack): WitherStaffInteractionConfig =
        stack.get(ModDataComponents.WITHER_STAFF_INTERACTION.get()) ?: WitherStaffInteractionConfig()

    @SubscribeEvent
    @JvmStatic
    fun onLeftClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
        if (event.action != PlayerInteractEvent.LeftClickBlock.Action.START) return
        val player: Player = event.entity
        val level = player.level()
        if (level !is ServerLevel) return
        val stack = player.mainHandItem
        // 组件驱动：任何带交互组件的物品都能触发时移
        if (!stack.has(ModDataComponents.WITHER_STAFF_INTERACTION.get())) return

        val pos = event.pos
        val now = level.gameTime
        if (now - (timewarpLast[player.uuid] ?: 0L) < interactionConfig(stack).timewarpCooldown) return

        val state = level.getBlockState(pos)
        val isCauldron = state.`is`(BlockTags.CAULDRONS)
        val isFishTank = level.getBlockEntity(pos) is FishTankBlockEntity
        val handled = when {
            // 鱼缸也在 #minecraft:cauldrons 标签里，必须先判鱼缸
            isFishTank -> timewarpItemsInFishTank(level, player, pos, stack)
            isCauldron -> timewarpItemsInCauldron(level, player, pos, stack)
            else -> false
        }
        if (!handled) return
        timewarpLast[player.uuid] = now
        // 服务端权威通知客户端在时移位置渲染光柱
        ModPayloads.sendTimewarpBeam(player as ServerPlayer, pos)
    }

    /**
     * 物品时移：左键炼药锅 → 锅内 / 上方物品实体匹配 anvilcraft:timewarp 配方 →
     * 物品实体移除，产出按输入数量整组换算（分堆弹出，每堆 ≤64）。
     */
    private fun timewarpItemsInCauldron(level: ServerLevel, player: Player, pos: BlockPos, stack: ItemStack): Boolean {
        val itemEntity = level.getEntitiesOfClass(
            ItemEntity::class.java,
            AABB.ofSize(pos.center, 2.0, 2.0, 2.0),
        ).firstOrNull { it.isAlive && !it.item.isEmpty } ?: return false
        val input = itemEntity.item
        for (holder in level.server.recipeManager.getAllRecipesFor(ModRecipeTypes.TIME_WARP_TYPE.get())) {
            val recipe = holder.value()
            if (recipe.inputItems.size != 1) continue
            if (!recipe.inputItems[0].test(input)) continue
            itemEntity.discard()
            dropTimewarpResults(level, pos, recipe.resultItems, input.count)
            level.levelEvent(2001, pos, Block.getId(level.getBlockState(pos)))
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f)
            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack))
            return true
        }
        return false
    }

    /**
     * 物品时移：左键鱼缸 → 鱼缸输入槽第一个可匹配物品走 timewarp 配方 →
     * 整组换算产出进鱼缸输出槽（放不下则掉落）。
     */
    private fun timewarpItemsInFishTank(level: ServerLevel, player: Player, pos: BlockPos, stack: ItemStack): Boolean {
        val tank = ModBlockEntities.FISH_TANK.get(level, pos).orElse(null) ?: return false
        // snapshot.2231+：具体输入处理器为 getInputHandler()（getInput() 返回 IItemHandler）
        val input = tank.inputHandler
        for (slot in 0 until input.slots) {
            val slotStack = input.getStackInSlot(slot)
            if (slotStack.isEmpty) continue
            for (holder in level.server.recipeManager.getAllRecipesFor(ModRecipeTypes.TIME_WARP_TYPE.get())) {
                val recipe = holder.value()
                if (recipe.inputItems.size != 1) continue
                if (!recipe.inputItems[0].test(slotStack)) continue
                val count = slotStack.count
                input.extractItem(slot, count, false)
                for (result in recipe.resultItems) {
                    val per = result.getResult(level)
                    if (per.isEmpty) continue
                    var remaining = per.count * count
                    while (remaining > 0) {
                        val n = min(remaining, 64)
                        val leftover = tank.insertRecipeOutput(per.copyWithCount(n))
                        if (!leftover.isEmpty) Block.popResource(level, pos.above(), leftover)
                        remaining -= n
                    }
                }
                level.levelEvent(2001, pos, Block.getId(level.getBlockState(pos)))
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f)
                stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack))
                return true
            }
        }
        return false
    }

    /** 按输入数量整组换算产出并分堆掉落（单堆上限 64） */
    private fun dropTimewarpResults(
        level: ServerLevel,
        pos: BlockPos,
        results: List<ChanceItemStack>,
        inputCount: Int,
    ) {
        for (result in results) {
            val per = result.getResult(level)
            if (per.isEmpty) continue
            var remaining = per.count * inputCount
            while (remaining > 0) {
                val n = min(remaining, 64)
                Block.popResource(level, pos.above(), per.copyWithCount(n))
                remaining -= n
            }
        }
    }
}

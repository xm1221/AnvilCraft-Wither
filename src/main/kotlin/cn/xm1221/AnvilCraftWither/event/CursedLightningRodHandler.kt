package cn.xm1221.AnvilCraftWither.event

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.block.CursedLightningRodBlock
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager
import dev.dubhe.anvilcraft.api.event.LightningBoltStrikeEvent
import dev.dubhe.anvilcraft.init.item.ModItems
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

/**
 * 诅咒引雷针的雷击结算。
 *
 * AnvilCraft 通过 `LightningBoltMixin` 在 `LightningBolt#powerLightningRod` 头部派发
 * [LightningBoltStrikeEvent]，因此**任何**闪电（本 mod 生成的、自然落雷、三叉戟引雷）
 * 命中引雷针时都会走到这里——判定只在「真正劈下来」的那一刻做一次：
 *
 * 1. 向 AnvilCraft 电力系统注入电荷（与铜块 / 避雷针被雷击相同的 32 电荷）；
 * 2. 按下方皇家钢数量 `n` 以 `n / 245` 的概率击毁引雷针，
 *    播放原版方块破坏粒子与音效，并掉落 1~3 个诅咒金锭。
 */
@EventBusSubscriber(modid = AnvilCraftWither.MOD_ID)
object CursedLightningRodHandler {
    /** 引雷针每次被雷击注入的电荷量（对齐 AnvilCraft 铜块 / 避雷针的数值） */
    private const val CHARGE_PER_STRIKE = 80.0

    @SubscribeEvent
    @JvmStatic
    fun onLightningStrike(event: LightningBoltStrikeEvent) {
        val level = event.level
        if (level !is ServerLevel) return

        // 闪电落点可能是引雷针本身，或其上下相邻格（细立柱不是完整方块），逐一尝试
        val rodPos = sequenceOf(event.pos, event.pos.below(), event.pos.above())
            .firstOrNull { level.getBlockState(it).block is CursedLightningRodBlock }
            ?: return
        val rodState = level.getBlockState(rodPos)

        val count = CursedLightningRodBlock.countRoyalSteelBelow(level, rodPos)
        if (count <= 0) return

        // 注入电荷（附近没有电荷收集器时不会产生效果）
        ChargeCollectorManager.charge(CHARGE_PER_STRIKE, level, rodPos)

        // 自毁判定：概率 = n / 245
        if (level.random.nextFloat() >= count / CursedLightningRodBlock.BREAK_DENOMINATOR) return

        // 原版方块破坏粒子 + 音效
        level.levelEvent(2001, rodPos, Block.getId(rodState))
        level.removeBlock(rodPos, false)

        // 掉落 1~3 个诅咒金锭。
        // 注意：掉落物必须设为无敌——闪电会在下一 tick 对落点附近的实体造成 5 点伤害，
        // 而 ItemEntity 的生命值正好是 5，未设无敌的掉落物会被这次雷击直接销毁（表现为「没有掉落」）。
        val drop = ItemEntity(
            level,
            rodPos.x + 0.5,
            rodPos.y + 0.5,
            rodPos.z + 0.5,
            ItemStack(ModItems.CURSED_GOLD_INGOT.get(), 1 + level.random.nextInt(3)),
        )
        drop.isInvulnerable = true
        level.addFreshEntity(drop)
    }
}

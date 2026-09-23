package cn.xm1221.AnvilCraftWither.block

import cn.xm1221.AnvilCraftWither.init.AddonBlockTags
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.ceil

/**
 * 诅咒引雷针（注册名 `cursed_bar`）。
 *
 * 造型为方块中心的竖立柱（模型纵向跨越方块上下），碰撞箱只取方块内的立柱。
 *
 * 引雷规则：
 * - 统计**下方 7×5×7 范围**（水平 7×7、竖直 5 格）内皇家钢系列方块
 *   （标签 `anvilcraft_wither:royal_steel_blocks`）的数量 `n`（最大 245）；
 * - 只要 `n > 0` 且引雷针上方露天，就**必定**生成闪电（不需要雷暴天气）；
 * - 引雷速率由数量决定：每 `ceil(135 / n)` tick 引雷一次（`n` 越大越频繁，最少 1 tick）；
 * - 自毁判定与电荷产出在闪电真正劈下来的那一刻结算，
 *   见 [cn.xm1221.AnvilCraftWither.event.CursedLightningRodHandler]。
 */
class CursedLightningRodBlock(properties: Properties) : Block(properties) {
    companion object {
        /** 与模型 x/z 6.5~9.5 对应的中心立柱碰撞箱 */
        private val SHAPE: VoxelShape = Block.box(6.0, 0.0, 6.0, 10.0, 18.0, 10.0)

        /** 不满足引雷条件时的重查间隔（tick） */
        private const val RECHECK_INTERVAL = 20

        /** 引雷速率基准：间隔 = ceil(BASE_TICKS / n) */
        const val BASE_TICKS = 135.0

        /** 自毁概率分母：概率 = n / BREAK_DENOMINATOR */
        const val BREAK_DENOMINATOR = 245f

        /** 统计范围：水平半径 3（共 7 格宽） */
        private const val RANGE_HORIZONTAL = 3

        /** 统计范围：向下深度 5（共 5 格高，对应 7×5×7 中的 y 轴 5 格） */
        private const val RANGE_DOWNWARD = 5

        /** 是否为皇家钢系列方块（标签 anvilcraft_wither:royal_steel_blocks，数据包可扩展） */
        private fun isRoyalSteel(state: BlockState): Boolean = state.`is`(AddonBlockTags.ROYAL_STEEL_BLOCKS)

        /** 统计下方 7×5×7 范围内的皇家钢方块数量 */
        fun countRoyalSteelBelow(level: Level, pos: BlockPos): Int {
            var count = 0
            for (dx in -RANGE_HORIZONTAL..RANGE_HORIZONTAL) {
                for (dz in -RANGE_HORIZONTAL..RANGE_HORIZONTAL) {
                    for (dy in 1..RANGE_DOWNWARD) {
                        if (isRoyalSteel(level.getBlockState(pos.offset(dx, -dy, dz)))) count++
                    }
                }
            }
            return count
        }
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
    ): VoxelShape = SHAPE

    override fun onPlace(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        oldState: BlockState,
        movedByPiston: Boolean,
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        if (level is ServerLevel) level.scheduleTick(pos, this, RECHECK_INTERVAL)
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        super.tick(state, level, pos, random)

        val count = countRoyalSteelBelow(level, pos)
        // 下方没有皇家钢，或上方不露天：不引雷，慢速重查
        if (count <= 0 || !level.canSeeSky(pos.above())) {
            level.scheduleTick(pos, this, RECHECK_INTERVAL)
            return
        }

        // 必定引雷：在引雷针上方生成闪电。
        // 电荷产出与自毁判定都在闪电真正劈下来时结算（LightningBoltStrikeEvent），
        // 见 CursedLightningRodHandler。
        val bolt = EntityType.LIGHTNING_BOLT.create(level)
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(pos.above()))
            level.addFreshEntity(bolt)
        }

        // 按数量决定下一次引雷间隔：ceil(135 / n)
        level.scheduleTick(pos, this, ceil(BASE_TICKS / count).toInt().coerceAtLeast(1))
    }
}

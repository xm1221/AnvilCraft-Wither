package cn.xm1221.AnvilCraftWither.items

import cn.xm1221.AnvilCraftWither.init.ModDataComponents
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.WitherSkull
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/**
 * 凋灵法杖的行为配置（Data Component）。
 *
 * 以组件形式挂在物品栈上，默认值由物品注册时写入；数据包 / 指令 / 合成可以覆盖任意字段。
 *
 * @param speed 骷髅头飞行速度（格/tick）
 * @param cooldown 两次发射的冷却（tick）
 * @param dangerous 是否发射强化（蓝色）凋灵之首
 */
data class WitherStaffConfig(
    val speed: Float = 1.5F,
    val cooldown: Int = 10,
    val dangerous: Boolean = false,
) {
    companion object {
        val CODEC: Codec<WitherStaffConfig> = RecordCodecBuilder.create { inst ->
            inst.group(
                Codec.FLOAT.optionalFieldOf("speed", 1.5F).forGetter { it.speed },
                Codec.INT.optionalFieldOf("cooldown", 10).forGetter { it.cooldown },
                Codec.BOOL.optionalFieldOf("dangerous", false).forGetter { it.dangerous },
            ).apply(inst) { speed, cooldown, dangerous ->
                WitherStaffConfig(speed, cooldown, dangerous)
            }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WitherStaffConfig> = StreamCodec.composite(
            ByteBufCodecs.FLOAT, { it.speed },
            ByteBufCodecs.INT, { it.cooldown },
            ByteBufCodecs.BOOL, { it.dangerous },
        ) { speed, cooldown, dangerous ->
            WitherStaffConfig(speed, cooldown, dangerous)
        }
    }
}

/**
 * 凋灵法杖的交互配置（Data Component）。
 *
 * 独立于发射配置，控制左键时移与 Shift+右键蓄力转化。
 *
 * @param timewarpCooldown 左键方块时移的冷却（tick）
 * @param transformCooldown Shift+右键蓄力实体转化的冷却（tick）
 * @param chargeDelay 蓄力转化前的按住时长（tick）
 * @param chargeInterval 蓄力期间两次上报的间隔（tick）
 */
data class WitherStaffInteractionConfig(
    val timewarpCooldown: Int = 60,
    val transformCooldown: Int = 20,
    val chargeDelay: Int = 10,
    val chargeInterval: Int = 10,
) {
    companion object {
        val CODEC: Codec<WitherStaffInteractionConfig> = RecordCodecBuilder.create { inst ->
            inst.group(
                Codec.INT.optionalFieldOf("timewarp_cooldown", 60).forGetter { it.timewarpCooldown },
                Codec.INT.optionalFieldOf("transform_cooldown", 20).forGetter { it.transformCooldown },
                Codec.INT.optionalFieldOf("charge_delay", 10).forGetter { it.chargeDelay },
                Codec.INT.optionalFieldOf("charge_interval", 10).forGetter { it.chargeInterval },
            ).apply(inst) { timewarpCooldown, transformCooldown, chargeDelay, chargeInterval ->
                WitherStaffInteractionConfig(timewarpCooldown, transformCooldown, chargeDelay, chargeInterval)
            }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WitherStaffInteractionConfig> = StreamCodec.composite(
            ByteBufCodecs.INT, { it.timewarpCooldown },
            ByteBufCodecs.INT, { it.transformCooldown },
            ByteBufCodecs.INT, { it.chargeDelay },
            ByteBufCodecs.INT, { it.chargeInterval },
        ) { timewarpCooldown, transformCooldown, chargeDelay, chargeInterval ->
            WitherStaffInteractionConfig(timewarpCooldown, transformCooldown, chargeDelay, chargeInterval)
        }
    }
}

class WitherStaff(properties: Properties) : Item(properties) {

    fun shot(world: ServerLevel, player: ServerPlayer, config: WitherStaffConfig) {
        val angle = player.lookAngle
        val pos = player.eyePosition
        val witherSkull = WitherSkull(world, player, angle)
        witherSkull.isDangerous = config.dangerous
        witherSkull.shoot(angle.x, angle.y, angle.z, config.speed, 0.0F)
        witherSkull.setPosRaw(pos.x + angle.x, pos.y + angle.y, pos.z + angle.z)
        world.addFreshEntity(witherSkull)
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        // 潜行右键让给「实体蓄力转化」，不发射骷髅
        if (player.isShiftKeyDown) return InteractionResultHolder.pass(player.getItemInHand(usedHand))
        if (level is ServerLevel) {
            val stack = player.getItemInHand(usedHand)
            val config = stack.get(ModDataComponents.WITHER_STAFF_CONFIG.get()) ?: WitherStaffConfig()
            shot(level, player as ServerPlayer, config)
            player.cooldowns.addCooldown(this, config.cooldown)
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide)
    }

    override fun canAttackBlock(state: BlockState, level: Level, pos: BlockPos, player: Player): Boolean {
        return false
    }
}

package cn.xm1221.AnvilCraftWither.items

import cn.xm1221.AnvilCraftWither.init.ModDataComponents
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
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
import net.minecraft.world.level.Level

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
        if (level is ServerLevel) {
            val stack = player.getItemInHand(usedHand)
            val config = stack.get(ModDataComponents.WITHER_STAFF_CONFIG.get()) ?: WitherStaffConfig()
            shot(level, player as ServerPlayer, config)
            player.cooldowns.addCooldown(this, config.cooldown)
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide)
    }
}

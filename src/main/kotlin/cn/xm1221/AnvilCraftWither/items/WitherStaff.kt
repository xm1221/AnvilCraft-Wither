package cn.xm1221.AnvilCraftWither.items

import cn.xm1221.AnvilCraftWither.init.ModDataComponents
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes
import dev.dubhe.anvilcraft.recipe.transform.MobTransformInput
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.horse.AbstractHorse
import net.minecraft.world.entity.animal.horse.SkeletonHorse
import net.minecraft.world.entity.animal.horse.ZombieHorse
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.WitherSkull
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

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
    val cooldown: Int = 45,
    val dangerous: Boolean = false,
) {
    companion object {
        val CODEC: Codec<WitherStaffConfig> = RecordCodecBuilder.create { inst ->
            inst.group(
                Codec.FLOAT.optionalFieldOf("speed", 1.5F).forGetter { it.speed },
                Codec.INT.optionalFieldOf("cooldown", 45).forGetter { it.cooldown },
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
 * 独立于发射配置，控制左键炼药锅时移与 Shift+右键蓄力转化。
 *
 * @param timewarpCooldown 左键炼药锅时移的冷却（tick）
 * @param transformCooldown 两次转化的冷却（tick）
 * @param chargeDelay 蓄力转化前的等待时长（tick）
 * @param chargeInterval 保留字段（未使用）
 */
data class WitherStaffInteractionConfig(
    val timewarpCooldown: Int = 60,
    val transformCooldown: Int = 20,
    val chargeDelay: Int = 40,
    val chargeInterval: Int = 10,
) {
    companion object {
        val CODEC: Codec<WitherStaffInteractionConfig> = RecordCodecBuilder.create { inst ->
            inst.group(
                Codec.INT.optionalFieldOf("timewarp_cooldown", 60).forGetter { it.timewarpCooldown },
                Codec.INT.optionalFieldOf("transform_cooldown", 20).forGetter { it.transformCooldown },
                Codec.INT.optionalFieldOf("charge_delay", 40).forGetter { it.chargeDelay },
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

    /** 近战属性（参考 AnvilHammerItem）：攻击伤害 +5，攻速 -3 */
    private val defaultModifiers: ItemAttributeModifiers = ItemAttributeModifiers.builder()
        .add(
            Attributes.ATTACK_DAMAGE,
            AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND,
        )
        .add(
            Attributes.ATTACK_SPEED,
            AttributeModifier(BASE_ATTACK_SPEED_ID, -3.0, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND,
        )
        .build()

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers = defaultModifiers

    /** 法杖无法破坏方块：左键挖掘由时移接管 */
    override fun canAttackBlock(state: BlockState, level: Level, pos: BlockPos, player: Player): Boolean = false

    // ============ 右键（空气 / 方块）：发射凋灵之首 ============

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        // 仅 Shift+右键（空气 / 方块）发射凋灵之首；普通右键让给实体蓄力转化
        if (!player.isShiftKeyDown) return InteractionResultHolder.pass(stack)
        if (level is ServerLevel) {
            val config = stack.get(ModDataComponents.WITHER_STAFF_CONFIG.get()) ?: WitherStaffConfig()
            shot(level, player as ServerPlayer, config, stack)
            player.cooldowns.addCooldown(this, config.cooldown)
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    private fun shot(world: ServerLevel, player: ServerPlayer, config: WitherStaffConfig, stack: ItemStack) {
        val angle = player.lookAngle
        val pos = player.eyePosition
        val witherSkull = WitherSkull(world, player, angle)
        witherSkull.isDangerous = config.dangerous
        witherSkull.shoot(angle.x, angle.y, angle.z, config.speed, 0.0F)
        witherSkull.setPosRaw(pos.x + angle.x, pos.y + angle.y, pos.z + angle.z)
        world.addFreshEntity(witherSkull)
        world.playSound(
            null, player.x, player.y, player.z,
            SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f,
        )
        stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack))
    }

    // ============ Shift+右键实体：蓄力转化 ============

    override fun interactLivingEntity(
        stack: ItemStack,
        player: Player,
        interactionTarget: LivingEntity,
        usedHand: InteractionHand,
    ): InteractionResult {
        // Shift+右键实体 = 发射凋灵之首（不蓄力）
        if (player.isShiftKeyDown) {
            if (player.level() is ServerLevel) {
                val config = stack.get(ModDataComponents.WITHER_STAFF_CONFIG.get()) ?: WitherStaffConfig()
                shot(player.level() as ServerLevel, player as ServerPlayer, config, stack)
                player.cooldowns.addCooldown(this, config.cooldown)
            }
            return InteractionResult.CONSUME
        }
        // 普通右键实体 = 蓄力转化
        if (player.level() is ServerLevel) {
            transformTargets[player.uuid] = interactionTarget.id
            useTicks.remove(player.uuid)
        }
        player.startUsingItem(usedHand)
        player.level().playSound(
            null, player.x, player.y, player.z,
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.0f,
        )
        return InteractionResult.CONSUME
    }

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int = 72000

    override fun getUseAnimation(stack: ItemStack): UseAnim = UseAnim.NONE

    override fun onUseTick(level: Level, livingEntity: LivingEntity, stack: ItemStack, remainingUseDuration: Int) {
        if (level !is ServerLevel) return
        val player = livingEntity as? ServerPlayer ?: return
        val config = stack.get(ModDataComponents.WITHER_STAFF_INTERACTION.get()) ?: WitherStaffInteractionConfig()

        // 蓄力期间按下 Shift（变成发射意图）→ 停止蓄力
        if (player.isShiftKeyDown) return stopUsing(player)
        val targetId = transformTargets[player.uuid] ?: return stopUsing(player)
        val target = level.getEntity(targetId) as? LivingEntity ?: return stopUsing(player)
        if (!target.isAlive || player.distanceToSqr(target) > 64.0) return stopUsing(player)

        // 与腐化信标一致的凋零 debuff（Wither I，120 tick）
        target.addEffect(MobEffectInstance(MobEffects.WITHER, 120, 0, true, true))
        // 蓄力期间持续消耗耐久
        stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack))

        // 蓄力等待：满 chargeDelay 后转化一次
        val ticks = (useTicks[player.uuid] ?: 0) + 1
        useTicks[player.uuid] = ticks
        if (ticks % 20 == 0) {
            level.playSound(
                null, player.x, player.y, player.z,
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.0f,
            )
        }
        if (ticks < max(config.chargeDelay, 1)) return

        // 转化冷却
        val now = level.gameTime
        if (now - (transformLast[player.uuid] ?: 0L) < max(config.transformCooldown, 1)) return

        transformLast[player.uuid] = now
        transformEntity(target, level)
        // 转化成功后给物品加冷却
        player.cooldowns.addCooldown(this, max(config.transformCooldown, 1))
        stopUsing(player)
    }

    override fun releaseUsing(stack: ItemStack, level: Level, livingEntity: LivingEntity, timeLeft: Int) {
        if (livingEntity is ServerPlayer) {
            transformTargets.remove(livingEntity.uuid)
            transformLast.remove(livingEntity.uuid)
            useTicks.remove(livingEntity.uuid)
        }
        super.releaseUsing(stack, level, livingEntity, timeLeft)
    }

    private fun stopUsing(player: ServerPlayer) {
        transformTargets.remove(player.uuid)
        transformLast.remove(player.uuid)
        useTicks.remove(player.uuid)
        player.stopUsingItem()
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
        if (resultEntity is ZombieHorse || resultEntity is SkeletonHorse) {
            (resultEntity as AbstractHorse).setTamed(true)
        }
        if (level.tryAddFreshEntityWithPassengers(resultEntity) && vehicle != null) {
            resultEntity.startRiding(vehicle)
        }
    }

    companion object {
        /** 玩家 → 蓄力转化目标实体 id（客户端 / 服务端各自持有，光柱渲染用） */
        @JvmField
        val transformTargets = ConcurrentHashMap<UUID, Int>()

        private val transformLast = ConcurrentHashMap<UUID, Long>()
        private val useTicks = ConcurrentHashMap<UUID, Int>()
    }
}

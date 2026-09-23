package cn.xm1221.AnvilCraftWither.block

import dev.dubhe.anvilcraft.block.plate.PowerLevelPressurePlateBlock
import net.minecraft.world.Container
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse
import net.minecraft.world.entity.npc.AbstractVillager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.phys.AABB

/**
 * 诅咒金压力板。
 *
 * 红石信号强度由踩在板上的生物**携带物品的满载程度**决定：
 * 取范围内所有生物中「已占用格子比例」的最大值映射到 0~15（携带得越满信号越强）。
 *
 * 统计口径：
 * - 有物品栏的生物（玩家、村民、带箱子的马类）→ 按物品栏格子统计
 *   （玩家的物品栏含盔甲与副手，因此穿装备同样计入）；
 * - 没有物品栏的一般生物（僵尸、骷髅、牛羊等）→ 按 6 个装备格统计
 *   （主手 / 副手 / 头盔 / 胸甲 / 护腿 / 靴子），所以手持物品或穿装备都能发出信号。
 *
 * 比例算法与 AnvilCraft 的 `PlayerInventoryPressurePlateBlock` 一致。
 */
class CursedGoldPressurePlateBlock(properties: Properties) :
    PowerLevelPressurePlateBlock(BlockSetType.GOLD, properties) {

    override fun getEntityClasses(): Set<Class<out Entity>> = setOf(LivingEntity::class.java)

    override fun getSignalStrength(level: Level, box: AABB, entityClasses: Set<Class<out Entity>>): Int {
        var maxOccupiedPercent = 0F
        val entities = level.getEntitiesOfClass(
            LivingEntity::class.java,
            box,
            EntitySelector.NO_SPECTATORS.and { !it.isIgnoringBlockTriggers },
        )
        for (entity in entities) {
            maxOccupiedPercent = maxOf(maxOccupiedPercent, occupiedPercent(entity))
        }
        return (maxOccupiedPercent * 15).toInt().coerceIn(0, 15)
    }

    companion object {
        /** 一般生物参与统计的装备格：主手 / 副手 / 四件盔甲 */
        private val EQUIPMENT_SLOTS = listOf(
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
        )

        /** 取实体的物品栏：玩家 / 带箱子的马类 / 村民，其余返回 null */
        private fun containerOf(entity: LivingEntity): Container? = when (entity) {
            is Player -> entity.inventory
            is AbstractChestedHorse -> entity.inventory
            is AbstractVillager -> entity.inventory
            else -> null
        }

        /** 实体携带物品的占用比例（0~1）：有物品栏按物品栏算，否则按装备格算 */
        private fun occupiedPercent(entity: LivingEntity): Float {
            val container = containerOf(entity)
            if (container != null && container.containerSize > 0) {
                var occupied = 0
                for (slot in 0 until container.containerSize) {
                    if (!container.getItem(slot).isEmpty) occupied++
                }
                return occupied.toFloat() / container.containerSize
            }
            var occupied = 0
            for (slot in EQUIPMENT_SLOTS) {
                if (!entity.getItemBySlot(slot).isEmpty) occupied++
            }
            return occupied.toFloat() / EQUIPMENT_SLOTS.size
        }
    }
}

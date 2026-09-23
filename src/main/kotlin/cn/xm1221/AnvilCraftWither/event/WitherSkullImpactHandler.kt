package cn.xm1221.AnvilCraftWither.event

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.init.ModRecipeTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.projectile.WitherSkull
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent

/**
 * 凋灵之首轰击处理：凋灵之首命中方块时查找 [ModRecipeTypes.WITHER_TRANSFORMATION_TYPE] 配方并执行转化。
 *
 * 配方按 [WitherSkull.isDangerous] 区分普通（黑色）与危险（蓝色）凋灵之首，只有类型一致才会触发。
 */
@EventBusSubscriber(modid = AnvilCraftWither.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object WitherSkullImpactHandler {

    @SubscribeEvent
    @JvmStatic
    fun onProjectileImpact(event: ProjectileImpactEvent) {
        val projectile = event.projectile as? WitherSkull ?: return
        val hitResult = event.rayTraceResult as? BlockHitResult ?: return
        val level = projectile.level() as? ServerLevel ?: return
        val anchor = hitResult.blockPos

        val recipes = level.recipeManager.getAllRecipesFor(ModRecipeTypes.WITHER_TRANSFORMATION_TYPE.get())
        for (holder in recipes) {
            val recipe = holder.value()
            if (!recipe.findMatch(level, anchor, projectile.isDangerous)) continue
            recipe.apply(level, anchor)
            break
        }
    }
}

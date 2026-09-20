package cn.xm1221.AnvilCraftWither.event

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.init.ModRecipeTypes
import cn.xm1221.AnvilCraftWither.recipe.WitherTransformationRecipe
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.projectile.WitherSkull
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent

/**
 * 凋灵之首命中处理：命中方块时查找 [ModRecipeTypes.WITHER_TRANSFORMATION_TYPE] 配方并执行转化。
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
            if (!recipe.findMatch(level, anchor)) continue
            if (recipe.mode == WitherTransformationRecipe.Mode.ANCHOR_NO_EXPLOSION) {
                // 抑制默认爆炸，并手动消散凋灵之首
                event.setCanceled(true)
                projectile.discard()
            }
            recipe.apply(level, anchor)
            break
        }
    }
}

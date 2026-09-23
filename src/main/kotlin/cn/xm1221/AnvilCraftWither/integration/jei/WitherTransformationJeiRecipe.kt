package cn.xm1221.AnvilCraftWither.integration.jei

import cn.xm1221.AnvilCraftWither.recipe.WitherTransformationRecipe
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder

/**
 * 凋灵之首轰击配方的 JEI 展示数据：从配方中提取代表方块与元信息。
 */
class WitherTransformationJeiRecipe(
    val id: ResourceLocation,
    val mode: WitherTransformationRecipe.Mode,
    val input: ItemStack,
    val output: ItemStack,
    val radius: Int,
    val dangerous: Boolean,
    val recipe: WitherTransformationRecipe,
) {
    companion object {
        /** 从配方持有者构建展示数据；无法提取代表方块时返回 null */
        fun from(holder: RecipeHolder<WitherTransformationRecipe>): WitherTransformationJeiRecipe? {
            val r = holder.value()
            val inputBlock = r.input.blocks.firstOrNull()?.value() ?: return null
            val outputBlock = r.result.state.block
            return WitherTransformationJeiRecipe(
                id = holder.id(),
                mode = r.mode,
                input = ItemStack(inputBlock),
                output = ItemStack(outputBlock),
                radius = r.radius,
                dangerous = r.dangerous,
                recipe = r,
            )
        }
    }
}

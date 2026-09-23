package cn.xm1221.AnvilCraftWither.data.recipe

import cn.xm1221.AnvilCraftWither.recipe.WitherTransformationRecipe
import cn.xm1221.AnvilCraftWither.recipe.WitherTransformationRecipe.Mode
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Recipe

/**
 * [WitherTransformationRecipe] 的 datagen 构建器。
 *
 * 输出 JSON 结构与 `wither_transformation` codec 完全一致：
 * `{type, mode, input, result, radius?, dangerous?}`。
 */
class WitherTransformationRecipeBuilder private constructor(
    private val mode: Mode,
    private val input: BlockStatePredicate,
    private val result: ChanceBlockState,
    private val radius: Int,
    private val dangerous: Boolean,
) {

    fun build(): WitherTransformationRecipe =
        WitherTransformationRecipe(mode, input, result, radius, dangerous)

    fun save(output: RecipeOutput, id: ResourceLocation) {
        output.accept(id, build() as Recipe<*>, null)
    }

    companion object {
        /** 单方块转化：只转化凋灵之首命中的那一格 */
        fun anchor(
            input: BlockStatePredicate,
            result: ChanceBlockState,
            dangerous: Boolean = false,
        ): WitherTransformationRecipeBuilder =
            WitherTransformationRecipeBuilder(Mode.ANCHOR, input, result, 0, dangerous)

        /** 范围转化：命中点半径内所有匹配的同种方块逐个转化 */
        fun area(
            input: BlockStatePredicate,
            result: ChanceBlockState,
            radius: Int,
            dangerous: Boolean = false,
        ): WitherTransformationRecipeBuilder =
            WitherTransformationRecipeBuilder(Mode.AREA, input, result, radius, dangerous)
    }
}

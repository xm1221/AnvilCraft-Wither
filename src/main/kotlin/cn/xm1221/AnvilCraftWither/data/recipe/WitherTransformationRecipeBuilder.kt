package cn.xm1221.AnvilCraftWither.data.recipe

import cn.xm1221.AnvilCraftWither.recipe.BlockPattern
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
 * `{type, mode, input, pattern?, result, structure_result?, radius?}`，
 * 其中 pattern 为嵌套对象 `{layers, symbols, anchor?, rotate?}`。
 */
class WitherTransformationRecipeBuilder private constructor(
    private val mode: Mode,
    private val input: BlockStatePredicate,
    private val result: ChanceBlockState,
) {
    private var pattern: BlockPattern? = null
    private val structureResult = mutableMapOf<Char, ChanceBlockState>()
    private var radius: Int = 0

    /** 设置多方块结构模式（可选） */
    fun pattern(pattern: BlockPattern): WitherTransformationRecipeBuilder = apply { this.pattern = pattern }

    /** 设置整体结构转化：符号 [symbol] 命中的非锚点方块 → [target] */
    fun structureResult(symbol: Char, target: ChanceBlockState): WitherTransformationRecipeBuilder =
        apply { this.structureResult[symbol] = target }

    /** 设置 AREA 模式的作用半径 */
    fun radius(radius: Int): WitherTransformationRecipeBuilder = apply { this.radius = radius }

    fun build(): WitherTransformationRecipe =
        WitherTransformationRecipe(mode, input, pattern, result, structureResult, radius)

    fun save(output: RecipeOutput, id: ResourceLocation) {
        output.accept(id, build() as Recipe<*>, null)
    }

    companion object {
        /** 锚点转化：凋灵之首命中格直接转化，爆炸照常 */
        fun anchor(input: BlockStatePredicate, result: ChanceBlockState): WitherTransformationRecipeBuilder =
            WitherTransformationRecipeBuilder(Mode.ANCHOR, input, result)

        /** 锚点转化且抑制爆炸（安全转化） */
        fun anchorNoExplosion(input: BlockStatePredicate, result: ChanceBlockState): WitherTransformationRecipeBuilder =
            WitherTransformationRecipeBuilder(Mode.ANCHOR_NO_EXPLOSION, input, result)

        /** 区域转化：以命中点为球心 [radius] 半径内所有匹配方块逐个转化 */
        fun area(input: BlockStatePredicate, result: ChanceBlockState, radius: Int): WitherTransformationRecipeBuilder =
            WitherTransformationRecipeBuilder(Mode.AREA, input, result).radius(radius)
    }
}

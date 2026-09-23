package cn.xm1221.AnvilCraftWither.data.recipe

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

/**
 * [cn.xm1221.AnvilCraftWither.recipe.WitherTransformationRecipe] 的 datagen 配方内容。
 *
 * 只有两类配方：单方块转化（ANCHOR）与范围内同种方块转化（AREA）。
 * `dangerous = true` 的配方需要危险（蓝色）凋灵之首，其余为普通（黑色）凋灵之首。
 */
object WitherTransformationRecipeHandler {

    /** ChanceBlockState.of 需要 Supplier<Block> */
    private fun cbs(block: Block): ChanceBlockState = ChanceBlockState.of { block }

    fun init(provider: RegistrumRecipeProvider) {
        // ---------- 单方块转化（普通凋灵之首） ----------
        WitherTransformationRecipeBuilder.anchor(
            BlockStatePredicate.builder().of(Blocks.STONE).build(),
            cbs(Blocks.OBSIDIAN),
        ).save(provider, AnvilCraftWither.of("wither_transformation/stone_to_obsidian"))

        WitherTransformationRecipeBuilder.anchor(
            BlockStatePredicate.builder().of(Blocks.COBBLESTONE).build(),
            cbs(Blocks.CRYING_OBSIDIAN),
        ).save(provider, AnvilCraftWither.of("wither_transformation/cobblestone_to_crying_obsidian"))

        WitherTransformationRecipeBuilder.anchor(
            BlockStatePredicate.builder().of(Blocks.SAND).build(),
            cbs(Blocks.SANDSTONE),
        ).save(provider, AnvilCraftWither.of("wither_transformation/sand_to_sandstone"))

        // ---------- 单方块转化（危险凋灵之首） ----------
        WitherTransformationRecipeBuilder.anchor(
            BlockStatePredicate.builder().of(Blocks.OBSIDIAN).build(),
            cbs(Blocks.CRYING_OBSIDIAN),
            dangerous = true,
        ).save(provider, AnvilCraftWither.of("wither_transformation/obsidian_to_crying_obsidian"))

        // ---------- 范围内同种方块转化 ----------
        WitherTransformationRecipeBuilder.area(
            BlockStatePredicate.builder().of(Blocks.SAND).build(),
            cbs(Blocks.SANDSTONE),
            radius = 2,
        ).save(provider, AnvilCraftWither.of("wither_transformation/area_sand_to_sandstone"))

        WitherTransformationRecipeBuilder.area(
            BlockStatePredicate.builder().of(Blocks.GRAVEL).build(),
            cbs(Blocks.COARSE_DIRT),
            radius = 1,
        ).save(provider, AnvilCraftWither.of("wither_transformation/area_gravel_to_coarse_dirt"))
    }
}

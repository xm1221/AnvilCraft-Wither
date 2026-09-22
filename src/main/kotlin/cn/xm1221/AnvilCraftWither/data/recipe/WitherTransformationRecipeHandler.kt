package cn.xm1221.AnvilCraftWither.data.recipe

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.recipe.BlockPattern
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState
import net.minecraft.world.level.block.Blocks

/**
 * [cn.xm1221.AnvilCraftWither.recipe.WitherTransformationRecipe] 的 datagen 配方内容。
 */
object WitherTransformationRecipeHandler {

    /** ChanceBlockState.of 需要 Supplier<Block> */
    private fun cbs(block: net.minecraft.world.level.block.Block): ChanceBlockState = ChanceBlockState.of { block }

    fun init(provider: RegistrumRecipeProvider) {
        // ---------- ANCHOR：凋灵之首直接命中转化（爆炸照常） ----------
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

        // ---------- AREA：命中点半径内所有匹配方块逐个转化（爆炸照常） ----------
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

        // ---------- ANCHOR_NO_EXPLOSION：安全转化（抑制凋灵之首爆炸） ----------
        WitherTransformationRecipeBuilder.anchorNoExplosion(
            BlockStatePredicate.builder().of(Blocks.OBSIDIAN).build(),
            cbs(Blocks.CRYING_OBSIDIAN),
        ).save(provider, AnvilCraftWither.of("wither_transformation/obsidian_to_crying_obsidian"))

        // ---------- ANCHOR_NO_EXPLOSION + 多方块结构（金块祭坛） ----------
        // 两层结构：底层 3x3 金块环（四角为草方块），二层中心金块为锚点。
        // 命中中心金块 → 中心金块转石头；四个草方块 → 哭泣黑曜石；不爆炸。
        val goldShrine = BlockPattern(
            layers = listOf(
                listOf("GSG", "S S", "GSG"),
                listOf("   ", " G ", "   "),
            ),
            symbols = mapOf(
                'G' to BlockStatePredicate.builder().of(Blocks.GOLD_BLOCK).build(),
                'S' to BlockStatePredicate.builder().of(Blocks.GRASS_BLOCK).build(),
            ),
            anchorOverride = null, // 几何中心 (1,1,1) = 二层中心金块
            rotate = true,
        )
        WitherTransformationRecipeBuilder.anchorNoExplosion(
            BlockStatePredicate.builder().of(Blocks.GOLD_BLOCK).build(),
            cbs(Blocks.STONE),
        )
            .pattern(goldShrine)
            .structureResult('S', cbs(Blocks.CRYING_OBSIDIAN))
            .save(provider, AnvilCraftWither.of("wither_transformation/gold_shrine"))
    }
}

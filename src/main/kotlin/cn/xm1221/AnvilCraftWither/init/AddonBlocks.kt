package cn.xm1221.AnvilCraftWither.init

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.AnvilCraftWither.Companion.REGISTRUM
import cn.xm1221.AnvilCraftWither.block.CursedGoldPressurePlateBlock
import cn.xm1221.AnvilCraftWither.block.CursedLightningRodBlock
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry
import dev.dubhe.anvilcraft.block.item.CursedBlockItem
import dev.dubhe.anvilcraft.block.plate.PowerLevelPressurePlateBlock
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.material.PushReaction
import net.neoforged.neoforge.client.model.generators.ConfiguredModel
import net.neoforged.neoforge.client.model.generators.ModelFile

class AddonBlocks {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    companion object {
        init {
            REGISTRUM.defaultCreativeTab(AddonItemGroups.ADDON_ITEMS.key)
        }

        /**
         * 诅咒金压力板。
         *
         * 信号强度由踩踏范围内带物品栏生物的物品栏占用比例决定（见 [CursedGoldPressurePlateBlock]），
         * 因此方块状态使用 AnvilCraft 可变信号压力板的 `power`(0~15) 而非原版 `powered`。
         *
         * 纹理复用 AnvilCraft 的诅咒金块（anvilcraft:block/cursed_gold_block），
         * 方块模型使用原版父级模型 minecraft:block/pressure_plate_up / pressure_plate_down。
         *
         * 物品形式使用 AnvilCraft 的 [CursedBlockItem]（实现 ICursed：携带时施加异常效果）。
         *
         * 注意：模型与 blockstate 写在 src/main/resources（手写 JSON），
         * 因为 datagen 的 ExistingFileHelper 不含 AnvilCraft 资源包，跨 mod 纹理引用会校验失败。
         */
        val CURSED_GOLD_PRESSURE_PLATE: BlockEntry<CursedGoldPressurePlateBlock> = REGISTRUM
            .block("cursed_gold_pressure_plate") { properties ->
                CursedGoldPressurePlateBlock(
                    properties
                        .forceSolidOn()
                        .noCollission()
                        .strength(0.5f)
                        .pushReaction(PushReaction.DESTROY),
                )
            }
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.PRESSURE_PLATES)
            .blockstate { ctx, provider ->
                // 引用手写模型生成 blockstate（power=0 用上抬模型，1~15 用下压模型）
                val helper = provider.models().existingFileHelper
                val up = ModelFile.ExistingModelFile(
                    AnvilCraftWither.of("block/cursed_gold_pressure_plate"),
                    helper,
                )
                val down = ModelFile.ExistingModelFile(
                    AnvilCraftWither.of("block/cursed_gold_pressure_plate_down"),
                    helper,
                )
                provider.getVariantBuilder(ctx.get()).forAllStates { state ->
                    ConfiguredModel.builder()
                        .modelFile(if (state.getValue(PowerLevelPressurePlateBlock.POWER) == 0) up else down)
                        .build()
                }
            }
            .item { block, properties -> CursedBlockItem(block, properties) }
            .model { _, _ -> } // 物品图标使用手写模型
            .tag(ItemTags.PIGLIN_LOVED)
            .build()
            .register()

        /**
         * 凋灵骷髅树脂块：完整立方体。
         *
         * 模型（models/block/wither_skull_resin_block.json）引用 AnvilCraft 树脂块
         * 与 mod 内凋灵头颅贴图；blockstate / 物品模型为手写 JSON（跨 mod 纹理）。
         */
        val WITHER_SKULL_RESIN_BLOCK: BlockEntry<Block> = REGISTRUM
            .block("wither_skull_resin_block") { properties ->
                Block(properties.strength(1.0f))
            }
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate { ctx, provider ->
                val model = ModelFile.ExistingModelFile(
                    AnvilCraftWither.of("block/wither_skull_resin_block"),
                    provider.models().existingFileHelper,
                )
                provider.getVariantBuilder(ctx.get())
                    .forAllStates { ConfiguredModel.builder().modelFile(model).build() }
            }
            .item()
            .model { _, _ -> } // 物品图标使用手写模型
            .build()
            .register()

        /**
         * 诅咒引雷针（注册名 `cursed_bar`）：方块中心的细立柱（非完整方块，无遮挡面）。
         *
         * 模型 models/block/cursed_bar.json 由诅咒木棍贴图构成，纵向跨越方块上下；
         * 立于皇家钢系列方块上时可按下方钢块层数引雷（见 [CursedLightningRodBlock]）。
         * 物品形式为 [CursedBlockItem]（诅咒物品）。
         */
        val CURSED_BAR: BlockEntry<out Block> = REGISTRUM
            .block("cursed_bar") { properties ->
                CursedLightningRodBlock(properties.noOcclusion().strength(1.0f))
            }
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate { ctx, provider ->
                val model = ModelFile.ExistingModelFile(
                    AnvilCraftWither.of("block/cursed_bar"),
                    provider.models().existingFileHelper,
                )
                provider.getVariantBuilder(ctx.get())
                    .forAllStates { ConfiguredModel.builder().modelFile(model).build() }
            }
            .item { block, properties -> CursedBlockItem(block, properties) }
            .model { _, _ -> } // 物品图标使用手写模型
            .build()
            .register()

        fun register() {
        }
    }
}

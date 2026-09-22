package cn.xm1221.AnvilCraftWither.init

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block

/**
 * 本 mod 自定义方块标签。
 *
 * 标签内容写在 `data/anvilcraft_wither/tags/block/` 下的 json 文件里，可直接引用其它 mod 的方块，
 * 也允许数据包 / 整合包往标签里追加方块（无需改代码）。
 */
object AddonBlockTags {
    /** 皇家钢系列方块（默认含 AnvilCraft 的 6 种皇家钢变体） */
    val ROYAL_STEEL_BLOCKS: TagKey<Block> =
        TagKey.create(Registries.BLOCK, AnvilCraftWither.of("royal_steel_blocks"))
}

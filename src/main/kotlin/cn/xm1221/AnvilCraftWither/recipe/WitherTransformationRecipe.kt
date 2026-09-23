package cn.xm1221.AnvilCraftWither.recipe

import cn.xm1221.AnvilCraftWither.init.ModRecipeTypes
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

/**
 * 凋灵之首轰击配方：凋灵之首命中 [input] 匹配的方块时，将方块按 [result] 转化。
 *
 * - [mode]：命中处理模式
 *   - [Mode.ANCHOR]：**单方块转化**，只转化命中的那一格；
 *   - [Mode.AREA]：**范围内同种方块转化**，以命中点为球心、[radius] 为半径的立方体内，
 *     所有匹配 [input] 的方块逐个转化。
 * - [input]：目标方块谓词（area 模式下为区域内每个方块的匹配谓词）；
 * - [result]：转化结果（含概率与方块实体 NBT）；
 * - [dangerous]：需要的凋灵之首类型——`false` 为普通凋灵之首（黑色），
 *   `true` 为危险凋灵之首（蓝色）；只有类型一致的凋灵之首才能触发该配方。
 */
class WitherTransformationRecipe(
    val mode: Mode,
    val input: BlockStatePredicate,
    val result: ChanceBlockState,
    val radius: Int,
    val dangerous: Boolean,
) : Recipe<RecipeInput> {

    enum class Mode(private val id: String) {
        ANCHOR("anchor"),
        AREA("area");

        companion object {
            val CODEC: Codec<Mode> = Codec.STRING.xmap(
                { id -> entries.firstOrNull { it.id == id } ?: error("Unknown wither transformation mode: $id") },
                { it.id },
            )
            val STREAM_CODEC: StreamCodec<ByteBuf, Mode> = StreamCodec.of(
                { buf, m -> buf.writeInt(m.ordinal) },
                { buf -> entries[buf.readInt()] },
            )
        }
    }

    /** 命中判定：凋灵之首类型一致，且锚点方块匹配 [input] */
    fun findMatch(level: Level, anchor: BlockPos, skullDangerous: Boolean): Boolean =
        dangerous == skullDangerous && input.testWithoutEntity(level.getBlockState(anchor))

    /** 执行转化；返回是否至少发生了一次方块转化 */
    fun apply(level: ServerLevel, anchor: BlockPos): Boolean =
        if (mode == Mode.AREA) applyArea(level, anchor) else applyAnchor(level, anchor)

    /** 单方块转化：只处理命中格 */
    private fun applyAnchor(level: ServerLevel, anchor: BlockPos): Boolean {
        if (!input.testWithoutEntity(level.getBlockState(anchor))) return false
        val applied = setResult(level, anchor, result)
        if (applied) playEffects(level, anchor)
        return applied
    }

    /** 范围转化：半径内所有匹配方块逐个转化 */
    private fun applyArea(level: ServerLevel, anchor: BlockPos): Boolean {
        var applied = false
        val min = anchor.offset(-radius, -radius, -radius)
        val max = anchor.offset(radius, radius, radius)
        for (pos in BlockPos.betweenClosed(min, max)) {
            val p = pos.immutable()
            if (!input.testWithoutEntity(level.getBlockState(p))) continue
            if (setResult(level, p, result)) {
                applied = true
                playEffects(level, p)
            }
        }
        return applied
    }

    /** 应用单个方块转化（含概率与方块实体 NBT）；未发生转化返回 false */
    private fun setResult(level: ServerLevel, pos: BlockPos, chanceState: ChanceBlockState): Boolean {
        val entry = chanceState.getResult(level) ?: return false
        val target = entry.key
        if (target == level.getBlockState(pos)) return false
        level.setBlock(pos, target, 3)
        val nbt = entry.value
        if (!nbt.isEmpty) {
            level.getBlockEntity(pos)?.loadWithComponents(nbt, level.registryAccess())
        }
        return true
    }

    private fun playEffects(level: ServerLevel, pos: BlockPos) {
        level.levelEvent(2001, pos, Block.getId(level.getBlockState(pos)))
        level.playSound(null, pos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.BLOCKS, 1.0f, 1.0f)
    }

    // ---------- Recipe 接口 ----------

    override fun matches(input: RecipeInput, level: Level): Boolean = false

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    @Deprecated("Unused: transformation recipes are triggered by wither skull impact")
    override fun assemble(input: RecipeInput, registries: HolderLookup.Provider): ItemStack = ItemStack.EMPTY

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    @Deprecated("Unused: transformation recipes are triggered by wither skull impact")
    override fun canCraftInDimensions(width: Int, height: Int): Boolean = false

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    @Deprecated("Unused: transformation recipes are triggered by wither skull impact")
    override fun getResultItem(registries: HolderLookup.Provider): ItemStack = ItemStack.EMPTY

    override fun getSerializer(): RecipeSerializer<*> = ModRecipeTypes.WITHER_TRANSFORMATION_SERIALIZER.get()

    override fun getType(): RecipeType<*> = ModRecipeTypes.WITHER_TRANSFORMATION_TYPE.get()

    override fun isSpecial(): Boolean = true

    // ---------- 编解码 ----------

    companion object {
        val CODEC: MapCodec<WitherTransformationRecipe> = RecordCodecBuilder.mapCodec { inst ->
            inst.group(
                Mode.CODEC.optionalFieldOf("mode", Mode.ANCHOR).forGetter { it.mode },
                BlockStatePredicate.CODEC.fieldOf("input").forGetter { it.input },
                ChanceBlockState.CODEC.fieldOf("result").forGetter { it.result },
                Codec.INT.optionalFieldOf("radius", 3).forGetter { it.radius },
                Codec.BOOL.optionalFieldOf("dangerous", false).forGetter { it.dangerous },
            ).apply(inst) { mode, input, result, radius, dangerous ->
                WitherTransformationRecipe(mode, input, result, radius, dangerous)
            }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WitherTransformationRecipe> = StreamCodec.composite(
            Mode.STREAM_CODEC, { it.mode },
            BlockStatePredicate.STREAM_CODEC, { it.input },
            ChanceBlockState.STREAM_CODEC, { it.result },
            ByteBufCodecs.INT, { it.radius },
            ByteBufCodecs.BOOL, { it.dangerous },
        ) { mode, input, result, radius, dangerous ->
            WitherTransformationRecipe(mode, input, result, radius, dangerous)
        }
    }

    class Serializer : RecipeSerializer<WitherTransformationRecipe> {
        override fun codec(): MapCodec<WitherTransformationRecipe> = CODEC
        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, WitherTransformationRecipe> = STREAM_CODEC
    }
}

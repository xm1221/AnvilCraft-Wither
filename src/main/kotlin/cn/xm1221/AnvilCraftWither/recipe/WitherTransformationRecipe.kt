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
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import java.util.Optional

/**
 * 凋灵之首转化配方：凋灵之首命中 [input] 匹配的方块（或 [pattern] 描述的多方块结构）时，
 * 将方块按 [result] / [structureResult] 转化。
 *
 * - [mode]：命中处理模式
 *   - [Mode.ANCHOR]：锚点（命中格）转化，爆炸照常；
 *   - [Mode.ANCHOR_NO_EXPLOSION]：锚点转化，且抑制凋灵之首的默认爆炸；
 *   - [Mode.AREA]：以命中点为球心、[radius] 为半径的立方体内所有匹配方块逐个转化，爆炸照常。
 * - [input]：锚点方块谓词（area 模式下为区域内每个方块的匹配谓词）；
 * - [pattern]：可选的多方块结构模式（有则要求锚点周围结构整体匹配）；
 * - [result]：锚点转化结果（含概率）；
 * - [structureResult]：可选的整体结构转化（按模式符号映射目标方块），只作用于非锚点位置。
 */
class WitherTransformationRecipe(
    val mode: Mode,
    val input: BlockStatePredicate,
    val pattern: BlockPattern?,
    val result: ChanceBlockState,
    val structureResult: Map<Char, ChanceBlockState>,
    val radius: Int,
) : Recipe<RecipeInput> {

    enum class Mode(private val id: String) {
        ANCHOR("anchor"),
        ANCHOR_NO_EXPLOSION("anchor_no_explosion"),
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

    /** 命中判定：锚点方块匹配 [input]，且（如有模式）结构在某个旋转下整体匹配 */
    fun findMatch(level: Level, anchor: BlockPos): Boolean {
        if (!input.testWithoutEntity(level.getBlockState(anchor))) return false
        return pattern == null || pattern.findRotation(level, anchor) != null
    }

    /** 执行转化；返回是否至少发生了一次方块转化 */
    fun apply(level: ServerLevel, anchor: BlockPos): Boolean = when (mode) {
        Mode.AREA -> applyArea(level, anchor)
        else -> applyAnchor(level, anchor)
    }

    private fun applyAnchor(level: ServerLevel, anchor: BlockPos): Boolean {
        if (!input.testWithoutEntity(level.getBlockState(anchor))) return false
        val rot = pattern?.findRotation(level, anchor)
        if (pattern != null && rot == null) return false

        var applied = setResult(level, anchor, result)
        if (applied) playEffects(level, anchor)

        if (applied && pattern != null && rot != null && structureResult.isNotEmpty()) {
            for ((pos, symbol) in pattern.symbolPositions(anchor, rot)) {
                if (pos == anchor) continue
                val target = structureResult[symbol] ?: continue
                if (setResult(level, pos, target)) {
                    playEffects(level, pos)
                }
            }
        }
        return applied
    }

    private fun applyArea(level: ServerLevel, anchor: BlockPos): Boolean {
        var applied = false
        val min = anchor.offset(-radius, -radius, -radius)
        val max = anchor.offset(radius, radius, radius)
        for (pos in BlockPos.betweenClosed(min, max)) {
            val p = pos.immutable()
            if (input.testWithoutEntity(level.getBlockState(p))) {
                if (setResult(level, p, result)) {
                    applied = true
                    playEffects(level, p)
                }
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
        private val STRUCTURE_RESULT_CODEC: Codec<Map<Char, ChanceBlockState>> =
            Codec.unboundedMap(Codec.STRING, ChanceBlockState.CODEC.codec()).xmap(
                { map -> map.mapKeys { it.key.first() } },
                { map -> map.mapKeys { it.key.toString() } },
            )

        private val OPTIONAL_PATTERN_STREAM: StreamCodec<RegistryFriendlyByteBuf, Optional<BlockPattern>> =
            StreamCodec.of(
                { buf, opt ->
                    buf.writeBoolean(opt.isPresent)
                    if (opt.isPresent) BlockPattern.STREAM_CODEC.encode(buf, opt.get())
                },
                { buf ->
                    if (buf.readBoolean()) Optional.of(BlockPattern.STREAM_CODEC.decode(buf)) else Optional.empty()
                },
            )

        private val STRUCTURE_RESULT_STREAM: StreamCodec<RegistryFriendlyByteBuf, Map<Char, ChanceBlockState>> =
            StreamCodec.of(
                { buf, map ->
                    buf.writeInt(map.size)
                    for ((c, v) in map) {
                        buf.writeChar(c.code)
                        ChanceBlockState.STREAM_CODEC.encode(buf, v)
                    }
                },
                { buf -> (0 until buf.readInt()).associate { buf.readChar().toChar() to ChanceBlockState.STREAM_CODEC.decode(buf) } },
            )

        val CODEC: MapCodec<WitherTransformationRecipe> = RecordCodecBuilder.mapCodec { inst ->
            inst.group(
                Mode.CODEC.optionalFieldOf("mode", Mode.ANCHOR).forGetter { it.mode },
                BlockStatePredicate.CODEC.fieldOf("input").forGetter { it.input },
                BlockPattern.CODEC.codec().optionalFieldOf("pattern").forGetter { Optional.ofNullable(it.pattern) },
                ChanceBlockState.CODEC.fieldOf("result").forGetter { it.result },
                STRUCTURE_RESULT_CODEC.optionalFieldOf("structure_result", emptyMap()).forGetter { it.structureResult },
                Codec.INT.optionalFieldOf("radius", 3).forGetter { it.radius },
            ).apply(inst) { mode, input, pattern, result, structureResult, radius ->
                WitherTransformationRecipe(mode, input, pattern.orElse(null), result, structureResult, radius)
            }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WitherTransformationRecipe> = StreamCodec.composite(
            Mode.STREAM_CODEC, { it.mode },
            BlockStatePredicate.STREAM_CODEC, { it.input },
            OPTIONAL_PATTERN_STREAM, { Optional.ofNullable(it.pattern) },
            ChanceBlockState.STREAM_CODEC, { it.result },
            STRUCTURE_RESULT_STREAM, { it.structureResult },
            ByteBufCodecs.INT, { it.radius },
        ) { mode, input, pattern, result, structureResult, radius ->
            WitherTransformationRecipe(mode, input, pattern.orElse(null), result, structureResult, radius)
        }
    }

    class Serializer : RecipeSerializer<WitherTransformationRecipe> {
        override fun codec(): MapCodec<WitherTransformationRecipe> = CODEC
        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, WitherTransformationRecipe> = STREAM_CODEC
    }
}

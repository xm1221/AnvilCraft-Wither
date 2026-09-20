package cn.xm1221.AnvilCraftWither.recipe

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Rotation
import java.util.Optional

/**
 * 字符图层多方块结构模式。
 *
 * - [layers] 从下到上排列；每层为等长字符串数组（层内索引 = z 方向，字符索引 = x 方向）；
 * - 空格字符 `' '` 表示"任意方块"，不参与匹配也不参与整体转化；
 * - [anchor] 为命中方块在模式内对应的坐标，null 表示取模式几何中心；
 * - [rotate] 为 true 时支持绕锚点 4 向旋转匹配（默认 true）。
 */
class BlockPattern(
    val layers: List<List<String>>,
    val symbols: Map<Char, BlockStatePredicate>,
    private val anchorOverride: Vec3i?,
    val rotate: Boolean,
) {
    /** 锚点（命中方块对应格）在模式内的坐标 */
    val anchor: Vec3i = anchorOverride ?: center(layers)

    val height: Int get() = layers.size
    /** z 方向行数 */
    val width: Int get() = layers[0].size
    /** x 方向列数 */
    val depth: Int get() = layers[0][0].length

    init {
        require(layers.isNotEmpty()) { "layers must not be empty" }
        val w = layers[0].size
        require(layers.all { it.size == w }) { "all layers must have equal depth" }
        val d = layers[0][0].length
        require(layers.all { layer -> layer.all { it.length == d } }) { "all rows must have equal width" }
        require(symbols.keys.none { it == ' ' }) { "' ' is a reserved symbol" }
    }

    /** 在世界锚点 [worldAnchor] 处查找匹配的旋转；不匹配返回 null */
    fun findRotation(level: Level, worldAnchor: BlockPos): Rotation? {
        val rotations = if (rotate) ROTATIONS else listOf(Rotation.NONE)
        return rotations.firstOrNull { matchAt(level, worldAnchor, it) }
    }

    /** 世界锚点 + 旋转 → 非空符号位置列表（相对坐标已按旋转变换） */
    fun symbolPositions(worldAnchor: BlockPos, rot: Rotation): List<Pair<BlockPos, Char>> {
        val out = mutableListOf<Pair<BlockPos, Char>>()
        for (y in 0 until height) {
            val layer = layers[y]
            for (z in 0 until width) {
                val row = layer[z]
                for (x in 0 until depth) {
                    val symbol = row[x]
                    if (symbol == ' ') continue
                    val rel = rotateRel(Vec3i(x - anchor.x, y - anchor.y, z - anchor.z), rot)
                    out.add(worldAnchor.offset(rel.x, rel.y, rel.z) to symbol)
                }
            }
        }
        return out
    }

    private fun matchAt(level: Level, worldAnchor: BlockPos, rot: Rotation): Boolean {
        for ((pos, symbol) in symbolPositions(worldAnchor, rot)) {
            val predicate = symbols[symbol] ?: return false
            if (!predicate.testWithoutEntity(level.getBlockState(pos).rotate(rot))) return false
        }
        return true
    }

    companion object {
        val ROTATIONS = listOf(
            Rotation.NONE,
            Rotation.CLOCKWISE_90,
            Rotation.CLOCKWISE_180,
            Rotation.COUNTERCLOCKWISE_90,
        )

        private fun rotateRel(rel: Vec3i, rot: Rotation): Vec3i = when (rot) {
            Rotation.NONE -> rel
            Rotation.CLOCKWISE_90 -> Vec3i(-rel.z, rel.y, rel.x)
            Rotation.CLOCKWISE_180 -> Vec3i(-rel.x, rel.y, -rel.z)
            Rotation.COUNTERCLOCKWISE_90 -> Vec3i(rel.z, rel.y, -rel.x)
        }

        /** 模式几何中心（向下取整） */
        fun center(layers: List<List<String>>): Vec3i =
            Vec3i(layers[0][0].length / 2, layers.size / 2, layers[0].size / 2)

        // ---------- 编解码 ----------

        val VEC3I_CODEC: Codec<Vec3i> = Codec.INT.listOf().xmap(
            { list -> Vec3i(list[0], list[1], list[2]) },
            { v -> listOf(v.x, v.y, v.z) },
        )

        val VEC3I_STREAM: StreamCodec<RegistryFriendlyByteBuf, Vec3i> = StreamCodec.of(
            { buf, v ->
                buf.writeInt(v.x)
                buf.writeInt(v.y)
                buf.writeInt(v.z)
            },
            { buf -> Vec3i(buf.readInt(), buf.readInt(), buf.readInt()) },
        )

        private val OPTIONAL_ANCHOR_STREAM: StreamCodec<RegistryFriendlyByteBuf, Optional<Vec3i>> = StreamCodec.of(
            { buf, opt ->
                buf.writeBoolean(opt.isPresent)
                if (opt.isPresent) VEC3I_STREAM.encode(buf, opt.get())
            },
            { buf -> if (buf.readBoolean()) Optional.of(VEC3I_STREAM.decode(buf)) else Optional.empty() },
        )

        private val SYMBOLS_CODEC: Codec<Map<Char, BlockStatePredicate>> =
            Codec.unboundedMap(Codec.STRING, BlockStatePredicate.CODEC).xmap(
                { map -> map.mapKeys { it.key.first() } },
                { map -> map.mapKeys { it.key.toString() } },
            )

        private val SYMBOLS_STREAM: StreamCodec<RegistryFriendlyByteBuf, Map<Char, BlockStatePredicate>> =
            StreamCodec.of(
                { buf, map ->
                    buf.writeInt(map.size)
                    for ((c, v) in map) {
                        buf.writeChar(c.code)
                        BlockStatePredicate.STREAM_CODEC.encode(buf, v)
                    }
                },
                { buf ->
                    val size = buf.readInt()
                    (0 until size).associate { buf.readChar().toChar() to BlockStatePredicate.STREAM_CODEC.decode(buf) }
                },
            )

        private val LAYERS_STREAM: StreamCodec<RegistryFriendlyByteBuf, List<List<String>>> = StreamCodec.of(
            { buf, layers ->
                buf.writeInt(layers.size)
                for (layer in layers) {
                    buf.writeInt(layer.size)
                    for (row in layer) buf.writeUtf(row)
                }
            },
            { buf ->
                val layerCount = buf.readInt()
                (0 until layerCount).map {
                    val rowCount = buf.readInt()
                    (0 until rowCount).map { buf.readUtf() }
                }
            },
        )

        val CODEC: MapCodec<BlockPattern> = RecordCodecBuilder.mapCodec { inst ->
            inst.group(
                Codec.STRING.listOf().listOf().fieldOf("layers").forGetter { it.layers },
                SYMBOLS_CODEC.fieldOf("symbols").forGetter { it.symbols },
                VEC3I_CODEC.optionalFieldOf("anchor").forGetter { Optional.ofNullable(it.anchorOverride) },
                Codec.BOOL.optionalFieldOf("rotate", true).forGetter { it.rotate },
            ).apply(inst) { layers, symbols, anchor, rotate ->
                BlockPattern(layers, symbols, anchor.orElse(null), rotate)
            }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, BlockPattern> = StreamCodec.composite(
            LAYERS_STREAM, { it.layers },
            SYMBOLS_STREAM, { it.symbols },
            OPTIONAL_ANCHOR_STREAM, { Optional.ofNullable(it.anchorOverride) },
            ByteBufCodecs.BOOL, { it.rotate },
        ) { layers, symbols, anchor, rotate ->
            BlockPattern(layers, symbols, anchor.orElse(null), rotate)
        }
    }
}

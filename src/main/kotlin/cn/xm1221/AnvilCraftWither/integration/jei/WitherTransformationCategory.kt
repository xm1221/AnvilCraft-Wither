package cn.xm1221.AnvilCraftWither.integration.jei

import cn.xm1221.AnvilCraftWither.recipe.WitherTransformationRecipe
import com.mojang.math.Axis
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.builder.ITooltipBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.WitherSkull
import net.minecraft.world.item.ItemStack

/**
 * 凋灵之首轰击（wither_transformation）JEI 类别。
 *
 * 布局：左侧输入方块 → 中间箭头处渲染一枚**凋灵之首实体**（按配方区分普通 / 危险）→ 右侧输出方块。
 * 箭头贴图复用 AnvilCraft 的 `anvilcraft:textures/gui/jei/arrow_default.png`（16×10）；
 * 类别图标同样是凋灵之首实体。
 */
class WitherTransformationCategory(private val helper: IGuiHelper) :
    IRecipeCategory<WitherTransformationJeiRecipe> {

    private val background: IDrawable = helper.createBlankDrawable(WIDTH, HEIGHT)

    /** 箭头贴图：复用 AnvilCraft 的 arrow_default（与 JeiRenderHelper#getArrowDefault 一致） */
    private val arrow: IDrawable = helper.drawableBuilder(
        ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/jei/arrow_default.png"),
        0,
        0,
        ARROW_WIDTH,
        ARROW_HEIGHT,
    ).setTextureSize(ARROW_WIDTH, ARROW_HEIGHT).build()

    /** 爆炸贴图：复用 AnvilCraft 的 explosion（表示凋灵之首的轰击爆炸） */
    private val explosion: IDrawable = helper.drawableBuilder(
        ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/jei/explosion.png"),
        0,
        0,
        EXPLOSION_SIZE,
        EXPLOSION_SIZE,
    ).setTextureSize(EXPLOSION_SIZE, EXPLOSION_SIZE).build()

    /** 类别图标：凋灵之首实体 */
    private val icon: IDrawable = WitherSkullIcon()

    /** 缓存的凋灵之首实体（按类型各一份，避免每帧新建） */
    private var cachedSkull: WitherSkull? = null
    private var cachedDangerous: Boolean? = null
    private var cachedLevel: ClientLevel? = null

    override fun getRecipeType(): RecipeType<WitherTransformationJeiRecipe> =
        WitherTransformationJeiPlugin.TYPE

    override fun getTitle(): Component =
        Component.translatable("gui.anvilcraft_wither.category.wither_transformation")

    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun getWidth(): Int = WIDTH

    override fun getHeight(): Int = HEIGHT

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: WitherTransformationJeiRecipe,
        focuses: IFocusGroup,
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
            .addItemStack(recipe.input)
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
            .addItemStack(recipe.output)
    }

    /**
     * 排布：凋灵之首实体 → 箭头 → 爆炸贴图（与输入方块部分重叠）→ 输入方块 → 箭头 → 产物方块
     */
    override fun draw(
        recipe: WitherTransformationJeiRecipe,
        slotsView: IRecipeSlotsView,
        guiGraphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double,
    ) {
        renderWitherSkull(guiGraphics, recipe.dangerous, SKULL_X, SKULL_Y, SKULL_SCALE)
        arrow.draw(guiGraphics, ARROW_HEAD_X, ARROW_Y)
        explosion.draw(guiGraphics, EXPLOSION_X, EXPLOSION_Y)
        arrow.draw(guiGraphics, ARROW_TAIL_X, ARROW_Y)
        drawModeLabel(guiGraphics, recipe)
    }

    /** 右下角标注转化模式（范围模式附加半径） */
    private fun drawModeLabel(guiGraphics: GuiGraphics, recipe: WitherTransformationJeiRecipe) {
        val font = Minecraft.getInstance().font
        val area = recipe.mode == WitherTransformationRecipe.Mode.AREA
        var label = Component.translatable(
            if (area) {
                "gui.anvilcraft_wither.category.wither_transformation.mode.area"
            } else {
                "gui.anvilcraft_wither.category.wither_transformation.mode.anchor"
            },
        )
        if (area && recipe.radius > 0) {
            label = label.copy()
                .append(" ")
                .append(
                    Component.translatable(
                        "gui.anvilcraft_wither.category.wither_transformation.radius",
                        recipe.radius,
                    ),
                )
        }
        val text = label.string
        guiGraphics.drawString(
            font,
            text,
            WIDTH - LABEL_MARGIN - font.width(text),
            HEIGHT - LABEL_MARGIN - font.lineHeight,
            LABEL_COLOR,
            false,
        )
    }

    override fun getTooltip(
        tooltip: ITooltipBuilder,
        recipe: WitherTransformationJeiRecipe,
        slotsView: IRecipeSlotsView,
        mouseX: Double,
        mouseY: Double,
    ) {
        // 转化模式已画在右下角，这里只提示所需的凋灵之首类型
        tooltip.add(
            Component.translatable(
                if (recipe.dangerous) {
                    "gui.anvilcraft_wither.category.wither_transformation.skull.dangerous"
                } else {
                    "gui.anvilcraft_wither.category.wither_transformation.skull.normal"
                },
            ).withStyle(if (recipe.dangerous) ChatFormatting.BLUE else ChatFormatting.DARK_GRAY),
        )
    }

    /** 在指定位置渲染凋灵之首实体：普通为黑色，危险为蓝色 */
    private fun renderWitherSkull(
        guiGraphics: GuiGraphics,
        dangerous: Boolean,
        x: Double,
        y: Double,
        scale: Float,
    ) {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return
        val skull = obtainSkull(level, dangerous) ?: return

        // 渲染失败不应影响整个 JEI 界面
        val dispatcher = minecraft.entityRenderDispatcher
        val pose = guiGraphics.pose()
        runCatching {
            guiGraphics.flush()
            pose.pushPose()
            try {
                pose.translate(x, y, 100.0)
                // GUI 坐标系 Y 轴向下，需翻转；缩放让凋灵之首大小合适
                pose.scale(scale, -scale, scale)
                // 视角：先俯视再水平转，使正面朝向玩家
                pose.mulPose(Axis.XP.rotationDegrees(SKULL_PITCH))
                pose.mulPose(Axis.YP.rotationDegrees(SKULL_YAW))
                dispatcher.setRenderShadow(false)
                dispatcher.render(
                    skull,
                    0.0,
                    0.0,
                    0.0,
                    0.0f,
                    1.0f,
                    pose,
                    guiGraphics.bufferSource(),
                    LightTexture.FULL_BRIGHT,
                )
                guiGraphics.flush()
            } finally {
                pose.popPose()
                dispatcher.setRenderShadow(true)
            }
        }
    }

    private fun obtainSkull(level: ClientLevel, dangerous: Boolean): WitherSkull? {
        val cached = cachedSkull
        if (cached != null && cachedDangerous == dangerous && cachedLevel === level) return cached
        // 注意：不能用 WitherSkull(level, shooter, movement) 构造——shooter 为 null 时
        // AbstractHurtingProjectile 构造内部会调用 owner.getX() 抛 NPE（JEI 里没有发射者）。
        val skull = EntityType.WITHER_SKULL.create(level) ?: return null
        skull.isDangerous = dangerous
        cachedSkull = skull
        cachedDangerous = dangerous
        cachedLevel = level
        return skull
    }

    /** 类别图标：一枚普通（黑色）凋灵之首 */
    private inner class WitherSkullIcon : IDrawable {
        override fun getWidth(): Int = ICON_SIZE

        override fun getHeight(): Int = ICON_SIZE

        override fun draw(guiGraphics: GuiGraphics, xOffset: Int, yOffset: Int) {
            renderWitherSkull(
                guiGraphics,
                false,
                xOffset + ICON_SIZE / 2.0,
                yOffset + ICON_SIZE - 2.0,
                ICON_SCALE,
            )
        }
    }

    companion object {
        private const val WIDTH = 142
        private const val HEIGHT = 74

        /** 凋灵之首实体（左端）的渲染中心、缩放与视角（绕 X 俯仰、绕 Y 水平） */
        private const val SKULL_X = 18.0
        private const val SKULL_Y = 43.0
        private const val SKULL_SCALE = 26.0f
        private const val SKULL_PITCH = 20.0f

        /** 绕 Y 旋转：凋灵之首模型默认背对观察者，+180° 转正 */
        private const val SKULL_YAW = 215.0f

        /** 凋灵之首 → 爆炸 之间的箭头 */
        private const val ARROW_HEAD_X = 36

        /** 爆炸贴图（anvilcraft:textures/gui/jei/explosion.png，32×32），与输入方块部分重叠 */
        private const val EXPLOSION_SIZE = 32
        private const val EXPLOSION_X = 52
        private const val EXPLOSION_Y = 21

        /** 输入 / 输出槽位置（输入槽左移到与爆炸贴图明显重叠） */
        private const val INPUT_X = 66
        private const val OUTPUT_X = 116
        private const val SLOT_Y = 28

        /** 箭头贴图（anvilcraft:textures/gui/jei/arrow_default.png，16×10） */
        private const val ARROW_WIDTH = 16
        private const val ARROW_HEIGHT = 10
        private const val ARROW_Y = 32

        /** 输入 → 产物 之间的箭头 */
        private const val ARROW_TAIL_X = 97

        /** 类别图标 */
        private const val ICON_SIZE = 16
        private const val ICON_SCALE = 13.0f

        /** 右下角模式标注 */
        private const val LABEL_MARGIN = 4
        private const val LABEL_COLOR = 0xFF404040.toInt()
    }
}

package cn.xm1221.AnvilCraftWither.integration.jei

import cn.xm1221.AnvilCraftWither.recipe.WitherTransformationRecipe
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
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * 凋灵转化（wither_transformation）JEI 类别：
 * 展示 凋灵之首 + 输入方块 → 输出方块，并将模式 / 半径 / 多方块结构写入 tooltip。
 */
class WitherTransformationCategory(private val helper: IGuiHelper) : IRecipeCategory<WitherTransformationJeiRecipe> {

    private val background: IDrawable = WitherTransformationBackground(helper)

    override fun getRecipeType(): RecipeType<WitherTransformationJeiRecipe> =
        WitherTransformationJeiPlugin.TYPE

    override fun getTitle(): Component =
        Component.translatable("gui.anvilcraft_wither.category.wither_transformation")

    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable? = helper.createDrawableItemStack(ItemStack(Items.WITHER_SKELETON_SKULL))

    override fun getWidth(): Int = background.width

    override fun getHeight(): Int = background.height

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: WitherTransformationJeiRecipe,
        focuses: IFocusGroup,
    ) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 8, 4)
            .addItemStack(ItemStack(Items.WITHER_SKELETON_SKULL))
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 36)
            .addItemStack(recipe.input)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 110, 36)
            .addItemStack(recipe.output)
    }

    override fun getTooltip(
        tooltip: ITooltipBuilder,
        recipe: WitherTransformationJeiRecipe,
        slotsView: IRecipeSlotsView,
        mouseX: Double,
        mouseY: Double,
    ) {
        val modeKey = when (recipe.mode) {
            WitherTransformationRecipe.Mode.ANCHOR -> "gui.anvilcraft_wither.category.wither_transformation.mode.anchor"
            WitherTransformationRecipe.Mode.ANCHOR_NO_EXPLOSION ->
                "gui.anvilcraft_wither.category.wither_transformation.mode.anchor_no_explosion"
            WitherTransformationRecipe.Mode.AREA -> "gui.anvilcraft_wither.category.wither_transformation.mode.area"
        }
        tooltip.add(Component.translatable(modeKey).withStyle(ChatFormatting.GRAY))
        if (recipe.mode == WitherTransformationRecipe.Mode.AREA && recipe.radius > 0) {
            tooltip.add(
                Component.translatable(
                    "gui.anvilcraft_wither.category.wither_transformation.radius",
                    recipe.radius,
                ).withStyle(ChatFormatting.DARK_GRAY),
            )
        }
        val pattern = recipe.recipe.pattern
        if (pattern != null) {
            tooltip.add(
                Component.translatable("gui.anvilcraft_wither.category.wither_transformation.pattern")
                    .withStyle(ChatFormatting.DARK_GRAY),
            )
            // 从下到上输出每一层
            for (layer in pattern.layers.reversed()) {
                tooltip.add(Component.literal(layer.joinToString("\n")).withStyle(ChatFormatting.DARK_GRAY))
            }
        }
    }

    /** 静态背景：边框 + 凋灵之首图标 + 箭头 */
    private class WitherTransformationBackground(helper: IGuiHelper) : IDrawable {
        private val width: Int = 132
        private val height: Int = 74
        private val border: IDrawable = helper.createBlankDrawable(width, height)

        override fun getWidth(): Int = width

        override fun getHeight(): Int = height

        override fun draw(graphics: GuiGraphics, xOffset: Int, yOffset: Int) {
            border.draw(graphics, xOffset, yOffset)
            val x = xOffset
            val y = yOffset
            // 边框
            graphics.fill(x, y, x + width, y + 1, 0xFF000000.toInt())
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFF000000.toInt())
            graphics.fill(x, y, x + 1, y + height, 0xFF000000.toInt())
            graphics.fill(x + width - 1, y, x + width, y + height, 0xFF000000.toInt())
            // 转化箭头（input → output）
            val ax = x + 62
            val ay = y + 43
            graphics.fill(ax, ay, ax + 22, ay + 6, 0xFF555555.toInt())
            graphics.fill(ax + 20, ay - 3, ax + 26, ay + 9, 0xFF555555.toInt())
        }
    }
}

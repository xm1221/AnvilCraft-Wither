package cn.xm1221.AnvilCraftWither.integration.jei

import cn.xm1221.AnvilCraftWither.init.AddonItems
import cn.xm1221.AnvilCraftWither.init.ModRecipeTypes
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

/**
 * AnvilCraft-Wither 的 JEI 集成：注册凋灵转化配方类别。
 *
 * JEI 通过 `@JeiPlugin` 扫描并实例化本类（需要公开无参构造，故用 class 而非 object）。
 */
@JeiPlugin
class WitherTransformationJeiPlugin : IModPlugin {

    companion object {
        /** 凋灵转化配方类别类型 */
        val TYPE: RecipeType<WitherTransformationJeiRecipe> = RecipeType.create(
            "anvilcraft_wither",
            "wither_transformation",
            WitherTransformationJeiRecipe::class.java,
        )
    }

    override fun getPluginUid(): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath("anvilcraft_wither", "jei_plugin")

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        registration.addRecipeCategories(
            WitherTransformationCategory(registration.jeiHelpers.guiHelper),
        )
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        val level = Minecraft.getInstance().level ?: return
        val recipes = level.recipeManager.getAllRecipesFor(ModRecipeTypes.WITHER_TRANSFORMATION_TYPE.get())
        registration.addRecipes(TYPE, recipes.mapNotNull { WitherTransformationJeiRecipe.from(it) })
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        // 催化剂为凋灵杖（发射凋灵之首的来源）
        registration.addRecipeCatalyst(ItemStack(AddonItems.WITHER_STAFF.get()), TYPE)
    }
}

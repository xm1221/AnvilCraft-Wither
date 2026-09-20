package cn.xm1221.AnvilCraftWither.init

import cn.xm1221.AnvilCraftWither.AnvilCraftWither
import cn.xm1221.AnvilCraftWither.recipe.WitherTransformationRecipe
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModRecipeTypes {
    private val RECIPE_TYPES: DeferredRegister<RecipeType<*>> =
        DeferredRegister.create(Registries.RECIPE_TYPE, AnvilCraftWither.MOD_ID)
    private val RECIPE_SERIALIZERS: DeferredRegister<RecipeSerializer<*>> =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, AnvilCraftWither.MOD_ID)

    /** 凋灵之首命中转化配方 */
    val WITHER_TRANSFORMATION_TYPE: DeferredHolder<RecipeType<*>, RecipeType<WitherTransformationRecipe>> =
        registerType("wither_transformation")
    val WITHER_TRANSFORMATION_SERIALIZER: DeferredHolder<RecipeSerializer<*>, RecipeSerializer<WitherTransformationRecipe>> =
        RECIPE_SERIALIZERS.register("wither_transformation", Supplier { WitherTransformationRecipe.Serializer() })

    private fun <T : Recipe<*>> registerType(name: String): DeferredHolder<RecipeType<*>, RecipeType<T>> =
        RECIPE_TYPES.register(
            name,
            Supplier {
                object : RecipeType<T> {
                    override fun toString(): String = AnvilCraftWither.of(name).toString()
                }
            },
        )

    fun register(modEventBus: IEventBus) {
        RECIPE_TYPES.register(modEventBus)
        RECIPE_SERIALIZERS.register(modEventBus)
    }
}

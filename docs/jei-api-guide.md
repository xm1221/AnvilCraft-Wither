# JEI API 速查（NeoForge 1.21.1 / JEI 19.50.0.414）

本工程已集成 JEI（见 `dependencies.gradle`、`gradle/libs.versions.toml`、`gradle/scripts/repositories.gradle`）。
现成例子：`src/main/kotlin/cn/xm1221/AnvilCraftWither/integration/jei/`。

---

## 1. 依赖与仓库

```gradle
// gradle/libs.versions.toml
jei = "19.50.0.414"
jei-common-api   = { group = "mezz.jei", name = "jei-1.21.1-common-api",   version.ref = "jei" }
jei-neoforge-api = { group = "mezz.jei", name = "jei-1.21.1-neoforge-api", version.ref = "jei" }
jei-neoforge     = { group = "mezz.jei", name = "jei-1.21.1-neoforge",     version.ref = "jei" }

// dependencies.gradle
compileClasspath(libs.jei.common.api)   { transitive = false }
compileClasspath(libs.jei.neoforge.api) { transitive = false }
compileClasspath(libs.jei.neoforge)     { transitive = false }
runtimeClasspath(libs.jei.neoforge)     { transitive = false }   // 开发环境 runClient 里才有 JEI
```

```gradle
// gradle/scripts/repositories.gradle
maven {
    name = "JEI Maven"
    url = "https://maven.blamejared.com/"
    content { includeGroup "mezz.jei" }
}
```

> 发布 jar 不打包 JEI；JEI 是**客户端 mod**，插件类只在装了 JEI 的客户端加载。

---

## 2. 插件入口

```kotlin
@JeiPlugin
class MyJeiPlugin : IModPlugin {
    companion object {
        val TYPE: RecipeType<MyDisplay> =
            RecipeType.create("anvilcraft_wither", "my_category", MyDisplay::class.java)
    }

    override fun getPluginUid(): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath("anvilcraft_wither", "jei_plugin")
}
```

要点：
- **必须 public class + 公开无参构造**——JEI 用它反射实例化；Kotlin 的 `object` **不行**（构造是 private）。
- `@JeiPlugin`（`mezz.jei.api.JeiPlugin`）+ `IModPlugin`（`mezz.jei.api.IModPlugin`）。
- `getPluginUid()` 全局唯一（一般 `<modid>:jei_plugin`）。

### IModPlugin 的回调（按需 override）

| 方法 | 参数类型 | 用途 |
|---|---|---|
| `registerCategories` | `IRecipeCategoryRegistration` | 注册自定义配方类别 |
| `registerRecipes` | `IRecipeRegistration` | 注册配方数据 / 信息页 |
| `registerRecipeCatalysts` | `IRecipeCatalystRegistration` | 注册催化剂（左侧物品栏来源列表） |
| `registerRecipeTransferHandlers` | `IRecipeTransferRegistration` | 配方一键填充到 GUI |
| `registerGuiHandlers` | `IGuiHandlerRegistration` | 点击区域、GUI 拖拽 |
| `registerAdvanced` | `IAdvancedRegistration` | 配方排序/搜索、subtype 解释器 |
| `onRuntimeAvailable` | `IJeiRuntime` | 运行时拿到配方管理器（隐藏配方等） |
| `onRuntimeUnavailable` | — | 清理 |

---

## 3. RecipeType 与展示数据

```kotlin
val TYPE: RecipeType<MyDisplay> = RecipeType.create("modid", "path", MyDisplay::class.java)
```

- 泛型 `T` 是**你自己定义的展示数据类**（不要求是 Minecraft `Recipe`）。
- 建议写一个 wrapper，把真实配方（如 `RecipeHolder<WitherTransformationRecipe>`）转换好：
  输入/输出 `ItemStack`、文本、结构等，`registerRecipes` 里 `mapNotNull { ... }` 转换。
- ⚠️ `mezz.jei.api.recipe.RecipeType` 与 `net.minecraft.world.item.crafting.RecipeType` **同名不同包**，
  Kotlin 里同时用到时给其中一个写全限定名或 import 别名（`import ... as JeiRecipeType`）。

---

## 4. IRecipeCategory<T>

```kotlin
class MyCategory(private val helper: IGuiHelper) : IRecipeCategory<MyDisplay> {
    private val background: IDrawable = helper.createBlankDrawable(132, 74)

    override fun getRecipeType() = MyJeiPlugin.TYPE
    override fun getTitle(): Component = Component.translatable("gui.modid.category.my")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable? = helper.createDrawableItemStack(ItemStack(Items.WITHER_SKELETON_SKULL))
    override fun getWidth(): Int = background.width
    override fun getHeight(): Int = background.height

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: MyDisplay, focuses: IFocusGroup) { ... }
}
```

**必须实现（19.50）**：`getRecipeType` / `getTitle` / `getBackground` / **`getIcon`（抽象，忘了会编译失败）** /
`getWidth` / `getHeight` / `setRecipe`。

⚠️ **`setRecipe` 的第三个参数是 `IFocusGroup`**（`mezz.jei.api.recipe.IFocusGroup`）。
网上很多例子写 `IRecipeSlotsView`——那是 JEI 20+（1.21.3+）的签名，在 19.x 上会报 "overrides nothing"。

可选 override：
- `getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView slots, double mouseX, double mouseY)`
  —— 悬停 tooltip：`tooltip.add(Component...)`
- `draw(T recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY)`
  —— 在背景之上画自定义内容（文字、图形）
- `handleInput(...)`、`getRegistryName()`（一般不用）

---

## 5. 布局：IRecipeLayoutBuilder

```kotlin
override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: MyDisplay, focuses: IFocusGroup) {
    builder.addSlot(RecipeIngredientRole.CATALYST, 8, 4).addItemStack(ItemStack(Items.WITHER_SKELETON_SKULL))
    builder.addSlot(RecipeIngredientRole.INPUT, 8, 36).addItemStack(recipe.input)
    builder.addSlot(RecipeIngredientRole.OUTPUT, 110, 36).addItemStack(recipe.output)
}
```

- 坐标以**背景左上角**为原点（像素，1 格槽位 18x18）。
- `RecipeIngredientRole`：`INPUT` / `OUTPUT` / `CATALYST` / `RENDER_ONLY`。
- `ISlotBuilder` 常用：
  - `.addItemStack(ItemStack)`、`.addItemStacks(List<ItemStack>)`
  - `.addIngredients(IIngredient)`（如 `Ingredient.of(...)`、tag）
  - `.addFluidStack(FluidStack)` / `.addFluidStack(Fluid, long)`
  - `.addRichTooltipCallback { view, tooltip -> ... }`、`.addTooltipCallback { ... }`
  - `.setCustomRenderer(type, renderer)`（自定义渲染，比如画方块/实体）
  - `.setStandardSlotBackground()` / `.setOutputSlotBackground()`
- 其它：
  - `builder.addInvisibleIngredients(role)` —— 登记配方成分但不占槽位
  - `builder.addShape(x, y)` —— 命中测试用的形状
  - `builder.setShapeless()` —— 标记无序
  - 槽位连接线：`builder.createLineBackground(2).line(...)`（网格风格的炼制线）

---

## 6. 背景与图标：IGuiHelper

`registration.jeiHelpers.guiHelper`（或构造参数注入）：

| 方法 | 说明 |
|---|---|
| `createBlankDrawable(w, h)` | 空白背景（自己用 GuiGraphics 画） |
| `createDrawable(ResourceLocation tex, u, v, w, h)` | 从纹理裁一块（纹理须 256x256） |
| `createDrawableItemStack(ItemStack)` | 物品图标（做 category 图标最常用） |
| `createDrawableIngredient(VanillaTypes.ITEM_STACK, stack)` | 成分图标 |
| `createDrawableBlockState(BlockState)` / `createDrawableBlockStateIcon(...)` | 方块图标（AnvilCraft 常用） |
| `getSlotDrawable()` | 标准槽位底图 |
| `createAnimatedDrawable(drawable, ticksPerCycle, startDirection, positiveDirection)` | 进度动画 |
| `drawableBuilder(...)` | 自定义 IDrawable 构建器 |

自绘背景（不需要纹理）：

```kotlin
private class MyBackground(private val helper: IGuiHelper) : IDrawable {
    override fun getWidth() = 132
    override fun getHeight() = 74
    override fun draw(g: GuiGraphics, xOffset: Int, yOffset: Int) {
        helper.createBlankDrawable(132, 74).draw(g, xOffset, yOffset)
        g.fill(xOffset, yOffset, xOffset + 132, yOffset + 1, 0xFF000000.toInt())  // 注意 toInt()
        g.drawString(Minecraft.getInstance().font, "文本", xOffset + 8, yOffset + 6, 0xFF404040.toInt(), false)
    }
}
```

⚠️ Kotlin 里 `0xFF000000` 是 `Long`，`GuiGraphics.fill/drawString` 要 `Int` → 加 `.toInt()`。

---

## 7. 注册配方与催化剂

```kotlin
override fun registerCategories(registration: IRecipeCategoryRegistration) {
    registration.addRecipeCategories(MyCategory(registration.jeiHelpers.guiHelper))
}

override fun registerRecipes(registration: IRecipeRegistration) {
    val level = Minecraft.getInstance().level ?: return
    val recipes = level.recipeManager.getAllRecipesFor(ModRecipeTypes.WITHER_TRANSFORMATION_TYPE.get())
    registration.addRecipes(MyJeiPlugin.TYPE, recipes.mapNotNull { MyDisplay.from(it) })
}

override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
    registration.addRecipeCatalyst(ItemStack(Items.WITHER_SKELETON_SKULL), MyJeiPlugin.TYPE)
    registration.addRecipeCatalyst(ItemStack(AddonItems.WITHER_STAFF.get()), MyJeiPlugin.TYPE)
}
```

- 配方从**客户端** `Minecraft.getInstance().level.recipeManager` 读（数据包同步后的配方集合）。
- `registerRecipes` 里还能：
  - `registration.addIngredientInfo(listOf(stack), VanillaTypes.ITEM_STACK, Component...)` —— 给物品加"用途/信息"页
  - `registration.getIngredientManager()`、`registration.getVanillaRecipeFactory()`
- 催化剂：物品栏里对某个物品按 `U`（用途）时，左侧会列出这些类别。

---

## 8. 一键填充（配方 → GUI）

```kotlin
override fun registerRecipeTransferHandlers(registration: IRecipeTransferRegistration) {
    registration.addRecipeTransferHandler(
        MyContainerMenu::class.java, ModMenus.MY_MENU.get(),
        MyJeiPlugin.TYPE,
        0, 3,      // 配方槽位起点 / 数量（容器侧）
        4, 36,     // 玩家背包起点 / 数量
    )
}
```

`addUniversalRecipeTransferHandler(...)` 用于任意容器。

---

## 9. 点击区域 / 高级注册

```kotlin
override fun registerGuiHandlers(registration: IGuiHandlerRegistration) {
    registration.addRecipeClickArea(
        MyScreen::class.java, 80, 35, 26, 16, MyJeiPlugin.TYPE,
    )
}

override fun registerAdvanced(registration: IAdvancedRegistration) {
    registration.addRecipeCategoryDecorator(...)             // 类别装饰
    registration.recipeManager 相关的排序/搜索定制
}
```

Subtype（NBT 变体区分，例如带不同组件的同类物品）：

```kotlin
override fun registerAdvanced(registration: IAdvancedRegistration) {
    registration.addSubtypeInterpreter(VanillaTypes.ITEM_STACK, stack, ISubtypeInterpreter { ... })
}
```

---

## 10. 运行时（隐藏配方 / 打开界面）

```kotlin
override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
    val recipeManager = jeiRuntime.recipeManager       // 可 hideRecipes / addRecipes
    val guiManager = jeiRuntime.guiManager             // showRecipes(...)
    val ingredientManager = jeiRuntime.ingredientManager
}
```

---

## 11. 本工程现成参考

- 插件：`integration/jei/WitherTransformationJeiPlugin.kt`
- 展示数据：`integration/jei/WitherTransformationJeiRecipe.kt`
- 类别：`integration/jei/WitherTransformationCategory.kt`（自绘背景 + 三个槽位 + tooltip 文本）
- 语言键：`assets/anvilcraft_wither/lang/zh_cn.json`（手写）与 datagen 生成的 `en_us.json`

AnvilCraft 官方参考（clone 在 `E:\miemod\libs\AnvilCraft`）：
- `src/main/java/dev/dubhe/anvilcraft/integration/jei/AnvilCraftJeiPlugin.java`
- `.../jei/category/anvil/liquid/TimeWarpCategory.java`（简单类别）
- `.../jei/category/multiblock/MultiBlockConversionCategory.java`（多方块结构展示）
- `.../jei/util/JeiRecipeUtil.java`、`.../jei/drawable/DrawableBlockStateIcon.java`

---

## 12. 常见坑

1. `@JeiPlugin` 类必须是 `class`（public 无参构造）——Kotlin `object` 无法被实例化。
2. `setRecipe` 第三参在 19.x 是 `IFocusGroup`（不是 `IRecipeSlotsView`）。
3. `getIcon()` 是抽象方法，必须实现（可返回 null）。
4. `RecipeType` 名字冲突：JEI 的与 MC 的区分开。
5. 背景/图标纹理必须是 256x256 的 png（`createDrawable` 的 u/v 是像素坐标）。
6. 颜色字面量加 `.toInt()`。
7. 槽位坐标算错会跑到背景外——先用 `getWidth/getHeight` 定好背景尺寸。
8. `registerRecipes` 在客户端执行，别碰服务端专属 API。

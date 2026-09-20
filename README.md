# AnvilCraft-Wither

[NeoForge 1.21.1](https://neoforged.net/) 的 [AnvilCraft](https://github.com/Anvil-Dev/AnvilCraft)（铁砧工艺）附属模组，围绕凋灵主题扩展玩法。

> ⚠️ **开发中**：当前仓库为初始化脚手架，尚未包含实际游戏内容。

## 计划内容

- 凋灵主题的物品、方块与机制（待定）

## 环境要求

| 依赖 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.152+ |
| AnvilCraft | 1.6.0+ |
| Kotlin for Forge | 5.9.0+ |
| AnvilLib | 2.0.0+snapshot.490 |

## 构建

需要 JDK 21。

```shell
./gradlew build          # 编译并打包，产物在 build/libs/
./gradlew runClient      # 启动开发客户端
./gradlew runServer      # 启动开发服务器
./gradlew runData        # 运行数据生成，输出到 src/generated/resources/
```

## 许可

代码与资源均以 MIT 协议发布，详见 [LICENSE](LICENSE) 与 [ASSETS_LICENSE](ASSETS_LICENSE)。

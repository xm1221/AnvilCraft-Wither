# AnvilCraft-Wither

An [AnvilCraft](https://github.com/Anvil-Dev/AnvilCraft) addon for [NeoForge 1.21.1](https://neoforged.net/) that expands the game with Wither-themed content.

> ⚠️ **Work in progress**: this repository is currently an initialized scaffold without any actual gameplay content yet.

## Planned content

- Wither-themed items, blocks and mechanics (TBD)

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.152+ |
| AnvilCraft | 1.6.0+ |
| Kotlin for Forge | 5.9.0+ |
| AnvilLib | 2.0.0+snapshot.490 |

## Building

Requires JDK 21.

```shell
./gradlew build          # Compile and package; artifacts go to build/libs/
./gradlew runClient      # Launch the dev client
./gradlew runServer      # Launch the dev server
./gradlew runData        # Run data generation, output to src/generated/resources/
```

## License

Both the code and the assets are released under the MIT License, see [LICENSE](LICENSE) and [ASSETS_LICENSE](ASSETS_LICENSE).

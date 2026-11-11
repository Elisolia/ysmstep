# ysmstep — Fabric 1.21.1 版

这是 NeoForge 版的 Fabric 移植，功能相同，共用同一套游戏逻辑代码。

## 和 NeoForge 版的差异

| | NeoForge | Fabric |
|---|---|---|
| 入口 | `@Mod` + `IEventBus` | `YsmStepFabric` / `YsmStepClientFabric` |
| 网络 | `RegisterPayloadHandlersEvent` | `PayloadTypeRegistry` + `Client/ServerPlayNetworking` |
| 键位 | `RegisterKeyMappingsEvent` | `KeyBindingHelper` |
| HUD | `RegisterGuiLayersEvent` | `HudRenderCallback`（1.21.1 还没有 HUD 层 API） |
| 客户端命令 | `RegisterClientCommandsEvent` | `ClientCommandRegistrationCallback` |
| 配置目录 | `FMLPaths.CONFIGDIR` | `FabricLoader.getConfigDir()` |
| 模组检测 | `ModList.get().isLoaded` | `FabricLoader.isModLoaded` |
| 服务端事件 | `PlayerEvent.PlayerLoggedInEvent` | `ServerPlayConnectionEvents.JOIN` |

**共享代码（Bob / StepTimer / RemoteSteps / Profile / ProfileStore / Mode / YsmClient / 三个 mixin）与 NeoForge 版逐字相同。**

## 构建

```bash
cd fabric
# 需要 Fabric 的参考 jar：Presence Footsteps（编译期只需要它的类，不打包）
#   放成 fabric/libs/presencefootsteps.jar
./gradlew build        # 或本机 gradle 8.10
```

产物：`fabric/build/libs/ysmstep-fabric-<version>.jar`

### 依赖版本

```
Minecraft      1.21.1
Fabric Loader  0.16.10
Fabric API     0.116.18+1.21.1
Loom           1.8.13（需要 Gradle >= 8.8；1.9+ 要 Gradle 8.11+）
映射           officialMojangMappings()（与 NeoForge 版类名一致）
```

### 两个版本适配的坑

```
· 1.21.1 的 Fabric API 没有 HudLayerRegistrationCallback（那是 1.21.4+），
  只能用已弃用的 HudRenderCallback，签名同样是 (GuiGraphics, DeltaTracker)
· vanilla 的 blockState.getSoundType() 无参；NeoForge 才有多参重载
· vanilla 的 SavedData.Factory 是三参（第三个 DataFixTypes 可为 null）
· Loom 1.8.x 没发布 plugin marker，只能用 buildscript classpath 引实现
```

## 运行时不依赖

**Presence Footsteps 是可选依赖。** 没装时 `PFCompat.present()` 为 false，走内置发声逻辑；
装了才把发声交给它、我们只定节拍。

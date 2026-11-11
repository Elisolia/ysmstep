# ysmstep — 让 YSM 模型的脚步声踩在它自己的步点上

> ⚠️ **非官方**。本项目与「是，史蒂夫模型」(Yes Steve Model)、Presence Footsteps 均无隶属关系，
> 不包含这两者的任何代码或资源。

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1%2B-CB9E5C)
![Fabric](https://img.shields.io/badge/Fabric-1.21.1-DBB69B)
![License](https://img.shields.io/badge/license-MIT-blue)

> 最新版本 **0.5.2**（2026-11-06）· [下载](https://github.com/Elisolia/ysmstep/releases/latest) · [更新日志](#更新日志)

---

## 它解决什么问题

```
YSM 的走路动画：按「动画时间」循环  → 周期固定（默认模型 1.0s / 跑步 0.667s）
原版脚步声：    按「走过的距离」触发 → 每 1.67 格一声（走路约 0.39s 一声）
```

两个钟不一样，**越走越错**。所以大多数 YSM 模型的脚落地和脚步声是对不上的——走两步，响三声，越走越飘。

ysmstep 把音效的节拍改成跟着**动画周期**走，于是脚步声和脚步动作重新对齐。

## 它怎么做的

### 1. 本地节拍器

- mixin 掉 `Player#playStepSound`（HEAD，可取消），压掉距离驱动那套原版声音
- 玩家在地面上移动时跑一个节拍器：`acc += 1 tick`，攒满 `interval` 响一声
- `interval = 动画周期 / 2`（一个走路循环 = 两步），跑步/潜行各有一个周期系数
- 声响取脚下方块的 `SoundType`，音量和音高与原版一致（`volume × 0.15`）
- 空中不推进、也不清零相位：跳过一格、落地抖动都不会把拍子打断

### 2. 按姿态换音色

有些模型跑步时整个人是**悬空**的（脚根本不落地），硬配脚步反而更假：

| 档 | 音效 | 用在 |
|---|---|---|
| `step` | 脚下方块的踩地声 | 默认 |
| `none` | 不出声 | 悬空的跑姿 |
| `wind` | `item.elytra.flying` | 悬浮、滑行 |
| `flap` | `entity.phantom.flap` | 飞行生物 |
| `soft` | 羊毛那种闷软声 | 轻足、灵体 |

走路 / 跑步 / 潜行可以分开设，例如「走路出声、跑步静音」。

### 3. Presence Footsteps 兼容（可选）

装了 [Presence Footsteps (NeoForge)](https://github.com/ZCRAFT-NPE/Presence-Footsteps-NeoForge) 时，
把「什么时候踩」交给 ysmstep，「踩出来是什么声」仍然交给 PF——材质关联、左右脚交替、
上下楼分档、体型放大后步幅变长全部保留。

做法是在 `TerrestrialStepSoundGenerator#simulateFootsteps` 的 HEAD 加一个闸门：
平时拦掉（PF 继续攒距离但不发声），到我们的节拍点放行一帧，攒下的距离刚好跨过阈值，
PF 用它自己的音色踩一步。跳跃落地、擦草、站立挪动的音效不拦。

### 4. 联机：让别人也听到对的

你校准的是**你的模型**，别人客户端不知道。所以 ysmstep 可以把档案传到服务端：

```
客户端 /ysmstep share 黑山羊之母
   ↓  C2S：周期 / 相位 / 各姿态系数与音色 / 模型名
服务端 存进 world/data/ysmstep_profiles.dat（byPlayer + byModel 两张表）
   ↓  S2C：登录时推送、有变更时广播
别人的客户端  给这个玩家单独跑一条节拍器，压掉他的原版脚步，按他的参数重播
```

服务端没装 ysmstep 时上传会静默失败（网络包是 optional 的，不会把你踢下线）。

### 5. 视角摇晃（可选，0.5.2 起）

原版镜头摇晃的相位也是 `walkDist`（走过的距离），和脚步声是同一个钟。脚步声搬到动画时钟
之后，镜头还留在距离时钟上——声音对了，镜头还和脚打架。

`bobMode = sync` 时，把 `bobView` 读的四个字段换掉（不改它的数学）：

- `walkDist / walkDistO` → 动画节拍换算的等效距离（一步 = `walkDist +1.0`，两次脚步一个摇晃周期，和原版比例一致）
- `bob / oBob` → 原值 × `bobScale`（幅度倍率，`0` = 完全不晃，上限 2）

只对本地玩家生效，第三人称看别人仍是原版。默认 `vanilla`（完全不动），想要的人自己开：

```
/ysmstep bob              在 vanilla / sync 之间切换
/ysmstep bobscale 0.8     摇晃幅度（默认 1.0；晕 3D 就调小）
```

镜头类模组（Do a Barrel Roll、Shoulder Surfing 等）叠加时是两条正弦叠在一起，可能更容易晕；
觉得晃就 `/ysmstep bob` 关掉，或把 `bobscale` 调到 0.5 以下。

## 安装

| 位置 | 需要 |
|---|---|
| 客户端 | 必需（渲染和播放都在客户端） |
| 服务端 | 可选；想要联机同步才装 |

依赖：NeoForge 21.1+ / Minecraft 1.21.1。**YSM 本身不是硬依赖**（不装也能跑，只是没意义）。
Presence Footsteps 可选。

## 快速开始

```
1. 进游戏，把「触拍」键绑一个（选项 → 控制 → YSM 脚步同步）
2. 开第三人称，盯着模型走路，跟着它的落地连点 4 下以上
   → 自动算出周期并保存
3. 走两步听一下；要是稳定地早/晚半拍，用 /ysmstep offset +0.05 挪相位
```

跑步悬空的模型（例如「黑山羊之母」）：

```
/ysmstep mode run none     跑步不出声
/ysmstep mode run wind     或者改成风声
```

## 命令

```
/ysmstep                      状态
/ysmstep on | off             开关（关掉 = 原版脚步声原样回来）
/ysmstep status               同上

/ysmstep cycle <值>           走路动画周期（0.2–4.0 秒），一步 = 半个周期
/ysmstep offset <值>          相位偏移（-1.0–1.0 秒）
/ysmstep run <系数>           跑步周期 = 走路周期 × 系数（默认 0.667）
/ysmstep sneak <系数>         潜行周期系数（默认 1.0）
/ysmstep follow <0–1>         0 = 固定节拍；1 = 完全跟移动速度缩放
/ysmstep volume <倍率>        音量倍率
/ysmstep mode <walk|run|sneak> <step|none|wind|flap|soft>

/ysmstep bob                  视角摇晃：原版 / 跟动画节拍（默认原版）
/ysmstep bobscale <倍率>      摇晃幅度（0–2，默认 1.0）

/ysmstep tap [step|cycle]     触拍一拍代表什么（一只脚落地 / 一整个循环）
/ysmstep test                 立刻播一声，验证声音通路
/ysmstep hud                  常驻 HUD
/ysmstep debug                诊断模式（写 config/ysmstep-debug.txt）
/ysmstep pf                   切换「是否让 Presence Footsteps 接管发声」

/ysmstep share [模型名]        把当前校准上传到服务端
/ysmstep apply                套用服务器里同模型名的档案
/ysmstep who                  看本机收到几份玩家档案
```

数值参数支持相对写法，省得每次敲全数：

```
/ysmstep cycle +0.05     加
/ysmstep cycle -0.05     减
/ysmstep cycle *2        翻倍
/ysmstep offset =0.1     要填负数时用 = 开头，避免和「减」混
```

配置文件：`config/ysmstep-client.json`

## 怎么判断该调周期还是相位

| 现象 | 是什么 | 怎么调 |
|---|---|---|
| 一会儿对上、一会儿又错，越走越明显 | 周期不对 | `/ysmstep cycle ±0.05` 调到不再漂 |
| 一直稳定地早/晚半步，但不会越走越错 | 相位不对 | `/ysmstep offset ±0.05` |

## 已知限制

- **周期要手动校准**：YSM 2.6.5 发的 jar 是全类名混淆的，没有可用的第三方接口读动画周期，
  模型文件（`.ysm`）内容是加密的，所以读不到。换模型要重校一次（触拍 4 下，几秒钟）。
- **模型名要手填**：`/ysmstep share 黑山羊之母` 里的名字只是给数据归类用，
  用来让「同一模型的其他玩家」自动有默认值，不读也不改模型文件。
- **Presence Footsteps 更新可能让它失效**：闸门是按类名 + 方法名挂上去的。
  PF 改了结构，`/ysmstep status` 会显示 `[PF闸门未触发]`，那时退回原版音色（`/ysmstep pf`）。
- **相位各端各算**：联机时每个客户端自己按收到的参数计时，会有几十毫秒的差，不是严格同相。
- **只管走路/跑步/潜行**：游泳、飞行、坐骑、鞘翅等姿态不接管，原版行为保留。
- **视角摇晃默认关**：它改的是镜头，不是声音；和别的相机模组叠加的效果没法替每个人保证，
  所以做成可选项。开了觉得晕就 `/ysmstep bob` 关掉。
- **只处理 64 格内的其他玩家**，更远的客户单不算（也听不见）。

## 构建

需要 JDK 21。仓库自带 Gradle wrapper：

```bash
./gradlew build          # Windows: gradlew.bat build
# 产物：build/libs/ysmstep-<version>.jar
```

编译依赖 NeoForge 21.1 工具链；运行环境 21.1.236 起都能跑（只用稳定的 API）。

## 更新日志

### 0.5.2（2026-11-06）

- 新增 **视角摇晃同步**（可选，默认原版）：`/ysmstep bob` 打开后，镜头摇晃的相位改由动画节拍驱动，
  和脚步声同一个钟；`/ysmstep bobscale 0.8` 调幅度（0–2）。
- 修正摇晃相位换算：一步 = `walkDist +1.0`（按 `Entity#move` 的实际比例），
  早期构建里写成 0.6，会让摇晃周期比脚步慢一倍。

### 0.5.1（2026-11-05）

- 修复 `Net.register` 漏 `@SubscribeEvent` 导致的启动崩溃。

### 0.5.0（2026-11-05）

- 双端：服务端逐玩家脚步档案缓存与广播，别人也能听到你的模型脚步（可选装服务端）。
- 按姿态选择发声方式：`step` / `none` / `wind` / `flap` / `soft`。
- `/ysmstep share`、`/ysmstep apply`、`/ysmstep who` 联机命令。

## 致谢

- [是，史蒂夫模型 (Yes Steve Model)](https://github.com/YesSteveModel/YesSteveModel) —— 本项目的存在理由
- [Presence Footsteps](https://github.com/Sollace/Presence-Footsteps) /
  [NeoForge 移植](https://github.com/ZCRAFT-NPE/Presence-Footsteps-NeoForge) —— 兼容层参考了它们的源码结构

两者均为独立项目，本项目**不包含**它们的任何代码、模型或音效资源。

## 许可

[MIT](LICENSE)

---

## 加载器支持

| 加载器 | 目录 | 产物 |
|---|---|---|
| NeoForge 1.21.1 | 仓库根目录 | `build/libs/ysmstep-<version>.jar` |
| Fabric 1.21.1 | `fabric/` | `fabric/build/libs/ysmstep-fabric-<version>.jar` |

两个版本共用同一套游戏逻辑代码；构建方法见 [fabric/README.md](fabric/README.md)。

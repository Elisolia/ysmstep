# ysmstep 0.5.2 —— NeoForge / Fabric 双加载器

> 发布日期：2026-11-11

## 下载

| 加载器 | 文件 | 说明 |
|---|---|---|
| NeoForge 1.21.1 | `ysmstep-0.5.2.jar` | 需要 NeoForge 21.1+ |
| Fabric 1.21.1 | `ysmstep-fabric-0.5.2.jar` | 需要 Fabric Loader 0.16+ 与 Fabric API |

**两个版本功能完全相同，装其中一个即可。** 不要把两个 jar 同时放进同一个 `mods/` 目录。

## 这个模组解决什么

YSM 的走路动画按「动画时间」循环（周期固定），原版脚步声按「走过的距离」触发。两个时钟不同，走久了脚步声就和模型落地对不上。

**它把脚步声改成按动画周期发声：** 压掉原版距离驱动的脚步声，改用节拍器，一步 = 半个动画周期。跑步/潜行各有周期系数，声音取脚下方块的 `SoundType`，音量音高与原版一致。

**可选：** 视角摇晃也能跟着动画节拍走（`/ysmstep bob` 切换），默认关闭。

## 依赖

```
必需   Minecraft 1.21.1
可选   是，史蒂夫模型（YSM）—— 本来就是为它做的
可选   Presence Footsteps —— 装了就由它发声，本模组只定节拍
```

**Presence Footsteps 是可选依赖：** 装了会把发声交给它（保留它自己的材质关联、左右脚交替），没装就用内置发声。

## 安装

1. 把对应加载器的 jar 放进 `mods/`
2. 启动游戏，第一次会生成 `config/ysmstep-client.json`
3. 进游戏敲 `/ysmstep status` 看是否加载

**联机：** 客户端装了就能听见自己的脚步声；想让别人也听见你的，服务端也装同一个 jar，玩家用 `/ysmstep share` 上传校准过的档案。

## 快速开始

1. 准备一个 YSM 模型，走两步，注意脚步和落地对不对得上
2. `/ysmstep cycle 1.1`（或更小）调到对上为止
3. 还差半拍就用 `/ysmstep offset +0.05` 微调相位
4. 跑/潜行对不上，分别调 `/ysmstep run` `/ysmstep sneak`
5. 校准满意后 `/ysmstep share 模型名` 上传，别人用同一模型就自动套用

**不想手调：** 绑上「触拍」键，跟着模型落地连点 4 下以上，`/ysmstep tap` 会用中位数自动算周期。

## 命令

```
/ysmstep                状态
/ysmstep test           手动踩一脚
/ysmstep hud on|off     HUD 开关
/ysmstep on|off         总开关（off = 原版脚步声回来）
/ysmstep pf             是否把发声交给 Presence Footsteps
/ysmstep cycle 1.2      周期（秒）；支持 +0.05 / -0.05 / *2 / /2
/ysmstep offset =0.1    相位（秒，负值用 = 开头）
/ysmstep run 0.667      跑步周期系数
/ysmstep sneak 1.0      潜行周期系数
/ysmstep follow 0.3     节拍跟随速度的比例（0 = 完全固定）
/ysmstep volume 1.0     音量倍率（原版 0.15）
/ysmstep bob            视角摇晃是否跟随动画
/ysmstep bobscale 0.5   摇晃幅度倍率（0 = 不晃）
/ysmstep tap [step|cycle] 用触拍自动校准周期
/ysmstep share [模型名]  上传当前校准到服务端
/ysmstep apply          套用服务端下发的模板
/ysmstep mode walk run 状态发声方式（step/none/wind/flap/soft）
```

**键位默认全不绑** —— 288 个模组的环境里绑了会撞。要用就去「选项 → 控制」自己设。

## 已知限制

- **周期要手动校准**：YSM 2.6.5 的 jar 是全类名混淆的，没有可用的第三方接口读动画周期，只能人耳对表或用触拍
- **模型名要手填**：`/ysmstep share 黑山羊之母` 里的名字只用于数据归类，不读取模型文件
- **Presence Footsteps 更新可能让它失效**：闸门按类名 + 方法名挂，PF 改结构就得跟着改
- **相位各端各算**：联机时每个客户端按收到的参数自己计时，会有几十毫秒的差，不是严格同相
- **只管走路/跑步/潜行**：游泳、飞行、坐骑、鞘翅等不接管，保留原版行为
- **视角摇晃默认关**：它改的是镜头不是声音，和别的相机模组叠加的效果没法替每个人保证

## 0.5.2 变更

- **新增 Fabric 1.21.1 支持**，与 NeoForge 版共用同一套游戏逻辑代码
- 视角摇晃可跟随动画节拍（`/ysmstep bob`、`/ysmstep bobscale`）

**Fabric 版的构建方式、版本对照和四个移植坑见 [fabric/README.md](../fabric/README.md)。**

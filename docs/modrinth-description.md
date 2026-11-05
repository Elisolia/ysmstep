# ysmstep

**Retimes footstep sounds to a Yes Steve Model animation clock.**
让 YSM 模型的脚步声踩在模型自己的动画节拍上。

> ⚠️ Unofficial. Not affiliated with Yes Steve Model or Presence Footsteps. This project
> contains no code or assets from either mod. / 非官方项目，与「是，史蒂夫模型」和 Presence
> Footsteps 均无隶属关系，不包含这两者的任何代码或资源。

---

## The problem / 问题

```
YSM walk animation: loops on animation time  → fixed period (default 1.0s, run 0.667s)
Vanilla footsteps:  trigger on distance moved → one step per 0.6 block (~0.39s)
```

Two different clocks. They drift apart, so on most models the footfall and the sound do not
line up — you walk two steps and hear three.

YSM 的走路动画按「动画时间」循环，原版脚步声按「走过的距离」触发。两个钟不一样，越走越错——
走两步响三声，脚落地和声音对不上。

---

## What it does / 它做什么

- **Animation-clock metronome.** Vanilla `Player#playStepSound` is suppressed; a metronome
  fires a step every half animation cycle, so sound and footfall stay locked.
- **Airborne-safe phase.** The phase does not advance or reset while airborne, so hopping a
  ledge or a one-tick `onGround` flicker no longer knocks the beat off.
- **Per-posture sound modes.** `step` / `none` / `wind` / `flap` / `soft` — for models that
  hover or never touch the ground while running, `none` or `wind` fits better than a footstep.
- **Presence Footsteps compatible.** When PF is installed, ysmstep decides *when* a step
  happens and PF keeps deciding *what it sounds like* (materials, left/right foot, stairs,
  sneaking, body scale).
- **Server-synced calibration.** Your calibration is uploaded to the server, stored in the
  world save and broadcast, so other players hear your footsteps on your model's beat.

- **动画时钟节拍器**：压掉原版脚步声，用「半个动画周期」作为一步的间隔。
- **空中不打断相位**：过坎、落地的单帧抖动不会把拍子拽偏。
- **按姿势换音色**：跑步悬空的模型可以用 `none` 或 `wind`，不必硬配踩地声。
- **兼容 Presence Footsteps**：我们管「什么时候踩」，PF 管「踩出来什么声」。
- **联机同步**：校准上传服务端并广播，别人按你的参数听你的脚步。

---

## Install / 安装

| Side | Needed |
|---|---|
| Client / 客户端 | **Required / 必需** |
| Server / 服务端 | Optional — only for synced calibration / 可选，想要联机同步才装（同一个 jar） |

Requires NeoForge 21.1+ on Minecraft 1.21.1.
Optional: [Presence Footsteps (NeoForge)](https://modrinth.com/mod/pf-neoforge).
**Yes Steve Model is not included** — install it separately.

---

## Getting started / 上手

```
1. Bind the "tap tempo" key (Options → Controls → YSM 脚步同步)
   绑「触拍」键（选项 → 控制 → YSM 脚步同步）
2. Stand in third person, walk, and tap along with the model's footfalls 4+ times
   → the cycle length is computed and saved
   开第三人称，跟着模型落地连点 4 下以上，自动算出周期
3. If it is consistently half a beat early/late: /ysmstep offset +0.05
   要是稳定地早/晚半步，用 /ysmstep offset +0.05 挪相位
4. Running while airborne: /ysmstep mode run none
   跑步整个人悬空的模型：/ysmstep mode run none
```

---

## Commands / 命令

```
/ysmstep                        status / 状态
/ysmstep on | off               toggle / 开关
/ysmstep cycle <seconds>        walk animation cycle length (0.2–4.0)
/ysmstep offset <seconds>       phase offset (-1.0–1.0)
/ysmstep run <factor>           run cycle = walk cycle × factor (default 0.667)
/ysmstep sneak <factor>         sneak cycle factor (default 1.0)
/ysmstep follow <0–1>           0 = fixed beat, 1 = follow movement speed
/ysmstep volume <multiplier>    volume multiplier
/ysmstep mode <walk|run|sneak> <step|none|wind|flap|soft>
/ysmstep tap [step|cycle]       what one tap means (one footfall / one full cycle)
/ysmstep test                   play one step now to verify audio
/ysmstep hud                    persistent HUD
/ysmstep debug                  diagnostics → config/ysmstep-debug.txt
/ysmstep pf                     toggle "let Presence Footsteps voice the steps"
/ysmstep share [model name]     upload calibration to the server
/ysmstep apply                  apply a stored calibration for the same model name
/ysmstep who                    how many player profiles this client has
```

Numeric values accept relative forms: `+0.05`, `-0.05`, `*2`, and `=0.1` for an absolute
negative. 数值参数支持相对写法：`+0.05` / `-0.05` / `*2` / `=0.1`（填负数用 `=` 开头）。

---

## Known limitations / 已知限制

- **Calibration is manual, once per model.** YSM does not expose its animation length and
  `.ysm` model files are encrypted, so the cycle has to be measured by ear (4 taps).
  周期需要按模型手动校准一次。
- **Model name is typed by hand** (`/ysmstep share 黑山羊之母`). It is only a label used to
  give other players of the same model a default; model files are never read or modified.
- **Presence Footsteps updates may break the compatibility layer.** It works by targeting
  `TerrestrialStepSoundGenerator#simulateFootsteps`; if that changes, ysmstep reports
  `[PF闸门未触发]` in-game and falls back to vanilla step sounds. No crash.
- **Phase is computed per client.** With synced profiles each client times its own beat;
  expect tens of milliseconds of difference, not strict phase lock.
- **Walking / running / sneaking only.** Swimming, flying, riding and elytra keep vanilla
  behaviour.
- **Other players are only handled within 64 blocks** (further away you would not hear them).

---

## Source / 源码

https://github.com/Elisolia/ysmstep — MIT

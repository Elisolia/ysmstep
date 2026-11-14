# ysmstep

**Retimes footstep sounds to a Yes Steve Model animation clock.**

> Unofficial project. Not affiliated with Yes Steve Model or Presence Footsteps. It contains no code or assets from either mod.

## Relationship to other mods — this is not a derivative work

ysmstep is an independent, standalone mod written from scratch. It is **not a fork, port, or derivative** of Yes Steve Model, Presence Footsteps, or any other project.

- It ships **no code and no assets** from those mods — no models, textures, animations, or sounds.
- It does **not** modify or redistribute them. It only changes the timing of vanilla footstep sounds.
- Yes Steve Model is not required at build time and nothing from it is referenced; the mod works out what beat to use from player movement and a manually measured cycle length.
- Presence Footsteps support is optional and limited to a single mixin hook that skips its own step trigger; that mod stays in charge of what a step sounds like.

Interoperability with those mods is the whole point of the project, in the same way any compatibility addon interoperates with what it supports. Credits for the mods this project interoperates with are listed at the bottom of this page.

Licensed MIT, source code public.

---

## The problem

Yes Steve Model plays walk animations on **animation time**, so the cycle length is fixed (1.0 s by default, 0.667 s when running). Vanilla Minecraft triggers footsteps on **distance travelled** — roughly one step every 0.6 blocks, about 0.39 s while walking.

Two independent clocks. They drift apart immediately, so on most models the footfall and the sound do not line up anymore: you take two steps and hear three. The longer you walk, the worse it gets.

## What it does

- **Animation-clock metronome.** Vanilla `Player#playStepSound` is suppressed and replaced with a metronome that fires one step every half animation cycle, so the sound stays locked to the model's footfall.
- **Airborne-safe phase.** The phase neither advances nor resets while you are in the air, so hopping a ledge or a one-tick `onGround` flicker no longer knocks the beat off.
- **Per-posture sound modes.** Walking, running and sneaking can each be set to `step`, `none`, `wind`, `flap` or `soft`. For models that hover or never touch the ground while running, `none` or `wind` sounds more natural than a footstep.
- **Presence Footsteps compatible.** When Presence Footsteps (NeoForge) is installed, ysmstep decides *when* a step happens and Presence Footsteps keeps deciding *what it sounds like* — block materials, left/right alternation, stairs, sneaking, body scale.
- **Server-synced calibration.** Your calibration can be uploaded to the server, stored in the world save and broadcast, so other players hear your footsteps on your model's beat.
- **Optional camera bob sync.** The view bob can be driven by the same metronome instead of the vanilla distance clock (`/ysmstep bob`).

## Requirements

- Minecraft 1.21.1, NeoForge 21.1+
- Yes Steve Model must be installed separately; this mod ships none of its assets or code
- Presence Footsteps (NeoForge) is optional

Client required. The server side is optional and uses the same jar — install it only if you want calibration synced between players.

## Getting started

1. Bind the "tap tempo" key under Options → Controls → YSM 脚步同步.
2. Stand in third person, walk, and tap along with the model's visible footfalls 4 or more times. The cycle length is computed and saved per model.
3. If the sound sits consistently half a beat early or late, nudge the phase with `/ysmstep offset +0.05`.
4. For models that hover while running, use `/ysmstep mode run none` (or `wind`).

## Commands

- `/ysmstep` — status
- `/ysmstep on` / `/ysmstep off` — toggle; turning it off restores vanilla footsteps
- `/ysmstep cycle <seconds>` — walk animation cycle length (0.2–4.0)
- `/ysmstep offset <seconds>` — phase offset (-1.0–1.0)
- `/ysmstep run <factor>` — run cycle as a multiple of the walk cycle (default 0.667)
- `/ysmstep sneak <factor>` — sneak cycle factor (default 1.0)
- `/ysmstep follow <0–1>` — 0 = fixed beat, 1 = follow movement speed
- `/ysmstep volume <multiplier>` — volume multiplier
- `/ysmstep mode <walk|run|sneak> <step|none|wind|flap|soft>` — sound mode per posture
- `/ysmstep tap [step|cycle]` — what a single tap means
- `/ysmstep test` — play one step immediately, to check the audio path
- `/ysmstep hud` — persistent on-screen status panel
- `/ysmstep debug` — write diagnostics to `config/ysmstep-debug.txt`
- `/ysmstep pf` — toggle "let Presence Footsteps voice the steps"
- `/ysmstep share [model name]` — upload calibration to the server
- `/ysmstep apply` — apply a stored calibration for the same model name
- `/ysmstep bob` / `/ysmstep bobscale <0–2>` — camera bob source and amplitude

Numeric arguments accept relative forms: `+0.05`, `-0.05`, `*2`; use `=0.1` to enter an absolute negative value.

## Known limitations

- **Calibration is manual, once per model.** Yes Steve Model does not expose its animation length, and `.ysm` model files are encrypted, so the cycle has to be measured by ear.
- **The model name is typed by hand** when sharing. It is only a label used to offer other players a default; model files are never read or modified.
- **Presence Footsteps updates can break the compatibility layer.** It works by targeting `TerrestrialStepSoundGenerator#simulateFootsteps`; if that changes, the mod reports `[PF gate not triggered]` in game and falls back to vanilla step sounds instead of crashing.
- **Phase is computed on each client.** With synced profiles every client times its own beat — expect tens of milliseconds of difference, not strict phase lock.
- **Walking, running and sneaking only.** Swimming, flying, riding and elytra keep vanilla behaviour.
- **Other players are only handled within 64 blocks.**

## 中文说明

YSM 的走路动画按「动画时间」循环，原版脚步声按「走过的距离」触发。两个钟不一样，越走越错——走两步响三声，脚落地和声音对不上。这个模组压掉原版那套距离触发的脚步声，改用一个按动画周期推进的节拍器发声；装了 Presence Footsteps 时，我们只决定「什么时候踩」，踩出来什么声仍由 PF 决定。周期需要按模型手动校准一次（连点 4 下），校准结果可以上传到服务端广播给同模型的玩家。

完整命令表、校准方法与原理见项目页与源码仓库。

## Works with

These are separate projects by other authors. They are **not bundled, not embedded, and not
required to build** ysmstep — no code or assets from them are included in this project.

- **Yes Steve Model** (by 寒叔 / 空气避难所) — the mod that draws player models in game.
  ysmstep does not read or modify its files; it only keeps footstep audio on the same beat as
  the animation the player sees. Useful only when this mod is installed; install it separately.
- **Presence Footsteps** (by Hurricaaane (Ha3), Sollace and contributors; NeoForge port by
  Paint_Ninja and ZCRAFT) — optional. When present, ysmstep decides *when* a step happens and
  Presence Footsteps keeps deciding *what* it sounds like.

Licensed under MIT.

## 0.5.3 — in-game settings panel

No more tuning through commands: there's a real panel now (**Ctrl + Y** by default).

### New: settings panel

- **3D preview** with walk / run / idle playback — you can watch exactly when the foot lands.
  Left-drag pans, right-drag rotates, wheel zooms.
- **Cycle & phase bar**: one loop across, two tick marks for the two footfalls, a playhead that
  moves and flashes on each step. Alignment becomes something you can *see*.
- Grouped into **Voice / Timing / Misc**.

### New: listen while you tune

- Pick walk or run and footsteps play live with the **current cycle and phase**.
- **Tap calibrate** silences the metronome for one and a half beats and plays exactly one step per
  press — tap 4+ times along with the model's footfalls and the cycle is computed for you
  (the button counts up as `0/4 … 4/4`).
- With Presence Footsteps installed and compatibility enabled, its own material sounds are used.

### New: wheel adjustment

| Input | Step |
|---|---|
| wheel | 0.1 |
| Shift + wheel | ×10 |
| Ctrl + wheel | ×0.1 |
| Alt + wheel (phase only) | ×0.01 |

Each slider also has a one-click reset (1.00s / 0 / ×1.00).

### Improved

- Voice mode (`step` / `none` / `wind` / `flap` / `soft`) is a **dropdown with explanations**, so
  you don't have to remember which word means "floating model with no footsteps".
- **Light and dark themes** (bottom-right switch, dark by default, remembered in config).
- Camera bobbing is forced to **zero for postures that don't touch the ground** — flying and
  roller-skating models no longer sway the camera in sync with a footstep that never happens.

### Fixed

- The mod list showed a stale version number (hardcoded in the metadata) — it now follows the jar.
- Added the mod icon.

### Loaders

- **NeoForge**: `ysmstep-0.5.3-neoforge.jar`
- **Fabric**: `ysmstep-0.5.3-fabric.jar`

Install only the jar that matches your loader. Both are feature-identical; on Fabric the panel key
appears as `Y` in Controls (the platform has no modifier-key layer) but Ctrl must still be held.

### Upgrading

Client-side only. Replace the jar — your calibration in `config/ysmstep-client.json` is kept.

package ysmstep;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 节拍器本体：玩家在跑动时按周期播一声脚步。
 *
 * 单位都是 tick（20 / 秒）。acc 攒满 interval 就响一声。
 *
 * 0.2.1 修的两个坑（2026-11-05 实测日志里抓到的）：
 * · 走过坎时 onGround 会抖一帧 → 以前会立刻把相位清零，节拍被"拽"一下。
 *   现在：空中不推进、但 4 tick 内的抖动不清相位。
 * · 空中不发声也不推进，落地后接着走原来的拍子。
 */
public class StepTimer {

    /** 一次抖动最多容忍几 tick 不清相位。 */
    private static final int GRACE_TICKS = 4;

    private double acc = 0.0;
    private boolean wasMoving = false;
    private int idleTicks = 0;
    private int tickCount = 0;

    /** 累计走了多少步（小数部分由 acc 提供），给视角摇晃换算相位用。 */
    private double stepsTotal = 0.0;

    /** 最近一次算出的步间隔（tick）。停下后沿用，摇晃相位不会跳。 */
    private double intervalNow = 0.0;

    /** HUD 闪点用：每响一声设成 4，然后慢慢减。 */
    public int stepFlash = 0;

    /** 0.5.3：面板打开期间把节拍器静音，改由玩家按键试听。 */
    public static boolean muted = false;

    // 给 HUD / 调试看的最近一帧状态
    public double dbgAcc = 0.0;
    public double dbgInterval = 0.0;
    public double dbgSpeed = 0.0;
    public boolean dbgOnGround = false;
    public boolean dbgMoving = false;
    public int dbgSteps = 0;
    public String dbgLast = "";

    public void reset() {
        acc = 0.0;
        wasMoving = false;
        idleTicks = 0;
        stepsTotal = 0.0;
        intervalNow = 0.0;
        Bob.reset();
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) {
            return;
        }
        Bob.beginTick();
        if (stepFlash > 0) {
            stepFlash--;
        }
        tickCount++;

        StepConfig cfg = YsmClient.CONFIG;
        double hs = Math.sqrt(p.getDeltaMovement().x * p.getDeltaMovement().x
                + p.getDeltaMovement().z * p.getDeltaMovement().z);
        dbgSpeed = hs;
        dbgOnGround = p.onGround();

        boolean active = cfg.enabled && wantsToWalk(p, hs);
        dbgMoving = active && p.onGround();

        if (!active) {
            // 停下来超过 GRACE_TICKS 才算真的停：下一段起步从模型循环的起点重新开始
            if (++idleTicks > GRACE_TICKS) {
                acc = cfg.offsetSeconds * 20.0;
                wasMoving = false;
            }
            dbgAcc = acc;
            publishPhase();
            heartbeat(p);
            return;
        }
        idleTicks = 0;

        if (!p.onGround()) {
            // 空中：不推进也不清零（跳过坑、跳一下都不会打破拍子）
            dbgAcc = acc;
            publishPhase();
            heartbeat(p);
            return;
        }

        if (!wasMoving) {
            acc = cfg.offsetSeconds * 20.0;
            wasMoving = true;
        }

        double interval = intervalTicks(p, hs);
        dbgInterval = interval;
        intervalNow = interval;
        acc += 1.0;
        int guard = 0;
        while (acc >= interval && guard++ < 8) {
            acc -= interval;
            // 面板打开时静音：让玩家自己按键试听，避免和节拍器混在一起听不清
            if (!muted) {
                emit(p, modeFor(p));
            }
        }
        dbgAcc = acc;
        publishPhase();
        heartbeat(p);
    }

    /** 把"走到第几步"换算成视角摇晃要的相位（每步 0.6 格，和原版脚步与距离的比例一致）。 */
    private void publishPhase() {
        double interval = intervalNow > 0.0 ? intervalNow : Math.max(1.0, YsmClient.CONFIG.stepIntervalTicks());
        double within = acc / interval;
        if (within < 0.0 || within > 1.0) {
            within = 0.0;
        }
        Bob.setSteps(stepsTotal + within);
    }

    private void heartbeat(LocalPlayer p) {
        if (!YsmClient.CONFIG.debug || tickCount % 10 != 0) {
            return;
        }
        StepDebug.log(String.format(
                "t=%d en=%b og=%b hs=%.3f moving=%b acc=%.2f iv=%.2f steps=%d pose=%s",
                tickCount, YsmClient.CONFIG.enabled, p.onGround(), dbgSpeed, dbgMoving,
                dbgAcc, dbgInterval, dbgSteps, p.getPose()));
    }

    /** 想不想走（不看 onGround：空中也算在走，只是不发声）。 */
    public static boolean wantsToWalk(LocalPlayer p, double horizontalSpeed) {
        return horizontalSpeed > 0.02
                && !p.isPassenger()
                && !p.isInWater()
                && !p.isSwimming()
                && !p.isFallFlying()
                && !p.isSpectator();
    }

    private double intervalTicks(LocalPlayer p, double hs) {
        StepConfig cfg = YsmClient.CONFIG;
        double cycle = cfg.cycleSeconds;
        if (p.isSprinting()) {
            cycle *= cfg.runFactor;
        } else if (p.isShiftKeyDown()) {
            cycle *= cfg.sneakFactor;
        }

        double interval = cycle * 20.0 / 2.0;

        if (cfg.speedFollow > 0) {
            double scale = 1.0 + cfg.speedFollow * (hs / StepConfig.WALK_SPEED_REF - 1.0);
            scale = Math.max(0.4, scale);
            interval /= scale;
        }
        return Math.max(1.0, interval);
    }

    /** 当前状态该用哪种发声方式。 */
    public static Mode modeFor(LocalPlayer p) {
        StepConfig c = YsmClient.CONFIG;
        if (p.isSprinting()) {
            return Mode.parse(c.runMode);
        }
        if (p.isShiftKeyDown()) {
            return Mode.parse(c.sneakMode);
        }
        return Mode.parse(c.walkMode);
    }

    /** 一拍。 */
    private void emit(LocalPlayer p, Mode mode) {
        stepsTotal += 1.0;
        switch (mode) {
            case NONE -> {
                dbgSteps++;
                dbgLast = "静音（这个姿态不出声）";
                logBeat(p, "NONE");
                stepFlash = 4;
            }
            case WIND -> {
                dbgSteps++;
                p.playSound(SoundEvents.ELYTRA_FLYING, 0.20F * (float) YsmClient.CONFIG.volume, 0.9F);
                dbgLast = "风声 item.elytra.flying";
                logBeat(p, "WIND");
                stepFlash = 4;
            }
            case FLAP -> {
                dbgSteps++;
                p.playSound(SoundEvents.PHANTOM_FLAP, 0.25F * (float) YsmClient.CONFIG.volume, 1.0F);
                dbgLast = "拍翅 entity.phantom.flap";
                logBeat(p, "FLAP");
                stepFlash = 4;
            }
            case SOFT -> {
                dbgSteps++;
                SoundType wool = SoundType.WOOL;
                p.playSound(wool.getStepSound(), wool.getVolume() * 0.15F * (float) YsmClient.CONFIG.volume, wool.getPitch());
                dbgLast = "软脚 " + wool.getStepSound().getLocation();
                logBeat(p, "SOFT");
                stepFlash = 4;
            }
            default -> playStep(p);
        }
    }

    private void logBeat(LocalPlayer p, String tag) {
        if (YsmClient.CONFIG.debug && dbgSteps <= 200) {
            StepDebug.log("STEP #" + dbgSteps + " " + tag + " hs=" + String.format("%.3f", dbgSpeed));
        }
    }

    private void playStep(LocalPlayer p) {
        dbgSteps++;
        // 装了 Presence Footsteps：时机交给我们、声音交给它
        if (PFCompat.handoff()) {
            dbgLast = "PF 出声";
            logBeat(p, "PF");
            stepFlash = 4;
            return;
        }
        // 和原版 Entity#playStepSound 完全同一条路：Entity#playSound
        BlockPos pos = p.getOnPos();
        BlockState state = p.level().getBlockState(pos);
        SoundType type = state.getSoundType();
        SoundEvent sound = type.getStepSound();
        float volume = type.getVolume() * 0.15F * (float) YsmClient.CONFIG.volume;
        float pitch = type.getPitch();
        dbgLast = String.format("pos=%s block=%s sound=%s vol=%.3f pitch=%.2f",
                pos, state.getBlock().toString(), sound.getLocation(), volume, pitch);

        p.playSound(sound, volume, pitch);

        if (YsmClient.CONFIG.debug && dbgSteps <= 200) {
            StepDebug.log("STEP #" + dbgSteps + " STEP " + dbgLast
                    + " sprint=" + p.isSprinting() + " sneak=" + p.isShiftKeyDown()
                    + " hs=" + String.format("%.3f", dbgSpeed));
        }
        stepFlash = 4;
    }

    /** 从配置里拿出一句话给 HUD 用。 */
    public static String stateLine() {
        StepConfig c = YsmClient.CONFIG;
        String pf = PFCompat.gateState();
        String modes = "";
        if (!"step".equals(c.walkMode) || !"step".equals(c.runMode) || !"step".equals(c.sneakMode)) {
            modes = String.format(" 走=%s 跑=%s 潜=%s", c.walkMode, c.runMode, c.sneakMode);
        }
        return String.format("YSM脚步 %s 周期 %.3fs(一步%.3fs) 相位 %+.3fs 跑x%.2f 跟随%.0f%%%s%s",
                c.enabled ? "开" : "关", c.cycleSeconds, c.cycleSeconds / 2,
                c.offsetSeconds, c.runFactor, c.speedFollow * 100, modes, pf);
    }

    /** 实时状态行（HUD 第二行）。 */
    public String liveLine() {
        return String.format("%s acc %.2f/%.2f og=%s hs=%.3f mov=%s steps=%d",
                stepFlash > 0 ? "●" : "○", dbgAcc, dbgInterval,
                dbgOnGround ? "1" : "0", dbgSpeed, dbgMoving ? "1" : "0", dbgSteps);
    }

    /** /ysmstep test：不管条件，立刻响一声，并返回解析结果。 */
    public static String testStep() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            return "没有玩家";
        }
        BlockPos pos = p.getOnPos();
        BlockState state = p.level().getBlockState(pos);
        SoundType type = state.getSoundType();
        SoundEvent sound = type.getStepSound();
        float volume = type.getVolume() * 0.15F * (float) YsmClient.CONFIG.volume;
        p.playSound(sound, volume, type.getPitch());
        String msg = String.format("test: block=%s sound=%s vol=%.3f pos=%s",
                state.getBlock().toString(), sound.getLocation(), volume, pos);
        StepDebug.log("TEST " + msg);
        return msg;
    }
}

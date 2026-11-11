package ysmstep;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;


import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * 客户端命令：/ysmstep
 *
 * 调参支持三种写法（省得每次敲全数）：
 *   /ysmstep cycle 1.2     直接给绝对值
 *   /ysmstep cycle +0.05   加
 *   /ysmstep cycle -0.05   减
 *   /ysmstep cycle *2      翻倍； /2 减半
 *   /ysmstep offset =0.1   负的绝对值用 = 开头，避免和"减"混淆
 */
public class StepCommands {

    private static final long[] TAPS = new long[16];
    private static int tapCount = 0;

    public static <S> void register(CommandDispatcher<S> d) {
        LiteralArgumentBuilder<S> root = LiteralArgumentBuilder.<S>literal("ysmstep")
                .executes(ctx -> status());

        sub(root, "status", StepCommands::status);
        sub(root, "test", () -> tell(StepTimer.testStep()));
        sub(root, "hud", () -> {
            YsmClient.CONFIG.hud = !YsmClient.CONFIG.hud;
            YsmClient.CONFIG.save();
            return tell("HUD " + (YsmClient.CONFIG.hud ? "开" : "关"));
        });
        sub(root, "debug", () -> {
            YsmClient.CONFIG.debug = !YsmClient.CONFIG.debug;
            YsmClient.CONFIG.save();
            if (YsmClient.CONFIG.debug) {
                StepDebug.reset();
                StepDebug.log("debug on");
            }
            return tell("调试 " + (YsmClient.CONFIG.debug ? "开（走两步，我读 config/ysmstep-debug.txt）" : "关"));
        });
        sub(root, "on", () -> {
            YsmClient.CONFIG.enabled = true;
            YsmClient.CONFIG.save();
            return tell("开");
        });
        sub(root, "off", () -> {
            YsmClient.CONFIG.enabled = false;
            YsmClient.CONFIG.save();
            return tell("关（原版脚步声恢复）");
        });
        sub(root, "pf", () -> {
            if (!PFCompat.present()) {
                return tell("没装 Presence Footsteps");
            }
            YsmClient.CONFIG.pfCompat = !YsmClient.CONFIG.pfCompat;
            YsmClient.CONFIG.save();
            return tell("Presence Footsteps " + (YsmClient.CONFIG.pfCompat ? "接管发声（我们只定节拍）" : "不接管（用原版音色）"));
        });

        sub(root, "bob", () -> {
            StepConfig c = YsmClient.CONFIG;
            c.bobMode = "sync".equals(c.bobMode) ? "vanilla" : "sync";
            c.save();
            return tell(Bob.stateLine());
        });

        knob(root, "cycle", 0.2, 4.0, () -> YsmClient.CONFIG.cycleSeconds,
                v -> YsmClient.CONFIG.cycleSeconds = v,
                "周期", v -> String.format("%.3fs（一步 %.3fs）", v, v / 2));
        knob(root, "bobscale", 0.0, 2.0, () -> YsmClient.CONFIG.bobScale,
                v -> YsmClient.CONFIG.bobScale = v,
                "摇晃幅度", v -> String.format("x%.2f", v));
        knob(root, "offset", -1.0, 1.0, () -> YsmClient.CONFIG.offsetSeconds,
                v -> YsmClient.CONFIG.offsetSeconds = v,
                "相位", v -> String.format("%+.3fs", v));
        knob(root, "run", 0.3, 1.5, () -> YsmClient.CONFIG.runFactor,
                v -> YsmClient.CONFIG.runFactor = v,
                "跑步系数", v -> String.format("x%.3f", v));
        knob(root, "sneak", 0.3, 1.5, () -> YsmClient.CONFIG.sneakFactor,
                v -> YsmClient.CONFIG.sneakFactor = v,
                "潜行系数", v -> String.format("x%.3f", v));
        knob(root, "follow", 0.0, 1.0, () -> YsmClient.CONFIG.speedFollow,
                v -> YsmClient.CONFIG.speedFollow = v,
                "速度跟随", v -> String.format("%.0f%%", v * 100));
        knob(root, "volume", 0.0, 4.0, () -> YsmClient.CONFIG.volume,
                v -> YsmClient.CONFIG.volume = v,
                "音量", v -> String.format("x%.2f", v));

        root.then(LiteralArgumentBuilder.<S>literal("share")
                .executes(ctx -> share(""))
                .then(RequiredArgumentBuilder.<S, String>argument("model", StringArgumentType.greedyString())
                        .executes(ctx -> share(StringArgumentType.getString(ctx, "model")))));

        root.then(LiteralArgumentBuilder.<S>literal("apply").executes(ctx -> {
            Profile p = RemoteSteps.takeTemplate();
            if (p == null) {
                return tell("没有待套用的模板（服务器里没这个模型的档案，或已经用过了）");
            }
            applyProfile(p);
            return tell("已套用：" + p.summary());
        }));

        root.then(LiteralArgumentBuilder.<S>literal("who").executes(ctx -> tell(
                RemoteSteps.size() == 0
                        ? "本机还没收到任何档案"
                        : "已收到 " + RemoteSteps.size() + " 份玩家档案")));

        root.then(LiteralArgumentBuilder.<S>literal("mode")
                .executes(ctx -> tell(String.format("发声方式：走=%s 跑=%s 潜=%s（可选 %s）",
                        YsmClient.CONFIG.walkMode, YsmClient.CONFIG.runMode, YsmClient.CONFIG.sneakMode, Mode.all())))
                .then(RequiredArgumentBuilder.<S, String>argument("state", StringArgumentType.word())
                        .then(RequiredArgumentBuilder.<S, String>argument("mode", StringArgumentType.word())
                                .executes(ctx -> {
                                    String st = StringArgumentType.getString(ctx, "state").toLowerCase();
                                    String md = StringArgumentType.getString(ctx, "mode").toLowerCase();
                                    if (!st.equals("walk") && !st.equals("run") && !st.equals("sneak")) {
                                        return tell("状态只认 walk / run / sneak");
                                    }
                                    if (!Mode.isValid(md)) {
                                        return tell("发声方式只认 " + Mode.all());
                                    }
                                    StepConfig c = YsmClient.CONFIG;
                                    if (st.equals("walk")) {
                                        c.walkMode = md;
                                    } else if (st.equals("run")) {
                                        c.runMode = md;
                                    } else {
                                        c.sneakMode = md;
                                    }
                                    c.save();
                                    return tell(String.format("走=%s 跑=%s 潜=%s",
                                            c.walkMode, c.runMode, c.sneakMode));
                                }))));

        root.then(LiteralArgumentBuilder.<S>literal("tap")
                .executes(ctx -> tell("触拍：" + modeText() + "；把触拍键绑上，跟着模型落地连点 4 下以上"))
                .then(RequiredArgumentBuilder.<S, String>argument("mode", StringArgumentType.word())
                        .executes(ctx -> {
                            String m = StringArgumentType.getString(ctx, "mode");
                            if (!m.equals("step") && !m.equals("cycle")) {
                                return tell("只认 step 或 cycle");
                            }
                            YsmClient.CONFIG.tapMode = m;
                            YsmClient.CONFIG.save();
                            tapCount = 0;
                            return tell("触拍：" + modeText());
                        })));

        d.register(root);
    }

    private interface Body {
        int run();
    }

    private static <S> void sub(LiteralArgumentBuilder<S> root, String name, Body body) {
        root.then(LiteralArgumentBuilder.<S>literal(name).executes(ctx -> body.run()));
    }

    private static <S> void knob(LiteralArgumentBuilder<S> root, String name,
                             double lo, double hi, DoubleSupplier get, DoubleConsumer set,
                             String label, java.util.function.Function<Double, String> fmt) {
        root.then(LiteralArgumentBuilder.<S>literal(name)
                .executes(ctx -> tell(label + " = " + fmt.apply(get.getAsDouble())))
                .then(RequiredArgumentBuilder.<S, String>argument("value", StringArgumentType.word())
                        .executes(ctx -> {
                            String raw = StringArgumentType.getString(ctx, "value");
                            double cur = get.getAsDouble();
                            Double v = parse(raw, cur, lo, hi);
                            if (v == null) {
                                return tell(label + " 看不懂：" + raw + "（例 1.2 / +0.05 / -0.05 / *2）");
                            }
                            set.accept(v);
                            YsmClient.CONFIG.save();
                            return tell(label + " → " + fmt.apply(v));
                        })));
    }

    /** 解析 1.2 / +0.05 / -0.05 / *2 / /2 / =−0.1 */
    static Double parse(String raw, double cur, double lo, double hi) {
        String s = raw.trim();
        try {
            if (s.startsWith("=")) {
                return StepConfig.clamp(Double.parseDouble(s.substring(1)), lo, hi);
            }
            if (s.startsWith("+")) {
                return StepConfig.clamp(cur + Double.parseDouble(s.substring(1)), lo, hi);
            }
            if (s.startsWith("-")) {
                return StepConfig.clamp(cur - Double.parseDouble(s.substring(1)), lo, hi);
            }
            if (s.startsWith("*")) {
                return StepConfig.clamp(cur * Double.parseDouble(s.substring(1)), lo, hi);
            }
            if (s.startsWith("/")) {
                return StepConfig.clamp(cur / Double.parseDouble(s.substring(1)), lo, hi);
            }
            return StepConfig.clamp(Double.parseDouble(s), lo, hi);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String modeText() {
        return "cycle".equals(YsmClient.CONFIG.tapMode) ? "一拍 = 一整个循环" : "一拍 = 一只脚落地";
    }

    /** 触拍一次，返回要显示的话。 */
    public static String tap() {
        long now = System.currentTimeMillis();
        if (tapCount > 0) {
            long dt = now - TAPS[(tapCount - 1) % TAPS.length];
            if (dt > 3000) {
                tapCount = 0;
            }
        }
        TAPS[tapCount % TAPS.length] = now;
        tapCount++;

        int n = Math.min(tapCount, TAPS.length);
        if (n < 4) {
            return "触拍 " + n + "/4…";
        }
        int m = n - 1;
        double[] diffs = new double[m];
        for (int i = 0; i < m; i++) {
            int a = (tapCount - n + i) % TAPS.length;
            int b = (tapCount - n + i + 1) % TAPS.length;
            diffs[i] = TAPS[b] - TAPS[a];
        }
        java.util.Arrays.sort(diffs);
        double median = diffs[m / 2] / 1000.0;
        if (median <= 0.05) {
            return "间隔太短，重新点";
        }
        double cycle = "cycle".equals(YsmClient.CONFIG.tapMode) ? median : median * 2.0;
        YsmClient.CONFIG.cycleSeconds = StepConfig.clamp(cycle, 0.2, 4.0);
        YsmClient.CONFIG.save();
        tapCount = 0;
        return String.format("采样 %d 拍 → 一拍 %.3fs → 周期 %.3fs 已保存",
                n, median, YsmClient.CONFIG.cycleSeconds);
    }

    /** /ysmstep share [模型名]：把当前校准上传到服务端（可顺手记下模型名）。 */
    private static int share(String model) {
        String name = model == null ? "" : model.trim();
        if (!name.isEmpty()) {
            YsmClient.CONFIG.modelName = name;
            YsmClient.CONFIG.save();
        }
        Profile p = Profile.of(YsmClient.CONFIG);
        try {
            Net.sendToServer(new Net.Upload(p));
        } catch (Throwable t) {
            return tell("上传失败：这个服务器可能没装 ysmstep（" + t.getClass().getSimpleName() + "）");
        }
        return tell("已上传：" + p.summary()
                + (p.modelName().isEmpty() ? "（没写模型名，只按你的 UUID 记）" : ""));
    }

    /** 把一份档案套到本地配置上。 */
    static void applyProfile(Profile p) {
        StepConfig c = YsmClient.CONFIG;
        c.cycleSeconds = p.cycle();
        c.offsetSeconds = p.offset();
        c.runFactor = p.runFactor();
        c.sneakFactor = p.sneakFactor();
        c.walkMode = p.walkMode();
        c.runMode = p.runMode();
        c.sneakMode = p.sneakMode();
        if (!p.modelName().isBlank()) {
            c.modelName = p.modelName();
        }
        c.save();
    }

    private static int status() {
        return tell(StepTimer.stateLine()
                + String.format(" 潜行x%.2f 音量x%.2f%s", YsmClient.CONFIG.sneakFactor, YsmClient.CONFIG.volume,
                YsmClient.CONFIG.debug ? " [debug]" : "")
                + " " + Bob.stateLine());
    }

    private static int tell(String text) {
        StepHud.message(text);
        return 1;
    }
}

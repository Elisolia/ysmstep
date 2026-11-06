package ysmstep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/ysmstep-client.json */
public class StepConfig {

    /** 总开关。关掉 = 原版脚步声原样回来。 */
    public boolean enabled = true;

    /** 走路动画一个循环的时长（秒）。一步 = 半个周期。 */
    public double cycleSeconds = 1.0;

    /** 相位偏移（秒）：脚步声比模型落地早/晚多少。 */
    public double offsetSeconds = 0.0;

    /** 跑步循环 = 走路循环 × 这个系数。 */
    public double runFactor = 0.667;

    /** 潜行循环 = 走路循环 × 这个系数。 */
    public double sneakFactor = 1.0;

    /** 0 = 完全固定节拍；1 = 节拍完全跟着移动速度缩放。 */
    public double speedFollow = 0.0;

    /** 脚步音量倍率（原版是 0.15）。 */
    public double volume = 1.0;

    /** 常驻显示 HUD。 */
    public boolean hud = false;

    /** 调试：HUD 显示实时状态，并把诊断写进 config/ysmstep-debug.txt。 */
    public boolean debug = false;

    /** 触拍时一拍代表什么：step = 一只脚落地，cycle = 一整个循环。 */
    public String tapMode = "step";

    /** 装了 Presence Footsteps 时，把发声交给它（我们只定节拍）。 */
    public boolean pfCompat = true;

    /**
     * 视角摇晃：
     *   vanilla = 原版（相位来自走过的距离）
     *   sync    = 跟着动画节拍（0.5.2 起）
     */
    public String bobMode = "vanilla";

    /** sync 模式下的摇晃幅度倍率（0 = 完全不晃）。 */
    public double bobScale = 1.0;

    /**
     * 各状态的发声方式：
     *   step = 踩地（默认）
     *   none = 不出声（整个人悬空的跑姿）
     *   wind = 鞘翅风声（悬浮/滑行）
     *   flap = 拍翅（飞行生物）
     *   soft = 软脚（轻足、灵体）
     */
    public String walkMode = "step";
    public String runMode = "step";
    public String sneakMode = "step";

    /** 你用的模型名（手填）。只用来给"同一模型的其他玩家"做默认值，不碰模型文件。 */
    public String modelName = "";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 步速基准：原版走路 ≈ 0.2158 格/tick（4.317 格/秒）。只在 speedFollow>0 时用得上。 */
    public static final double WALK_SPEED_REF = 0.2158;

    public Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("ysmstep-client.json");
    }

    public void load() {
        Path p = file();
        try {
            if (Files.exists(p)) {
                String s = Files.readString(p, StandardCharsets.UTF_8);
                StepConfig read = GSON.fromJson(s, StepConfig.class);
                if (read != null) {
                    enabled = read.enabled;
                    cycleSeconds = clamp(read.cycleSeconds, 0.2, 4.0);
                    offsetSeconds = clamp(read.offsetSeconds, -2.0, 2.0);
                    runFactor = clamp(read.runFactor, 0.2, 2.0);
                    sneakFactor = clamp(read.sneakFactor, 0.2, 2.0);
                    speedFollow = clamp(read.speedFollow, 0.0, 1.0);
                    volume = clamp(read.volume, 0.0, 4.0);
                    hud = read.hud;
                    debug = read.debug;
                    tapMode = "cycle".equals(read.tapMode) ? "cycle" : "step";
                    pfCompat = read.pfCompat;
                    bobMode = "sync".equals(read.bobMode) ? "sync" : "vanilla";
                    bobScale = clamp(read.bobScale, 0.0, 2.0);
                    walkMode = Mode.parse(read.walkMode).id();
                    runMode = Mode.parse(read.runMode).id();
                    sneakMode = Mode.parse(read.sneakMode).id();
                    modelName = read.modelName == null ? "" : read.modelName.trim();
                }
            } else {
                save();
            }
        } catch (Exception e) {
            YsmStep.LOG.warn("[ysmstep] 读配置失败，用默认值：{}", e.toString());
        }
    }

    public void save() {
        Path p = file();
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            YsmStep.LOG.warn("[ysmstep] 写配置失败：{}", e.toString());
        }
    }

    /** 一步的间隔（tick）。 */
    public double stepIntervalTicks() {
        return cycleSeconds * 20.0 / 2.0;
    }

    public String summary() {
        return String.format("cycle=%.2fs offset=%+.2fs run=x%.2f follow=%.0f%%",
                cycleSeconds, offsetSeconds, runFactor, speedFollow * 100);
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}

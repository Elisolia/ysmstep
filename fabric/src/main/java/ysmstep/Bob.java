package ysmstep;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * 视角摇晃跟随（0.5.2 起）。
 *
 * 原版 GameRenderer#bobView 的相位来自 walkDist（走过的距离），和脚步声是同一个钟。
 * 我们把脚步声搬到动画时钟之后，镜头还留在距离时钟上——两个节奏打架。
 *
 * 做法：不接管 bobView 的数学，只把它的"输入"换掉。
 *   · walkDist / walkDistO → 由动画节拍换算出来的"等效行走距离"
 *   · bob / oBob           → 原值 × bobScale（幅度倍率）
 *
 * 换算保持原版比例（Entity#move 第 690/692/996 行）：
 *   walkDist 每格 +0.6，而一只脚落地发生在 moveDist 每 +1.0 时
 *   ⇒ 一步 = walkDist +1.0，两次脚步 = 一个完整摇晃周期（sin 的周期是 2）
 * 于是摇晃的形状、幅度、和脚的对应关系都和原版一致，只是由动画节拍驱动。
 *
 * 只对本地玩家生效；其他人（第三人称看别人）仍是原版。
 */
public final class Bob {

    /** 一步对应的 walkDist 增量：原版 moveDist 每 +1.0 落一脚，walkDist 同步 +1.0。 */
    public static final double WALK_DIST_PER_STEP = 1.0;

    private static float now = 0.0F;
    private static float prev = 0.0F;
    private static boolean armed = false;

    private Bob() {
    }

    /** 每 tick 开头调一次：把上一 tick 的值挪进 prev。 */
    public static void beginTick() {
        prev = now;
    }

    /** StepTimer 算出"累计步数"后喂进来。 */
    public static void setSteps(double totalSteps) {
        now = (float) (totalSteps * WALK_DIST_PER_STEP);
        armed = true;
    }

    public static void reset() {
        now = 0.0F;
        prev = 0.0F;
        armed = false;
    }

    // ---- 给 mixin 用的四个取值 ----

    public static float walkDist(Player p) {
        return active(p) ? now : p.walkDist;
    }

    public static float walkDistO(Player p) {
        return active(p) ? prev : p.walkDistO;
    }

    public static float bob(Player p) {
        return active(p) ? p.bob * scale() : p.bob;
    }

    public static float oBob(Player p) {
        return active(p) ? p.oBob * scale() : p.oBob;
    }

    private static boolean active(Player p) {
        StepConfig c = YsmClient.CONFIG;
        if (!c.enabled || !"sync".equals(c.bobMode) || !armed) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && p == mc.player;
    }

    private static float scale() {
        // 当前姿势不是"踩地"（飞行模型、轮滑模型 → none/wind/soft/flap），脚下根本没有落地，
        // 镜头就别跟着节拍晃了，幅度归零。
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && StepTimer.modeFor(mc.player) != Mode.STEP) {
            return 0.0F;
        }
        return (float) StepConfig.clamp(YsmClient.CONFIG.bobScale, 0.0, 2.0);
    }

    /** 给命令 / HUD 用的一句话。 */
    public static String stateLine() {
        StepConfig c = YsmClient.CONFIG;
        if ("sync".equals(c.bobMode)) {
            return String.format("视角摇晃：跟动画节拍（幅度 x%.2f）", c.bobScale);
        }
        return "视角摇晃：原版（按距离）";
    }
}

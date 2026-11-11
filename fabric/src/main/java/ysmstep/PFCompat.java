package ysmstep;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.loader.api.FabricLoader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 与 Presence Footsteps (NeoForge) 的对接。
 *
 * PF 的节拍是它自己按「走过的距离」攒的：
 *   MotionTracker.distanceTraveled → simulateFootsteps() 里 dwm 超过 DISTANCE_HUMAN 就踩一步
 * 而且它每 tick 都会调 generateFootsteps()，一帧最多踩一步。
 *
 * 做法：平时把 simulateFootsteps 拦掉（它继续攒距离但不发声），
 * 到我们的节拍点放行一帧 —— 攒下的距离刚好跨过阈值，PF 用它自己的音色踩一步。
 * 材质关联、左右脚交替、音高音量、上下楼/潜行分档全归 PF 管，我们只管时机。
 *
 * 只拦「归我们管」的玩家：本地玩家，以及带着档案的其他玩家。生物和别人原样。
 */
public final class PFCompat {

    /** PF 的 modid。 */
    public static final String PF_MODID = "presencefootsteps";

    private static boolean present = false;
    /** 每个玩家还剩几帧放行。 */
    private static final Map<UUID, Integer> allowFrames = new HashMap<>();
    /** 闸门有没有真的被调用过（mixin 打上了没）。 */
    private static boolean gateSeen = false;

    private PFCompat() {
    }

    /** 启动时调一次。 */
    public static void detect() {
        present = FabricLoader.getInstance().isModLoaded(PF_MODID);
        if (present) {
            YsmStep.LOG.info("[ysmstep] 检测到 Presence Footsteps，脚步时机交给它");
        }
    }

    public static boolean present() {
        return present;
    }

    /** 现在是"我们定时机、PF 出声"吗。 */
    public static boolean active() {
        return present && YsmClient.CONFIG.pfCompat && YsmClient.CONFIG.enabled;
    }

    /** 本地玩家的节拍点。 */
    public static boolean handoff() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return handoff(mc.player.getUUID());
    }

    /** 某个玩家的节拍点（本地玩家或带档案的别人）。 */
    public static boolean handoff(UUID id) {
        if (!active()) {
            return false;
        }
        // 放行 2 帧：不怕监听器先后顺序，多放的那帧距离差≈0，PF 自己不会出声
        allowFrames.put(id, 2);
        return true;
    }

    /**
     * 闸门。由 mixin 在 simulateFootsteps 的 HEAD 调用。
     *
     * @return true = 拦掉这一帧
     */
    public static boolean gate(LivingEntity entity) {
        if (!active() || entity == null) {
            return false;
        }
        if (!tracked(entity)) {
            return false;
        }
        gateSeen = true;
        UUID id = entity.getUUID();
        Integer left = allowFrames.get(id);
        if (left != null && left > 0) {
            allowFrames.put(id, left - 1);
            return false;
        }
        return true;
    }

    /** 归我们管的玩家：本地玩家，或者带着档案的别人。 */
    private static boolean tracked(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == entity) {
            return true;
        }
        return entity instanceof Player player && RemoteSteps.has(player);
    }

    /** 兜底：别让放行标记堆积。 */
    public static void onClientTick() {
        Iterator<Map.Entry<UUID, Integer>> it = allowFrames.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            int v = e.getValue() - 1;
            if (v <= 0) {
                it.remove();
            } else {
                e.setValue(v);
            }
        }
    }

    /** 给状态行看的一句话：mixin 到底打上没有。 */
    public static String gateState() {
        if (!present) {
            return "";
        }
        if (!YsmClient.CONFIG.pfCompat) {
            return " [PF关]";
        }
        return gateSeen ? " [PF出声]" : " [PF闸门未触发]";
    }
}

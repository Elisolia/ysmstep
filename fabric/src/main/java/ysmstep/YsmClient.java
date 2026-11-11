package ysmstep;

import net.minecraft.client.Minecraft;

/**
 * 只有客户端才有的那部分状态。
 *
 * 拆出来的原因：模组现在要双端跑（服务端要存别人的脚步档案），
 * 而 YsmStep 主类在服务端也会被加载 —— 它不能碰 Minecraft / LocalPlayer 这些客户端类，
 * 否则服务端启动就 NoClassDefFoundError。
 */
public final class YsmClient {

    public static final StepConfig CONFIG = new StepConfig();
    public static final StepTimer TIMER = new StepTimer();

    private YsmClient() {
    }

    public static void init() {
        YsmClient.CONFIG.load();
        PFCompat.detect();
    }

    /** 只处理「自己」——别人的脚步由 {@link RemoteSteps} 按档案重算。 */
    public static boolean isLocalPlayer(Object entity) {
        return entity != null && Minecraft.getInstance().player == entity;
    }
}

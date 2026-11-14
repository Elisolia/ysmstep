package ysmstep;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * 游戏总线的监听。
 *
 * 用 @EventBusSubscriber / @SubscribeEvent 这个写法，是因为它是这个整合包里已经验证能跑的方式
 * （icaddon 全用它）。0.1.0 用的是 EVENT_BUS.addListener(方法引用)，症状是声音一声不响，
 * 所以先换成这条确定的路。
 */
@EventBusSubscriber(modid = YsmStep.MODID, value = Dist.CLIENT)
public final class YsmStepEvents {

    private YsmStepEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        YsmClient.TIMER.tick();
        RemoteSteps.tick();
        StepKeys.poll();
        PFCompat.onClientTick();

        // 面板里选了「跑」：玩家自己的 tick 会把 sprinting 清掉（没按前进键），
        // 这里在 tick 全部跑完之后再按住一次，YSM 下一帧就能看到"疾跑中"。
        if (PreviewScreen.runOverride) {
            Minecraft.getInstance().player.setSprinting(true);
        }
    }

    @SubscribeEvent
    public static void onClientCommands(RegisterClientCommandsEvent event) {
        StepCommands.register(event.getDispatcher());
        PreviewCommand.register(event.getDispatcher()); // 0.5.3 测试用，正式版删
    }
}

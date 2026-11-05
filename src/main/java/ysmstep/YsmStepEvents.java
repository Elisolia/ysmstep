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
    }

    @SubscribeEvent
    public static void onClientCommands(RegisterClientCommandsEvent event) {
        StepCommands.register(event.getDispatcher());
    }
}

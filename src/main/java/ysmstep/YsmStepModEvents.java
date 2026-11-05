package ysmstep;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** 模组总线的监听（注册期一次性事件）。 */
@EventBusSubscriber(modid = YsmStep.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class YsmStepModEvents {

    private YsmStepModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        StepKeys.register(event);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        StepHud.register(event);
    }
}

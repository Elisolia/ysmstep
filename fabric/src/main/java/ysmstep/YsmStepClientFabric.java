package ysmstep;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** 客户端入口：初始化配置、键位、HUD、网络接收与每 tick 的节拍。 */
public final class YsmStepClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        YsmClient.init();
        Net.registerClient();
        StepKeys.register();
        StepHud.register();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) {
                return;
            }
            YsmClient.TIMER.tick();
            RemoteSteps.tick();
            StepKeys.poll();
            PFCompat.onClientTick();
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                StepCommands.register(dispatcher));

        YsmStep.LOG.info("[ysmstep] 客户端就绪 0.5.2, {}, pf={}", YsmClient.CONFIG.summary(), PFCompat.present());
    }
}

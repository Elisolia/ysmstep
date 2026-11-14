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

            // 面板里选了「跑」：玩家自己的 tick 会把 sprinting 清掉（没按前进键），
            // 这里在 tick 全部跑完之后再按住一次，YSM 下一帧就能看到"疾跑中"。
            if (PreviewScreen.runOverride) {
                mc.player.setSprinting(true);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                StepCommands.register(dispatcher));

        YsmStep.LOG.info("[ysmstep] 客户端就绪 0.5.3, {}, pf={}", YsmClient.CONFIG.summary(), PFCompat.present());
    }
}

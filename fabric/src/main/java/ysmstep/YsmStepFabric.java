package ysmstep;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** 通用入口：注册网络通道与服务端逻辑（集成服务端也会走到这里）。 */
public final class YsmStepFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Net.registerCommon();
        ServerProfileHandler.register();

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            YsmStep.LOG.info("[ysmstep] Fabric 客户端加载中 0.5.2");
        } else {
            YsmStep.LOG.info("[ysmstep] 服务端就绪 0.5.2 —— 负责缓存与广播玩家脚步档案");
        }
    }
}

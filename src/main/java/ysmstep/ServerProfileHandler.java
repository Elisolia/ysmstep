package ysmstep;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

/**
 * 服务端这半边：收档案、存档、广播。
 *
 * 只在服务端跑（客户端上这些类也有，但不会有人调用）。
 */
@EventBusSubscriber(modid = YsmStep.MODID)
public final class ServerProfileHandler {

    private ServerProfileHandler() {
    }

    /** 玩家上传了自己的校准。 */
    public static void onUpload(ServerPlayer player, Profile incoming) {
        Profile profile = incoming.sanitized();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ProfileStore store = ProfileStore.get(server);

        // 先看模型名有没有现成档案（在覆盖之前比）
        Profile existing = store.model(profile.modelName());
        if (existing != null && !existing.equals(profile)) {
            PacketDistributor.sendToPlayer(player, new Net.TemplateHint(existing));
        }

        store.putPlayer(player.getUUID(), profile);
        broadcast(server);
        YsmStep.LOG.info("[ysmstep] 存档 {} 的档案：{}", player.getGameProfile().getName(), profile.summary());
    }

    /** 有人进来了，把现存的档案推给他。 */
    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MinecraftServer server = sp.getServer();
            if (server != null) {
                PacketDistributor.sendToPlayer(sp, new Net.Snapshot(ProfileStore.get(server).entries()));
            }
        }
    }

    private static void broadcast(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(new Net.Snapshot(ProfileStore.get(server).entries()));
    }

    /** 给服务端命令用的只读快照。 */
    public static Map<java.util.UUID, Profile> dump(MinecraftServer server) {
        return ProfileStore.get(server).players();
    }
}

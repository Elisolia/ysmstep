package ysmstep;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * 服务端这半边：收档案、存档、广播。（Fabric 版）
 *
 * 只在服务端跑（客户端上这些类也有，但不会有人调用）。
 */
public final class ServerProfileHandler {

    private ServerProfileHandler() {
    }

    /** 有人进来了，把现存的档案推给他。 */
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer sp = handler.player;
            if (sp != null) {
                ServerPlayNetworking.send(sp, new Net.Snapshot(ProfileStore.get(server).entries()));
            }
        });
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
            ServerPlayNetworking.send(player, new Net.TemplateHint(existing));
        }

        store.putPlayer(player.getUUID(), profile);
        broadcast(server);
        YsmStep.LOG.info("[ysmstep] 存档 {} 的档案：{}", player.getGameProfile().getName(), profile.summary());
    }

    private static void broadcast(MinecraftServer server) {
        Net.Snapshot snapshot = new Net.Snapshot(ProfileStore.get(server).entries());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, snapshot);
        }
    }

    /** 给服务端命令用的只读快照。 */
    public static Map<java.util.UUID, Profile> dump(MinecraftServer server) {
        return ProfileStore.get(server).players();
    }
}

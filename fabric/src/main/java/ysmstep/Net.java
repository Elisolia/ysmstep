package ysmstep;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 三个包：上传、快照、模型模板提示。（Fabric 版） */
public final class Net {

    private Net() {
    }

    /** 两端都要注册：C2S 由服务端收，S2C 由客户端收。 */
    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(Upload.TYPE, Upload.CODEC);
        PayloadTypeRegistry.playS2C().register(Snapshot.TYPE, Snapshot.CODEC);
        PayloadTypeRegistry.playS2C().register(TemplateHint.TYPE, TemplateHint.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Upload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> ServerProfileHandler.onUpload(player, payload.profile()));
        });
    }

    /** 客户端专用接收。 */
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE, (payload, context) ->
                context.client().execute(() -> RemoteSteps.onSnapshot(payload.entries())));

        ClientPlayNetworking.registerGlobalReceiver(TemplateHint.TYPE, (payload, context) ->
                context.client().execute(() -> RemoteSteps.onTemplate(payload.profile())));
    }

    /** 客户端把档案发给服务端。 */
    public static void sendToServer(Upload payload) {
        if (ClientPlayNetworking.canSend(Upload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
    }

    // ---------------- 包定义 ----------------

    /** 客户端 → 服务端：我的档案。UUID 从连接里取，不信客户端报的。 */
    public record Upload(Profile profile) implements CustomPacketPayload {
        public static final Type<Upload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(YsmStep.MODID, "upload"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Upload> CODEC =
                StreamCodec.composite(Profile.CODEC, Upload::profile, Upload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 服务端 → 客户端：全量快照。 */
    public record Snapshot(List<Map.Entry<UUID, Profile>> entries) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(YsmStep.MODID, "snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of(
                (buf, s) -> {
                    buf.writeVarInt(s.entries.size());
                    for (Map.Entry<UUID, Profile> e : s.entries) {
                        buf.writeUUID(e.getKey());
                        Profile.CODEC.encode(buf, e.getValue());
                    }
                },
                buf -> {
                    int n = buf.readVarInt();
                    List<Map.Entry<UUID, Profile>> list = new ArrayList<>(Math.max(0, n));
                    for (int i = 0; i < n; i++) {
                        UUID id = buf.readUUID();
                        list.add(Map.entry(id, Profile.CODEC.decode(buf)));
                    }
                    return new Snapshot(list);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 服务端 → 客户端：你的模型名在服务器里有现成档案，要不要套用。 */
    public record TemplateHint(Profile profile) implements CustomPacketPayload {
        public static final Type<TemplateHint> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(YsmStep.MODID, "template"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TemplateHint> CODEC =
                StreamCodec.composite(Profile.CODEC, TemplateHint::profile, TemplateHint::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

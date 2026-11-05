package ysmstep;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 三个包：上传、快照、模型模板提示。 */
@EventBusSubscriber(modid = YsmStep.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Net {

    private Net() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1").optional();
        r.playToServer(Upload.TYPE, Upload.CODEC, Net::handleUpload);
        r.playToClient(Snapshot.TYPE, Snapshot.CODEC, Net::handleSnapshot);
        r.playToClient(TemplateHint.TYPE, TemplateHint.CODEC, Net::handleTemplate);
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

    // ---------------- 处理 ----------------

    private static void handleUpload(Upload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                ServerProfileHandler.onUpload(sp, payload.profile());
            }
        });
    }

    private static void handleSnapshot(Snapshot payload, IPayloadContext ctx) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        ctx.enqueueWork(() -> RemoteSteps.onSnapshot(payload.entries()));
    }

    private static void handleTemplate(TemplateHint payload, IPayloadContext ctx) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        ctx.enqueueWork(() -> RemoteSteps.onTemplate(payload.profile()));
    }
}

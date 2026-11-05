package ysmstep;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 别人的脚步。
 *
 * 服务端把每个玩家上传的档案广播下来，这里就给那个玩家单独跑一条节拍器：
 * 压掉他的原版脚步（mixin 那边查 {@link #has}），按他的周期/相位/音色在本地播。
 *
 * 装了 Presence Footsteps 时同样走闸门 —— 只放行我们算好的那一帧。
 */
public final class RemoteSteps {

    /** 超过这个距离就不算了，反正也听不见。 */
    private static final double MAX_DIST_SQ = 64.0 * 64.0;

    private static final Map<UUID, Profile> PROFILES = new HashMap<>();
    private static final Map<UUID, Stepper> STEPPERS = new HashMap<>();

    private static Profile pendingTemplate = null;
    private static boolean uploadedThisSession = false;

    private RemoteSteps() {
    }

    // ---------------- 档案 ----------------

    public static boolean has(UUID id) {
        return PROFILES.containsKey(id);
    }

    public static boolean has(Player player) {
        return player != null && PROFILES.containsKey(player.getUUID());
    }

    public static Profile profileOf(UUID id) {
        return PROFILES.get(id);
    }

    public static void onSnapshot(List<Map.Entry<UUID, Profile>> entries) {
        PROFILES.clear();
        for (Map.Entry<UUID, Profile> e : entries) {
            PROFILES.put(e.getKey(), e.getValue());
        }
        STEPPERS.keySet().removeIf(id -> !PROFILES.containsKey(id));
        YsmStep.LOG.info("[ysmstep] 收到 {} 条档案", PROFILES.size());
    }

    public static void onTemplate(Profile template) {
        pendingTemplate = template;
        StepHud.message("服务器里这个模型有现成档案：" + template.summary() + "  —— /ysmstep apply 套用");
    }

    public static Profile takeTemplate() {
        Profile p = pendingTemplate;
        pendingTemplate = null;
        return p;
    }

    public static int size() {
        return PROFILES.size();
    }

    // ---------------- 每 tick ----------------

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer self = mc.player;
        ClientLevel level = mc.level;
        if (self == null || level == null) {
            uploadedThisSession = false;
            STEPPERS.clear();
            return;
        }
        if (!uploadedThisSession) {
            uploadedThisSession = true;
            upload();
        }
        if (!YsmClient.CONFIG.enabled || PROFILES.isEmpty()) {
            STEPPERS.clear();
            return;
        }

        List<UUID> seen = new ArrayList<>(PROFILES.size());
        for (AbstractClientPlayer other : level.players()) {
            if (other == self) {
                continue;
            }
            Profile profile = PROFILES.get(other.getUUID());
            if (profile == null) {
                continue;
            }
            if (other.distanceToSqr(self) > MAX_DIST_SQ) {
                continue;
            }
            seen.add(other.getUUID());
            STEPPERS.computeIfAbsent(other.getUUID(), id -> new Stepper()).tick(other, profile);
        }
        STEPPERS.keySet().removeIf(id -> !seen.contains(id));
    }

    /** 把我的档案发给服务端。服务端没装这个 mod 时静默放弃（包是 optional 的）。 */
    public static void upload() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        try {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new Net.Upload(Profile.of(YsmClient.CONFIG)));
        } catch (Throwable t) {
            YsmStep.LOG.debug("[ysmstep] 上传档案失败（服务端可能没装）：{}", t.toString());
        }
    }

    // ---------------- 单个玩家的节拍器 ----------------

    static final class Stepper {

        private double acc = 0.0;
        private boolean wasMoving = false;
        private int idleTicks = 0;

        void tick(Player player, Profile profile) {
            double hs = Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x
                    + player.getDeltaMovement().z * player.getDeltaMovement().z);

            boolean wants = hs > 0.02
                    && !player.isPassenger()
                    && !player.isInWater()
                    && !player.isSwimming()
                    && !player.isFallFlying()
                    && !player.isSpectator();

            if (!wants) {
                if (++idleTicks > 4) {
                    acc = profile.offset() * 20.0;
                    wasMoving = false;
                }
                return;
            }
            idleTicks = 0;
            if (!player.onGround()) {
                return;
            }
            if (!wasMoving) {
                acc = profile.offset() * 20.0;
                wasMoving = true;
            }

            double cycle = profile.cycle();
            if (player.isSprinting()) {
                cycle *= profile.runFactor();
            } else if (player.isShiftKeyDown()) {
                cycle *= profile.sneakFactor();
            }
            double interval = Math.max(1.0, cycle * 10.0);

            acc += 1.0;
            int guard = 0;
            while (acc >= interval && guard++ < 8) {
                acc -= interval;
                emit(player, profile);
            }
        }

        private void emit(Player player, Profile profile) {
            Mode mode;
            if (player.isSprinting()) {
                mode = Mode.parse(profile.runMode());
            } else if (player.isShiftKeyDown()) {
                mode = Mode.parse(profile.sneakMode());
            } else {
                mode = Mode.parse(profile.walkMode());
            }
            float vol = (float) YsmClient.CONFIG.volume;

            switch (mode) {
                case NONE -> {
                    return;
                }
                case WIND -> {
                    play(player, SoundEvents.ELYTRA_FLYING, 0.20F * vol, 0.9F);
                    return;
                }
                case FLAP -> {
                    play(player, SoundEvents.PHANTOM_FLAP, 0.25F * vol, 1.0F);
                    return;
                }
                case SOFT -> {
                    SoundType wool = SoundType.WOOL;
                    play(player, wool.getStepSound(), wool.getVolume() * 0.15F * vol, wool.getPitch());
                    return;
                }
                default -> {
                }
            }

            if (PFCompat.handoff(player.getUUID())) {
                return;
            }
            BlockPos pos = player.getOnPos();
            BlockState state = player.level().getBlockState(pos);
            SoundType type = state.getSoundType(player.level(), pos, player);
            SoundEvent sound = type.getStepSound();
            play(player, sound, type.getVolume() * 0.15F * vol, type.getPitch());
        }

        private void play(Player player, SoundEvent sound, float volume, float pitch) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.PLAYERS, volume, pitch);
        }
    }
}

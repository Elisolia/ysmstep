package ysmstep;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 一个玩家/一个模型的脚步档案 —— 上传到服务端的那几字节。
 *
 * cycle / offset / runFactor / sneakFactor 是节拍参数；
 * walkMode / runMode / sneakMode 是各姿态发声方式（step/none/wind/flap/soft）；
 * modelName 是玩家手填的模型名，用来给「同一模型的其他人」做默认值。留空也行。
 */
public record Profile(float cycle, float offset, float runFactor, float sneakFactor,
                      String walkMode, String runMode, String sneakMode,
                      String modelName) {

    public static final Profile DEFAULT = new Profile(1.0F, 0.0F, 0.667F, 1.0F, "step", "step", "step", "");

    public static Profile of(StepConfig c) {
        return new Profile(
                (float) c.cycleSeconds, (float) c.offsetSeconds,
                (float) c.runFactor, (float) c.sneakFactor,
                c.walkMode, c.runMode, c.sneakMode,
                c.modelName == null ? "" : c.modelName);
    }

    /** 校验：范围不对就别信。 */
    public Profile sanitized() {
        return new Profile(
                clamp(cycle, 0.2F, 4.0F),
                clamp(offset, -1.0F, 1.0F),
                clamp(runFactor, 0.3F, 1.5F),
                clamp(sneakFactor, 0.3F, 1.5F),
                Mode.parse(walkMode).id(),
                Mode.parse(runMode).id(),
                Mode.parse(sneakMode).id(),
                modelName == null ? "" : modelName.trim());
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public String summary() {
        return String.format("%.2fs 相位%+.2f 跑x%.2f 潜x%.2f 走=%s 跑=%s 潜=%s%s",
                cycle, offset, runFactor, sneakFactor,
                walkMode, runMode, sneakMode,
                modelName.isEmpty() ? "" : " 模型=" + modelName);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, Profile> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeFloat(p.cycle);
                buf.writeFloat(p.offset);
                buf.writeFloat(p.runFactor);
                buf.writeFloat(p.sneakFactor);
                writeUtf(buf, p.walkMode, 16);
                writeUtf(buf, p.runMode, 16);
                writeUtf(buf, p.sneakMode, 16);
                writeUtf(buf, p.modelName, 64);
            },
            buf -> new Profile(
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    readUtf(buf, 16), readUtf(buf, 16), readUtf(buf, 16), readUtf(buf, 64)));

    private static void writeUtf(ByteBuf buf, String s, int max) {
        String v = s == null ? "" : s;
        if (v.length() > max) {
            v = v.substring(0, max);
        }
        byte[] bytes = v.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length > 255) {
            bytes = java.util.Arrays.copyOf(bytes, 255);
        }
        buf.writeByte(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readUtf(ByteBuf buf, int max) {
        int len = buf.readUnsignedByte();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        String s = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        return s.length() > max ? s.substring(0, max) : s;
    }
}

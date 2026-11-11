package ysmstep;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 调试用：把状态写进 config/ysmstep-debug.txt。
 * 不依赖游戏日志（这包装了 buffering，运行中 latest.log 是空的，读不到）。
 */
public class StepDebug {

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("ysmstep-debug.txt");
    }

    public static void reset() {
        try {
            Files.writeString(file(), "", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static void log(String line) {
        try {
            Files.writeString(file(), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }
}

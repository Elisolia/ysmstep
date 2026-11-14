package ysmstep;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * 开发期临时命令：/ysmpreview 打开预览面板。
 *
 * 单独一个根命令、不并进 /ysmstep，是为了以后删掉它时不碰 StepCommands
 * （那个文件两个加载器共用）。正式版里这个类会被删掉，换成按键。
 */
public final class PreviewCommand {

    private PreviewCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ysmpreview").executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.setScreen(new PreviewScreen()));
            return 1;
        }));
    }
}

package ysmstep;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** 屏幕左上角的状态行；调参数时自动闪一下。 */
public class StepHud {

    private static int flashTicks = 0;
    private static String lastMessage = null;
    private static int messageTicks = 0;

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(YsmStep.MODID, "hud"),
                (LayeredDraw.Layer) StepHud::render
        );
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (flashTicks > 0) {
            flashTicks--;
        }
        if (messageTicks > 0) {
            messageTicks--;
        }

        StepConfig cfg = YsmClient.CONFIG;
        if (!cfg.hud && !cfg.debug && flashTicks <= 0 && messageTicks <= 0) {
            return;
        }

        int y = 4;
        if (cfg.hud || cfg.debug || flashTicks > 0) {
            graphics.drawString(mc.font, StepTimer.stateLine(), 4, y, 0xFFE0E0E0, true);
            y += 11;
        }
        if (cfg.hud || cfg.debug) {
            graphics.drawString(mc.font, YsmClient.TIMER.liveLine(), 4, y, 0xFF80C0FF, true);
            y += 11;
            if (cfg.debug && !YsmClient.TIMER.dbgLast.isEmpty()) {
                graphics.drawString(mc.font, YsmClient.TIMER.dbgLast, 4, y, 0xFFA0FFA0, true);
            }
        }
        if (messageTicks > 0 && lastMessage != null) {
            graphics.drawString(mc.font, lastMessage, 4, y + 12, 0xFFFFE070, true);
        }
    }

    public static void flashTuning() {
        flashTicks = 60;
    }

    /** 屏幕上显示一条短消息（也顺带打到聊天栏）。 */
    public static void message(String text) {
        lastMessage = text;
        messageTicks = 100;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[YSM脚步] " + text), true);
        }
    }
}

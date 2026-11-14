package ysmstep;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * 键位默认全都不绑（288 个模组的整合包里绑了就会撞）。
 * 用之前去「选项 → 控制」里给「YSM 脚步同步」那几项设键。
 *
 * 不想设键也行：全部功能都有 /ysmstep 命令。
 */
public class StepKeys {

    public static final String CATEGORY = "key.categories.ysmstep";

    public static final KeyMapping CYCLE_UP = new KeyMapping("key.ysmstep.cycle_up", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping CYCLE_DOWN = new KeyMapping("key.ysmstep.cycle_down", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping OFFSET_LATER = new KeyMapping("key.ysmstep.offset_later", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping OFFSET_EARLIER = new KeyMapping("key.ysmstep.offset_earlier", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping TAP = new KeyMapping("key.ysmstep.tap", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping HUD_TOGGLE = new KeyMapping("key.ysmstep.hud", InputConstants.UNKNOWN.getValue(), CATEGORY);

    /**
     * 0.5.3：打开设置面板，默认 Ctrl + Y。
     * Fabric 没有 NeoForge 的 KeyModifier（那个能把修饰键存进绑定里），
     * 所以这里绑 Y、Ctrl 在 poll 里自己判 —— 代价是控制界面里只显示 Y。
     */
    public static final KeyMapping PANEL = new KeyMapping("key.ysmstep.panel", GLFW.GLFW_KEY_Y, CATEGORY);

    public static void register() {
        KeyBindingHelper.registerKeyBinding(PANEL);
        KeyBindingHelper.registerKeyBinding(CYCLE_UP);
        KeyBindingHelper.registerKeyBinding(CYCLE_DOWN);
        KeyBindingHelper.registerKeyBinding(OFFSET_LATER);
        KeyBindingHelper.registerKeyBinding(OFFSET_EARLIER);
        KeyBindingHelper.registerKeyBinding(TAP);
        KeyBindingHelper.registerKeyBinding(HUD_TOGGLE);
    }

    /** 客户端 tick 里收键（F1 隐藏 HUD 时也能调）。 */
    static void poll() {
        StepConfig cfg = YsmClient.CONFIG;

        int up = consume(CYCLE_UP);
        int down = consume(CYCLE_DOWN);
        int later = consume(OFFSET_LATER);
        int earlier = consume(OFFSET_EARLIER);
        int taps = consume(TAP);
        int hud = consume(HUD_TOGGLE);
        int panel = consume(PANEL);

        if (panel != 0 && Screen.hasControlDown() && Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(new PreviewScreen());
        }

        if (up != 0 || down != 0) {
            cfg.cycleSeconds = StepConfig.clamp(cfg.cycleSeconds + 0.02 * (up - down), 0.2, 4.0);
            changed();
        }
        if (later != 0 || earlier != 0) {
            cfg.offsetSeconds = StepConfig.clamp(cfg.offsetSeconds + 0.01 * (later - earlier), -1.0, 1.0);
            changed();
        }
        if (hud != 0) {
            cfg.hud = !cfg.hud;
            cfg.save();
            StepHud.message(cfg.hud ? "HUD 开" : "HUD 关");
        }
        for (int i = 0; i < taps; i++) {
            String msg = StepCommands.tap();
            if (msg != null) {
                StepHud.message(msg);
            }
        }
    }

    private static int consume(KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return 0;
        }
        int n = 0;
        while (mapping.consumeClick()) {
            n++;
            if (n > 20) {
                break;
            }
        }
        return n;
    }

    private static void changed() {
        YsmClient.CONFIG.save();
        // 只有 HUD 开着才提示，免得调的时候刷屏
        if (YsmClient.CONFIG.hud) {
            StepHud.flashTuning();
        }
    }
}

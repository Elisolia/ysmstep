package ysmstep;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 0.5.3 主面板（深色卡片风，全部用 GuiGraphics 现画，不依赖任何贴图）。
 *
 * 布局：
 *   ┌ 标题栏 ─────────────────────────────────────────┐
 *   │ YSM 脚步同步            周期 0.94s · 相位 +0.00s  ✕ │
 *   ├ 预览卡片（3/5 高）──────────┬ 发声 ──────────────┤
 *   │   [模型]                    │ 走路 ▾ 跑步 ▾ 潜行 ▾ │
 *   │   [停][走][跑]              │ 时序 ──────────────┤
 *   ├ 周期与相位卡片 ─────────────┤ 周期 ▬▬▬▬ 1.20s     │
 *   │ 0 ─── 半步 ─── 一步         │ [触拍校准 0/4]       │
 *   │ ▓▓▓▓▓▓░░░░░░░░░░            │ 相位 ▬▬▬▬ +0.000s   │
 *   └─────────────────────────────┴ 其他 / 关闭 ────────┘
 *
 * 两个踩过的坑写在下面代码里，别再动：
 *  1. renderBackground 不能调 super（原版会给整屏加模糊+灰罩，把模型糊掉）。
 *  2. 画模型前后开关深度测试、还原 deltaMovement；冲刺标记要在 ClientTickEvent.Post 重新按住。
 */
public class PreviewScreen extends Screen {

    // ── 配色（随主题切换，所以不是 final） ──
    private static int BG = 0xFF121218;
    private static int CARD = 0xFF1C1C24;
    private static int CARD_HI = 0xFF23232D;
    private static int LINE = 0xFF2E2E3A;
    private static int TEXT = 0xFFE8E8F0;
    private static int DIM = 0xFF8A8A9A;
    private static int ACCENT = 0xFFFFC24D;
    private static int ACCENT_DIM = 0xFF6B5420;
    private static int OK = 0xFF7FD18A;
    /** 相位条的槽、已走部分、刻度小字、弹层投影。 */
    private static int TRACK = 0xFF15151B;
    private static int TRACK_FILL = 0xFF3A3F63;
    private static int FAINT = 0xFF5A5A66;
    private static int SHADOW = 0xFF000000;

    /** 控件自身的底色/滑轨/圆点/说明字（也随主题）。 */
    private static int BTN_BG = 0xFF1A1A22;
    private static int SLIDER_TRACK = 0xFF2A2A34;
    private static int SLIDER_FILL = 0xFF6E6E88;
    private static int KNOB = 0xFFD0D0DC;
    private static int HINT = 0xFF9AA88F;
    private static int MENU_DESC = 0xFF70707E;
    private static int MENU_DESC_HI = 0xFFB8B8C4;
    private static int OFF = 0xFFB06A6A;

    /** 按主题切换整套配色。dark = 原来的深色，light = 昼间。 */
    private static void applyTheme(boolean light) {
        if (light) {
            BG = 0xFFE9E9EE;
            CARD = 0xFFF8F8FB;
            CARD_HI = 0xFFFFFFFF;
            LINE = 0xFFCFCFD8;
            TEXT = 0xFF1B1B22;
            DIM = 0xFF5E5E6A;
            ACCENT = 0xFFC97A12;
            ACCENT_DIM = 0xFFE9C48C;
            OK = 0xFF2E7D4F;
            TRACK = 0xFFDCDCE4;
            TRACK_FILL = 0xFF9FA6C8;
            FAINT = 0xFF8C8C98;
            SHADOW = 0x40000000;
            BTN_BG = 0xFFEFEFF4;
            SLIDER_TRACK = 0xFFD8D8E0;
            SLIDER_FILL = 0xFF9AA0C0;
            KNOB = 0xFF6A6A78;
            HINT = 0xFF3F6B4A;
            MENU_DESC = 0xFF6E6E7A;
            MENU_DESC_HI = 0xFF2A2A32;
            OFF = 0xFF9C4B4B;
        } else {
            BG = 0xFF121218;
            CARD = 0xFF1C1C24;
            CARD_HI = 0xFF23232D;
            LINE = 0xFF2E2E3A;
            TEXT = 0xFFE8E8F0;
            DIM = 0xFF8A8A9A;
            ACCENT = 0xFFFFC24D;
            ACCENT_DIM = 0xFF6B5420;
            OK = 0xFF7FD18A;
            TRACK = 0xFF15151B;
            TRACK_FILL = 0xFF3A3F63;
            FAINT = 0xFF5A5A66;
            SHADOW = 0xFF000000;
            BTN_BG = 0xFF1A1A22;
            SLIDER_TRACK = 0xFF2A2A34;
            SLIDER_FILL = 0xFF6E6E88;
            KNOB = 0xFFD0D0DC;
            HINT = 0xFF9AA88F;
            MENU_DESC = 0xFF70707E;
            MENU_DESC_HI = 0xFFB8B8C4;
        }
    }

    /** 「还原」图标（32×32 白色贴图，用 shader 着色）。 */
    private static final ResourceLocation RESET_ICON =
            ResourceLocation.fromNamespaceAndPath(YsmStep.MODID, "textures/gui/reset.png");

    private static final int MARGIN = 16;
    private static final int ROW_H = 18;
    private static final int ROW_GAP = 5;

    /** 预览里让她走起来用的假速度（格/tick）。0 = 停，跑用冲刺档。 */
    private static final double[] FAKE_SPEED = {0.0, 0.13, 0.45};
    private static final String[] PLAY_LABEL = {"停", "走", "跑"};

    private float yaw = 165f;
    private float pitch = -6f;
    private int playMode = 0;

    private float panX = 0f;
    private float panY = 0f;
    private float zoom = 1f;

    private boolean savedSprint = false;
    public static boolean runOverride = false;

    private double localAcc = 0;
    private int tapPauseTicks = 0;
    private String hint = "";

    private FlatButton tapBtn;
    private FlatSlider cycleSlider;
    private FlatSlider offsetSlider;
    private FlatSlider volumeSlider;
    private FlatButton enabledBtn;
    private FlatButton hudBtn;
    private FlatButton bobBtn;

    /** 三个分组小标题的 y（在 init 里跟控件一起算，别再写死）。 */
    private int secVoiceY, secTimingY, secOtherY;

    /** 发声三个按钮（弹层要锚在它们下面）。 */
    private FlatButton modeWalkBtn, modeRunBtn, modeSneakBtn;

    /** 当前展开的发声菜单：null / "walk" / "run" / "sneak"。 */
    private String openMenu = null;

    /** 发声方式的名字与解释，按 Mode.values() 的顺序。 */
    private static final String[] MODE_DESC = {
            "踩地 · 按脚下方块的材质踩一步（默认）",
            "不出声 · 悬空、漂移的跑姿，硬踩地反而假",
            "风声 · 鞘翅滑翔、气流擦过的声音",
            "拍翅 · 飞行生物的翅膀拍打",
            "软脚 · 羊毛那类闷软的落地（轻足、灵体）"
    };

    public PreviewScreen() {
        super(Component.literal("YSM 脚步同步"));
    }

    // ───────────── 几何 ─────────────

    private int headerH() {
        return 24;
    }

    private int contentTop() {
        return MARGIN + headerH() + 10;
    }

    private int contentBottom() {
        return this.height - MARGIN;
    }

    private int leftW() {
        return (int) ((this.width - MARGIN * 3) * 0.46);
    }

    private int previewH() {
        return (int) ((contentBottom() - contentTop()) * 0.60);
    }

    private int rightX() {
        return MARGIN + leftW() + MARGIN;
    }

    private int rightW() {
        return this.width - rightX() - MARGIN;
    }

    // ───────────── 构建 ─────────────

    @Override
    protected void init() {
        applyTheme(isLight());
        StepTimer.muted = true;
        panX = 0;
        panY = 0;
        zoom = 1f;
        Player p = Minecraft.getInstance().player;
        savedSprint = p != null && p.isSprinting();

        int lx = MARGIN;
        int lw = leftW();
        int py = contentTop() + previewH();
        int segY = py - ROW_H - 10;   // 预览卡片内部的按钮条
        int segW = (lw - 20 - ROW_GAP * 2) / 3;
        for (int i = 0; i < 3; i++) {
            final int mode = i;
            addRenderableWidget(new FlatButton(lx + 10 + i * (segW + ROW_GAP), segY, segW, ROW_H,
                    () -> Component.literal(PLAY_LABEL[mode]),
                    () -> playMode == mode, b -> setPlay(mode)));
        }

        int rx = rightX();
        int rw = rightW();
        secVoiceY = contentTop() + 3;
        int y = secVoiceY + 13;   // 让开「发声」小标题

        // ── 发声 ──
        String[][] voiceRows = {{"走路", "walk"}, {"跑步", "run"}, {"潜行", "sneak"}};
        for (String[] row : voiceRows) {
            FlatButton b = modeButton(rx, y, rw, row[0], row[1]);
            addRenderableWidget(b);
            if ("walk".equals(row[1])) {
                modeWalkBtn = b;
            } else if ("run".equals(row[1])) {
                modeRunBtn = b;
            } else {
                modeSneakBtn = b;
            }
            y += ROW_H + ROW_GAP;
        }

        // ── 时序 ──
        secTimingY = y + 4;
        y = secTimingY + 13;
        cycleSlider = new FlatSlider(rx, y, rw - 22, ROW_H, "周期",
                () -> YsmClient.CONFIG.cycleSeconds, v -> YsmClient.CONFIG.cycleSeconds = v,
                0.2, 4.0, v -> String.format("%.2fs", v));
        addRenderableWidget(cycleSlider);
        addRenderableWidget(new ResetButton(rx + rw - 20, y, 20, ROW_H, "周期回到默认 1.00s", () -> {
            YsmClient.CONFIG.cycleSeconds = 1.0;
            YsmClient.CONFIG.save();
            syncSliders();
        }));
        y += ROW_H + ROW_GAP;

        tapBtn = new FlatButton(rx, y, rw, ROW_H,
                () -> Component.literal("触拍校准　" + Math.min(StepCommands.tapProgress(), 4) + " / 4"),
                () -> StepCommands.tapProgress() > 0, b -> onTap());
        tapBtn.setTooltip(Tooltip.create(Component.literal(
                "跟着模型脚步落地连点 4 下以上，自动算出动画周期。\n"
                        + "面板打开时节拍器静音，你按一下响一声，方便对拍子。")));
        addRenderableWidget(tapBtn);
        y += ROW_H + ROW_GAP;

        offsetSlider = new FlatSlider(rx, y, rw - 22, ROW_H, "相位",
                () -> YsmClient.CONFIG.offsetSeconds, v -> YsmClient.CONFIG.offsetSeconds = v,
                -0.5, 0.5, v -> String.format("%+.3fs", v), true);
        addRenderableWidget(offsetSlider);
        addRenderableWidget(new ResetButton(rx + rw - 20, y, 20, ROW_H, "相位回到 0", () -> {
            YsmClient.CONFIG.offsetSeconds = 0.0;
            YsmClient.CONFIG.save();
            syncSliders();
        }));
        y += ROW_H + ROW_GAP;

        volumeSlider = new FlatSlider(rx, y, rw - 22, ROW_H, "音量",
                () -> YsmClient.CONFIG.volume, v -> YsmClient.CONFIG.volume = v,
                0.0, 4.0, v -> String.format("×%.2f", v));
        addRenderableWidget(volumeSlider);
        addRenderableWidget(new ResetButton(rx + rw - 20, y, 20, ROW_H, "音量回到 1.00", () -> {
            YsmClient.CONFIG.volume = 1.0;
            YsmClient.CONFIG.save();
            syncSliders();
        }));

        // ── 其他 ──
        secOtherY = y + ROW_H + 8;
        y = secOtherY + 13;
        int half = (rw - ROW_GAP) / 2;
        enabledBtn = new FlatButton(rx, y, half, ROW_H,
                () -> Component.literal(YsmClient.CONFIG.enabled ? "总开关 开" : "总开关 关"),
                () -> YsmClient.CONFIG.enabled, b -> {
            YsmClient.CONFIG.enabled = !YsmClient.CONFIG.enabled;
            YsmClient.CONFIG.save();
        });
        addRenderableWidget(enabledBtn);

        hudBtn = new FlatButton(rx + half + ROW_GAP, y, half, ROW_H,
                () -> Component.literal(YsmClient.CONFIG.hud ? "HUD 开" : "HUD 关"),
                () -> YsmClient.CONFIG.hud, b -> {
            YsmClient.CONFIG.hud = !YsmClient.CONFIG.hud;
            YsmClient.CONFIG.save();
        });
        addRenderableWidget(hudBtn);
        y += ROW_H + ROW_GAP;

        bobBtn = new FlatButton(rx, y, rw, ROW_H, this::bobLabel, this::bobOn, b -> {
            YsmClient.CONFIG.bobMode = "sync".equals(YsmClient.CONFIG.bobMode) ? "vanilla" : "sync";
            YsmClient.CONFIG.save();
        });
        bobBtn.setTooltip(Tooltip.create(Component.literal(
                "镜头摇晃的相位要不要跟着动画节拍走。\n"
                        + "当前姿势的发声方式不是 step（飞行、轮滑）时，摇晃自动归零。")));
        addRenderableWidget(bobBtn);
        y += ROW_H + ROW_GAP;

        int third = (rw - ROW_GAP * 2) / 3;
        addRenderableWidget(new FlatButton(rx, y, third, ROW_H, () -> Component.literal("上传"),
                () -> false, b -> run("ysmstep share")));
        addRenderableWidget(new FlatButton(rx + third + ROW_GAP, y, third, ROW_H,
                () -> Component.literal("套用"), () -> false, b -> run("ysmstep apply")));
        addRenderableWidget(new FlatButton(rx + (third + ROW_GAP) * 2, y, third, ROW_H,
                () -> Component.literal("关闭"), () -> false, b -> onClose()));

        // 右下角：主题切换（夜间 ⇄ 昼间）
        addRenderableWidget(new FlatButton(this.width - MARGIN - 84, this.height - MARGIN - 16, 84, 16,
                () -> Component.literal(isLight() ? "主题：昼间" : "主题：夜间"), this::isLight, b -> {
            YsmClient.CONFIG.theme = isLight() ? "dark" : "light";
            YsmClient.CONFIG.save();
            applyTheme(isLight());
        }));
    }

    private boolean isLight() {
        return "light".equals(YsmClient.CONFIG.theme);
    }

    private Component bobLabel() {
        StepConfig c = YsmClient.CONFIG;
        if (!"sync".equals(c.bobMode)) {
            return Component.literal("视角摇晃　原版");
        }
        net.minecraft.client.player.LocalPlayer p = Minecraft.getInstance().player;
        boolean noFoot = p != null && StepTimer.modeFor(p) != Mode.STEP;
        return Component.literal(noFoot ? "视角摇晃　跟节拍（当前姿势非踩地，已暂停）" : "视角摇晃　跟节拍");
    }

    private boolean bobOn() {
        return "sync".equals(YsmClient.CONFIG.bobMode);
    }

    private FlatButton modeButton(int x, int y, int w, String label, String key) {
        java.util.function.Supplier<String> get = modeGetter(key);
        return new FlatButton(x, y, w, ROW_H,
                () -> Component.literal(label + "　" + get.get() + "  ▾"),
                () -> !"step".equals(get.get()),
                b -> openMenu = key);   // 点开 = 弹出全部选项，不再转圈切换
    }

    private java.util.function.Supplier<String> modeGetter(String key) {
        return switch (key) {
            case "run" -> () -> YsmClient.CONFIG.runMode;
            case "sneak" -> () -> YsmClient.CONFIG.sneakMode;
            default -> () -> YsmClient.CONFIG.walkMode;
        };
    }

    private void setMode(String key, Mode m) {
        switch (key) {
            case "run" -> YsmClient.CONFIG.runMode = m.id();
            case "sneak" -> YsmClient.CONFIG.sneakMode = m.id();
            default -> YsmClient.CONFIG.walkMode = m.id();
        }
        YsmClient.CONFIG.save();
        openMenu = null;
    }

    /** 弹出菜单锚在哪个按钮下面。 */
    private FlatButton menuAnchor() {
        if (openMenu == null) {
            return null;
        }
        return switch (openMenu) {
            case "run" -> modeRunBtn;
            case "sneak" -> modeSneakBtn;
            default -> modeWalkBtn;
        };
    }

    private int menuW() {
        return Math.min(rightW() - 40, 320);
    }

    private int menuRowH() {
        return 18;
    }

    private int menuH() {
        return MODE_DESC.length * menuRowH() + 8;
    }

    /** 菜单左上角：贴在按钮下方，靠近底部时翻到上方。 */
    private int menuX() {
        FlatButton a = menuAnchor();
        return a == null ? 0 : a.getX();
    }

    private int menuY() {
        FlatButton a = menuAnchor();
        if (a == null) {
            return 0;
        }
        int below = a.getY() + a.getHeight() + 2;
        return below + menuH() > this.height - 8 ? a.getY() - menuH() - 2 : below;
    }

    // ───────────── 动作 ─────────────

    private void onTap() {
        // 装了 Presence Footsteps 且开了对接 → 用它自己的音色踩一下；
        // 否则退回我们自己的原版方块声。
        Minecraft mc = Minecraft.getInstance();
        boolean viaPf = PFCompat.requestOneStep(mc.player);
        String msg = viaPf ? "试听（Presence Footsteps 音色）" : StepTimer.testStep();
        String tap = StepCommands.tap();
        if (tap != null) {
            msg = tap;
        }
        hint = msg;
        StepHud.message(msg);
        tapPauseTicks = 30;
        YsmClient.TIMER.stepFlash = 4;
        syncSliders();   // 触拍攒够 4 下会改周期，滑条要跟着挪
    }

    /** 配置被别处改了（触拍、命令、还原按钮）之后，让三个滑条回到正确位置。 */
    private void syncSliders() {
        if (cycleSlider != null) {
            cycleSlider.syncFromConfig();
        }
        if (offsetSlider != null) {
            offsetSlider.syncFromConfig();
        }
        if (volumeSlider != null) {
            volumeSlider.syncFromConfig();
        }
    }

    private void setPlay(int mode) {
        playMode = mode;
        localAcc = 0;
        runOverride = (mode == 2);
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            p.setSprinting(mode == 2);
        }
    }

    private void run(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand(command);
            hint = "已执行 /" + command;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (tapPauseTicks > 0) {
            tapPauseTicks--;
            return;
        }
        if (playMode == 0) {
            return;
        }
        double interval = YsmClient.CONFIG.stepIntervalTicks();
        localAcc += 1.0;
        if (localAcc >= interval) {
            localAcc -= interval;
            StepTimer.testStep();
            YsmClient.TIMER.stepFlash = 4;
        }
    }

    @Override
    public void onClose() {
        StepTimer.muted = false;
        runOverride = false;
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            p.setSprinting(savedSprint);
        }
        super.onClose();
    }

    // ───────────── 绘制 ─────────────

    /** 不要 super：原版会给整屏加模糊 + 灰罩（Esc 那样），把模型糊掉。 */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, BG);
        Font font = this.font;

        // ── 标题栏 ──
        drawHeader(g, font);

        // ── 预览卡片 ──
        int lx = MARGIN;
        int lw = leftW();
        int top = contentTop();
        int ph = previewH();
        card(g, lx, top, lw, ph);
        g.fill(lx + 1, top + 1, lx + 4, top + ph - 1, ACCENT_DIM);   // 左侧强调条
        g.drawString(font, "预览", lx + 12, top + 8, DIM, false);
        String modeText = playMode == 0 ? "静止" : (playMode == 2 ? "奔跑中" : "行走中");
        g.drawString(font, modeText, lx + lw - 12 - font.width(modeText), top + 8,
                playMode == 0 ? DIM : ACCENT, false);

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int clipBottom = top + ph - ROW_H - 18;
            g.enableScissor(lx + 1, top + 20, lx + lw - 1, clipBottom);
            float scale = Math.min(lw, ph) / 3.4f * zoom;
            drawPlayer(g, player, lx + lw / 2f + panX, clipBottom + panY, scale, partialTick);
            g.disableScissor();
        }

        // ── 相位卡片 ──
        int py = top + ph + MARGIN;
        int phH = contentBottom() - py;
        card(g, lx, py, lw, phH);
        drawPhase(g, font, lx, py, lw, phH);

        super.render(g, mouseX, mouseY, partialTick);

        // ── 右列分组标题（画在控件背后不遮挡的位置） ──
        int rx = rightX();
        int rw = rightW();
        sectionTitle(g, font, "发声", rx, secVoiceY, rw, "");
        sectionTitle(g, font, "时序", rx, secTimingY, rw,
                "滚轮调值 · Shift ×10 · Ctrl ×0.1 · 相位 Alt ×0.01");
        sectionTitle(g, font, "其他", rx, secOtherY, rw, "");

        String foot = "拖拽预览框旋转视角 · 左键拖动平移 · 滚轮缩放 · Esc 关闭";
        g.drawString(font, foot, MARGIN, this.height - 12, FAINT, false);

        drawModeMenu(g, font, mouseX, mouseY);
    }

    /** 发声菜单：贴着按钮弹出，每项一行，带解释。画在最上层。 */
    private void drawModeMenu(GuiGraphics g, Font font, int mouseX, int mouseY) {
        FlatButton anchor = menuAnchor();
        if (anchor == null) {
            return;
        }
        int mx = menuX();
        int my = menuY();
        int mw = menuW();
        int rh = menuRowH();

        g.fill(mx - 2, my - 2, mx + mw + 2, my + menuH() + 2, SHADOW);
        card(g, mx, my, mw, menuH());
        g.fill(mx, my, mx + mw, my + 1, ACCENT);

        String current = modeGetter(openMenu).get();
        Mode[] all = Mode.values();
        for (int i = 0; i < all.length; i++) {
            int ry = my + 4 + i * rh;
            boolean hover = mouseX >= mx && mouseX <= mx + mw && mouseY >= ry && mouseY < ry + rh;
            boolean sel = all[i].id().equals(current);
            if (hover) {
                g.fill(mx + 1, ry, mx + mw - 1, ry + rh, CARD_HI);
            }
            if (sel) {
                g.fill(mx + 1, ry, mx + 3, ry + rh, ACCENT);
            }
            g.drawString(font, all[i].id(), mx + 9, ry + 2, sel ? ACCENT : (hover ? TEXT : DIM), false);
            int nameEnd = mx + 9 + font.width(all[i].id()) + 8;
            g.drawString(font, MODE_DESC[i], nameEnd + font.width("  "), ry + 2,
                    hover ? MENU_DESC_HI : MENU_DESC, false);
        }
    }

    private void drawHeader(GuiGraphics g, Font font) {
        int y = MARGIN;
        int h = headerH();
        card(g, MARGIN, y, this.width - MARGIN * 2, h);

        g.drawString(font, "YSM 脚步同步", MARGIN + 12, y + 8, TEXT, false);
        int x = MARGIN + 12 + font.width("YSM 脚步同步") + 16;

        StepConfig c = YsmClient.CONFIG;
        String s1 = String.format("周期 %.2fs", c.cycleSeconds);
        String s2 = String.format("· 一步 %.2fs", c.cycleSeconds / 2);
        String s3 = String.format("· 相位 %+.3fs", c.offsetSeconds);
        g.drawString(font, s1, x, y + 8, DIM, false);
        x += font.width(s1) + 6;
        g.drawString(font, s2, x, y + 8, DIM, false);
        x += font.width(s2) + 6;
        g.drawString(font, s3, x, y + 8, DIM, false);

        String state = c.enabled ? "运行中" : "已关闭";
        int w = font.width(state);
        g.drawString(font, state, this.width - MARGIN - 12 - w, y + 8,
                c.enabled ? OK : OFF, false);
    }

    private void drawPhase(GuiGraphics g, Font font, int x, int y, int w, int h) {
        g.drawString(font, "周期与相位", x + 12, y + 8, DIM, false);

        StepConfig c = YsmClient.CONFIG;
        // 面板开着、且选了走/跑时，看的是面板自己那套试听节拍器；
        // 否则看游戏内的节拍器（站着不动时它本来就停在 0%）。
        double interval = Math.max(1.0, c.stepIntervalTicks());
        double within;
        String state;
        if (playMode != 0) {
            within = Math.max(0.0, Math.min(1.0, localAcc / interval));
            state = playMode == 2 ? "试听·奔跑" : "试听·行走";
        } else {
            interval = YsmClient.TIMER.dbgInterval > 0.01 ? YsmClient.TIMER.dbgInterval : interval;
            within = Math.max(0.0, Math.min(1.0, YsmClient.TIMER.dbgAcc / interval));
            state = c.enabled ? (YsmClient.TIMER.dbgMoving ? "游戏内·走着" : "游戏内·停着") : "总开关关";
        }

        int bx0 = x + 12;
        int bx1 = x + w - 12;
        int by = y + 30;
        int bh = 12;
        g.fill(bx0, by, bx1, by + bh, TRACK);
        int phX = bx0 + (int) ((bx1 - bx0) * within);
        g.fill(bx0, by, phX, by + bh, TRACK_FILL);

        if (YsmClient.TIMER.stepFlash > 0) {
            g.fill(bx0, by, bx1, by + bh, 0x22FFC24D);
        }

        // 半步 / 一步刻度
        int midX = bx0 + (bx1 - bx0) / 2;
        g.fill(midX - 1, by - 3, midX + 1, by + bh + 3, LINE);
        g.fill(bx1 - 1, by - 3, bx1, by + bh + 3, LINE);

        g.fill(Math.max(bx0, phX - 1), by - 4, Math.min(bx1, phX + 1), by + bh + 4, ACCENT);

        g.drawString(font, "0", bx0, by + bh + 5, FAINT, false);
        centered(g, font, "半步", midX, by + bh + 5, FAINT);
        String one = "一步";
        g.drawString(font, one, bx1 - font.width(one), by + bh + 5, FAINT, false);

        String line = String.format("当前相位 %.0f%%　·　%s", within * 100, state);
        g.drawString(font, line, x + 12, by + bh + 20, DIM, false);

        if (!hint.isEmpty() && y + h - 16 > by + bh + 20) {
            g.drawString(font, trim(hint, bx1 - bx0), x + 12, y + h - 16, HINT, false);
        }
    }

    private void drawPlayer(GuiGraphics g, Player player, float cx, float cy, float scale, float partial) {
        Vec3 savedMove = player.getDeltaMovement();
        double fake = FAKE_SPEED[playMode];
        if (fake > 0) {
            player.setDeltaMovement(0.0, savedMove.y, fake);
            player.walkAnimation.update(Math.min(1f, partial * 4f), 1f);
        }

        try {
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            MultiBufferSource.BufferSource buffer = g.bufferSource();

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            Lighting.setupForEntityInInventory();
            PoseStack ps = g.pose();
            ps.pushPose();
            ps.translate(cx, cy, 100f + scale);
            ps.scale(scale, scale, -scale);
            ps.mulPose(Axis.ZP.rotationDegrees(180f));
            ps.mulPose(Axis.XP.rotationDegrees(pitch));
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
            dispatcher.render(player, 0, 0, 0, 0f, partial, ps, buffer, 0xF000F0);
            ps.popPose();
            buffer.endBatch();
            Lighting.setupFor3DItems();

            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        } finally {
            player.setDeltaMovement(savedMove);
        }
    }

    // ───────────── 小工具 ─────────────

    private static void card(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, CARD);
        g.fill(x, y, x + w, y + 1, LINE);
        g.fill(x, y + h - 1, x + w, y + h, LINE);
        g.fill(x, y, x + 1, y + h, LINE);
        g.fill(x + w - 1, y, x + w, y + h, LINE);
    }

    private static void sectionTitle(GuiGraphics g, Font font, String title, int x, int y, int w,
                                     String rightHint) {
        g.drawString(font, title, x, y, DIM, false);
        int lineStart = x + font.width(title) + 6;
        int lineEnd = x + w;
        if (rightHint != null && !rightHint.isEmpty()) {
            int hw = font.width(rightHint);
            g.drawString(font, rightHint, x + w - hw, y, FAINT, false);
            lineEnd = x + w - hw - 6;
        }
        if (lineEnd > lineStart) {
            g.fill(lineStart, y + 4, lineEnd, y + 5, LINE);
        }
    }

    private static void centered(GuiGraphics g, Font font, String s, int cx, int y, int color) {
        g.drawString(font, s, cx - font.width(s) / 2, y, color, false);
    }

    private static String trim(String s, int maxWidth) {
        String out = s;
        while (out.length() > 4 && Minecraft.getInstance().font.width(out) > maxWidth) {
            out = out.substring(0, out.length() - 2);
        }
        return out;
    }

    // ───────────── 交互 ─────────────

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (inPreview(mx, my)) {
            if (button == 0) {
                panX += (float) dx;
                panY += (float) dy;
                return true;
            }
            if (button == 1) {
                yaw += (float) dx * 1.2f;
                pitch = (float) Math.max(-40, Math.min(30, pitch + dy * 0.8f));
                return true;
            }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // 菜单展开时优先吃点击：点菜单项 = 选中；点外面 = 关掉，并且不把这次点击漏给下层
        if (openMenu != null) {
            int x0 = menuX();
            int y0 = menuY();
            int x1 = x0 + menuW();
            int y1 = y0 + menuH();
            if (mx >= x0 && mx <= x1 && my >= y0 && my <= y1) {
                int idx = (int) ((my - y0 - 4) / menuRowH());
                if (idx >= 0 && idx < Mode.values().length) {
                    setMode(openMenu, Mode.values()[idx]);
                    return true;
                }
            }
            openMenu = null;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (openMenu != null && keyCode == 256) {   // Esc 先关菜单
            openMenu = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        // 滚轮优先给鼠标下的滑条：默认 0.1，Shift ×10，Ctrl ×0.1，相位再加 Alt ×0.01
        FlatSlider hit = sliderAt(mx, my);
        if (hit != null) {
            double mult = 1.0;
            if (hasShiftDown()) {
                mult = 10.0;
            } else if (hasControlDown()) {
                mult = 0.1;
            } else if (hasAltDown() && hit.altFine) {
                mult = 0.01;
            }
            hit.step(dy > 0 ? 1 : -1, 0.1 * mult);
            return true;
        }
        if (inPreview(mx, my)) {
            zoom = (float) Math.max(0.3, Math.min(4.0, zoom * (dy > 0 ? 1.1 : 0.9)));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    private FlatSlider sliderAt(double mx, double my) {
        for (FlatSlider s : new FlatSlider[]{cycleSlider, offsetSlider, volumeSlider}) {
            if (s != null && mx >= s.getX() && mx <= s.getX() + s.getWidth()
                    && my >= s.getY() && my <= s.getY() + s.getHeight()) {
                return s;
            }
        }
        return null;
    }

    private boolean inPreview(double mx, double my) {
        return mx >= MARGIN && mx <= MARGIN + leftW()
                && my >= contentTop() && my <= contentTop() + previewH() - ROW_H - 14;
    }

    // ───────────── 自绘控件 ─────────────

    /** 还原按钮：画一个转圈的箭头（不依赖字体里有没有 ↺ 这个字形）。 */
    private static class ResetButton extends AbstractWidget {

        private final String tip;
        private final Runnable action;

        ResetButton(int x, int y, int w, int h, String tip, Runnable action) {
            super(x, y, w, h, Component.empty());
            this.tip = tip;
            this.action = action;
            setTooltip(Tooltip.create(Component.literal(tip)));
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean hover = isHoveredOrFocused();
            int color = hover ? ACCENT : DIM;
            g.fill(getX(), getY(), getX() + width, getY() + height, hover ? CARD_HI : BTN_BG);
            g.fill(getX(), getY(), getX() + width, getY() + 1, LINE);
            g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, LINE);

            drawCircleArrow(g, getX() + width / 2, getY() + height / 2, color);
        }

        /** 圆形箭头图标：贴图 + 着色（贴图本身是白色的，靠 setShaderColor 上色）。 */
        private void drawCircleArrow(GuiGraphics g, int cx, int cy, int color) {
            int size = 14;
            float r = ((color >> 16) & 0xFF) / 255f;
            float gg = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            RenderSystem.setShaderColor(r, gg, b, 1f);
            // 10 参数版：采样区 (0,0,32,32) 整张贴图 → 画到 14×14 的格子里
            g.blit(RESET_ICON, cx - size / 2, cy - size / 2, size, size, 0f, 0f, 32, 32, 32, 32);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    /** 扁平按钮：无灰色贴图，靠底色和左侧强调条表达状态。 */    private static class FlatButton extends AbstractWidget {

        private final java.util.function.Supplier<Component> label;
        private final java.util.function.BooleanSupplier active;
        private final java.util.function.Consumer<FlatButton> press;

        FlatButton(int x, int y, int w, int h, java.util.function.Supplier<Component> label,
                   java.util.function.BooleanSupplier active, java.util.function.Consumer<FlatButton> press) {
            super(x, y, w, h, Component.empty());
            this.label = label;
            this.active = active;
            this.press = press;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean hover = isHoveredOrFocused();
            boolean on = active.getAsBoolean();
            g.fill(getX(), getY(), getX() + width, getY() + height, hover ? CARD_HI : BTN_BG);
            g.fill(getX(), getY(), getX() + width, getY() + 1, on ? ACCENT : LINE);
            g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, LINE);
            g.fill(getX(), getY(), getX() + 1, getY() + height, on ? ACCENT : LINE);
            g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, LINE);

            Component text = label.get();
            int color = on ? TEXT : (hover ? TEXT : DIM);
            g.drawString(Minecraft.getInstance().font, text, getX() + 8,
                    getY() + (height - 8) / 2, color, false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            press.accept(this);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    /** 扁平滑条：左侧标签，右侧数值，中间一条细轨。 */
    private static class FlatSlider extends AbstractSliderButton {

        private final String label;
        private final java.util.function.DoubleSupplier get;
        private final java.util.function.DoubleConsumer set;
        private final double lo;
        private final double hi;
        private final java.util.function.DoubleFunction<String> fmt;

        /** 相位这类需要更细调节的滑条：允许 Alt+滚轮再缩小一百倍。 */
        final boolean altFine;

        FlatSlider(int x, int y, int w, int h, String label, java.util.function.DoubleSupplier get,
                   java.util.function.DoubleConsumer set, double lo, double hi,
                   java.util.function.DoubleFunction<String> fmt) {
            this(x, y, w, h, label, get, set, lo, hi, fmt, false);
        }

        FlatSlider(int x, int y, int w, int h, String label, java.util.function.DoubleSupplier get,
                   java.util.function.DoubleConsumer set, double lo, double hi,
                   java.util.function.DoubleFunction<String> fmt, boolean altFine) {
            super(x, y, w, h, Component.empty(), (get.getAsDouble() - lo) / (hi - lo));
            this.label = label;
            this.get = get;
            this.set = set;
            this.lo = lo;
            this.hi = hi;
            this.fmt = fmt;
            this.altFine = altFine;
        }

        /** 滚轮调值：dir = +1/−1，按物理量纲给步长（周期 0.1s 之类）。 */
        void step(int dir, double amount) {
            double cur = lo + (hi - lo) * this.value;
            double next = StepConfig.clamp(cur + dir * amount, lo, hi);
            this.value = (next - lo) / (hi - lo);
            applyValue();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.empty());
        }

        @Override
        protected void applyValue() {
            set.accept(lo + (hi - lo) * this.value);
            YsmClient.CONFIG.save();
        }

        /** 配置在别处被改了（触拍算出新周期、点了还原）→ 把滑条挪到对应位置。 */
        void syncFromConfig() {
            double v = (get.getAsDouble() - lo) / (hi - lo);
            this.value = Math.max(0.0, Math.min(1.0, v));
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean hover = isHoveredOrFocused();
            g.fill(getX(), getY(), getX() + width, getY() + height, hover ? CARD_HI : BTN_BG);
            g.fill(getX(), getY(), getX() + width, getY() + 1, LINE);
            g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, LINE);

            String val = fmt.apply(lo + (hi - lo) * this.value);
            g.drawString(font, label, getX() + 8, getY() + (height - 8) / 2, DIM, false);
            g.drawString(font, val, getX() + width - 8 - font.width(val),
                    getY() + (height - 8) / 2, TEXT, false);

            int tx0 = getX() + 8 + font.width(label) + 8;
            int tx1 = getX() + width - 8 - font.width(val) - 8;
            if (tx1 > tx0 + 8) {
                int ty = getY() + height / 2;
                g.fill(tx0, ty - 1, tx1, ty + 1, SLIDER_TRACK);
                int kx = tx0 + (int) ((tx1 - tx0) * this.value);
                g.fill(tx0, ty - 1, kx, ty + 1, SLIDER_FILL);
                g.fill(kx - 2, ty - 3, kx + 2, ty + 3, hover ? ACCENT : KNOB);
            }
        }
    }
}

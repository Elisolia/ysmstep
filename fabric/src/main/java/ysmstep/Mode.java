package ysmstep;

/** 一拍该发什么声。 */
public enum Mode {

    /** 踩地：原版方块音，或交给 Presence Footsteps。 */
    STEP("step"),
    /** 不出声：整个人悬空的跑姿，硬踩地反而更假。 */
    NONE("none"),
    /** 鞘翅风声：悬浮、滑行。 */
    WIND("wind"),
    /** 拍翅：飞行生物。 */
    FLAP("flap"),
    /** 软脚：羊毛那类闷软声，轻足、灵体。 */
    SOFT("soft");

    private final String id;

    Mode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Mode parse(String s) {
        if (s != null) {
            for (Mode m : values()) {
                if (m.id.equalsIgnoreCase(s.trim())) {
                    return m;
                }
            }
        }
        return STEP;
    }

    public static boolean isValid(String s) {
        if (s == null) {
            return false;
        }
        for (Mode m : values()) {
            if (m.id.equalsIgnoreCase(s.trim())) {
                return true;
            }
        }
        return false;
    }

    public static String all() {
        StringBuilder sb = new StringBuilder();
        for (Mode m : values()) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append(m.id);
        }
        return sb.toString();
    }
}

package ysmstep;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * YSM 脚步同步（Fabric 版）。
 *
 * 问题：YSM 的走路动画按「动画时间」循环（周期固定），原版脚步声按「走过的距离」触发。
 * 两个钟不同，越走越错。
 *
 * 做法：压掉原版脚步声，改用「按动画周期走的节拍器」播脚步。
 * 客户端本地跑；校准好的档案可以上传到服务端，服务端存档 + 广播，
 * 别人就能按你模型的参数听你的脚步。
 *
 * 这个类是双端的，不许碰任何客户端专属类 —— 那些在 YsmClient 里。
 */
public class YsmStep {

    public static final String MODID = "ysmstep";
    public static final Logger LOG = LogUtils.getLogger();

    private YsmStep() {
    }
}

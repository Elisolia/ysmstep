package ysmstep.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ysmstep.RemoteSteps;
import ysmstep.YsmClient;

/**
 * 压掉「归我们管」的原版脚步声（距离驱动那套）：
 *   · 自己
 *   · 带着档案的其他玩家（服务端广播下来，我们按他的周期在本地重播）
 * 水里不动：原版的涉水声/水花是另一套，留着。
 */
@Mixin(Player.class)
public abstract class PlayerStepSoundMixin {

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void ysmstep$suppressVanillaStep(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!YsmClient.CONFIG.enabled) {
            return;
        }
        Player self = (Player) (Object) this;
        boolean mine = YsmClient.isLocalPlayer(self) || RemoteSteps.has(self);
        if (!mine) {
            return;
        }
        if (self.isInWater()) {
            return;
        }
        ci.cancel();
    }
}

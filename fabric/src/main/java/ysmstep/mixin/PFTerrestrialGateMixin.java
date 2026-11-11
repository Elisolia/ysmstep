package ysmstep.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ysmstep.PFCompat;

/**
 * Presence Footsteps (NeoForge) 的节拍闸门。
 *
 * 目标类是按名字打的（targets），不编译期依赖 PF；PF 不在时由 PFMixinPlugin 整体跳过。
 * remap = false：simulateFootsteps 是 PF 自己的方法名，不该被映射。
 *
 * 注意：只注入 simulateFootsteps，不碰 generateFootsteps ——
 * 跳跃落地(simulateAirborne)、擦草(simulateBrushes)、站着挪动(simulateStationary) 保持原样。
 */
@Mixin(targets = "eu.ha3.presencefootsteps.sound.generator.TerrestrialStepSoundGenerator", remap = false)
public abstract class PFTerrestrialGateMixin {

    @Shadow
    @Final
    protected LivingEntity entity;

    @Inject(method = "simulateFootsteps", at = @At("HEAD"), cancellable = true, remap = false)
    private void ysmstep$gateSimulateFootsteps(CallbackInfo ci) {
        if (PFCompat.gate(this.entity)) {
            ci.cancel();
        }
    }
}

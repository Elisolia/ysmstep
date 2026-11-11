package ysmstep.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ysmstep.Bob;

/**
 * 视角摇晃跟随（0.5.2）。
 *
 * 不 cancel、不重写 bobView 的数学——只把它读的四个字段换掉：
 *   walkDist / walkDistO → 动画节拍换算出的等效距离
 *   bob / oBob           → 幅度倍率
 *
 * 这样别的东西往 bobView 里注的东西（相机 mod、第一人称 mod）不受影响。
 *
 * 字段归属从字节码确认过（javap -c GameRenderer → bobView）：
 *   getfield Player.walkDist:F   ×2
 *   getfield Player.walkDistO:F
 *   getfield Player.oBob:F
 *   getfield Player.bob:F
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererBobMixin {

    @Redirect(
            method = "bobView",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Player;walkDist:F",
                    opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float ysmstep$walkDist(Player player) {
        return Bob.walkDist(player);
    }

    @Redirect(
            method = "bobView",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Player;walkDistO:F",
                    opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float ysmstep$walkDistO(Player player) {
        return Bob.walkDistO(player);
    }

    @Redirect(
            method = "bobView",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Player;oBob:F",
                    opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float ysmstep$oBob(Player player) {
        return Bob.oBob(player);
    }

    @Redirect(
            method = "bobView",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Player;bob:F",
                    opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float ysmstep$bob(Player player) {
        return Bob.bob(player);
    }
}

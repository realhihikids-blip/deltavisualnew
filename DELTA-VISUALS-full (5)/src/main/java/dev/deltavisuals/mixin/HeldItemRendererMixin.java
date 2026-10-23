package dev.deltavisuals.mixin;

import dev.deltavisuals.feature.HandState;
import dev.deltavisuals.module.Modules;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies Hand Transform (offset / scale / roll) and Hand Visibility to first-person hands and items.
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void deltavisuals$head(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                   float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                   OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        HandState.pushed = false;
        boolean main = hand == Hand.MAIN_HAND;

        if (Modules.HAND_VISIBILITY.isEnabled()) {
            if (item.isEmpty()) {
                if (Modules.HIDE_EMPTY.get()) {
                    ci.cancel();
                    return;
                }
            } else if ((main && Modules.HIDE_MAIN.get()) || (!main && Modules.HIDE_OFF.get())) {
                ci.cancel();
                return;
            }
        }

        if (Modules.HAND_TRANSFORM.isEnabled() && (main ? Modules.HAND_MAIN.get() : Modules.HAND_OFF.get())) {
            boolean rightSide = main == (player.getMainArm() == Arm.RIGHT);
            double side = rightSide ? 1.0 : -1.0;
            matrices.push();
            HandState.pushed = true;
            matrices.translate(Modules.HAND_X.get() * side, Modules.HAND_Y.get(), Modules.HAND_Z.get());
            float s = Modules.HAND_SCALE.getFloat();
            matrices.scale(s, s, s);
            float roll = Modules.HAND_ROLL.getFloat() * (float) side;
            if (roll != 0.0f) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
            }
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void deltavisuals$return(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                     float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                     OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        if (HandState.pushed) {
            matrices.pop();
            HandState.pushed = false;
        }
    }
}

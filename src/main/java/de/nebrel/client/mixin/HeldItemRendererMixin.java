package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the Item Model module's first-person transform.
 *
 * <p>Injected at the head of the first-person item render, before vanilla's own
 * swing and equip offsets, so the transform composes with them rather than
 * fighting them: the item still swings, it just does so from a different
 * resting position.</p>
 *
 * <p>The matrix is pushed here and popped at the tail, which keeps the change
 * scoped to this one item and leaves the stack exactly as it was found.</p>
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void nebrel$pushTransform(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                      Hand hand, float swingProgress, ItemStack stack,
                                      float equipProgress, MatrixStack matrices,
                                      VertexConsumerProvider consumers, int light,
                                      CallbackInfo info) {
        matrices.push();
        if (!NebrelState.itemModelActive) {
            return;
        }
        // Mirror the offset for the off hand so both hands move outwards.
        float side = hand == Hand.MAIN_HAND ? 1.0F : -1.0F;

        matrices.translate(NebrelState.itemOffsetX * side,
                NebrelState.itemOffsetY,
                NebrelState.itemOffsetZ);

        if (NebrelState.itemRotateX != 0.0F) {
            matrices.multiply(new Quaternionf().rotationX(
                    (float) Math.toRadians(NebrelState.itemRotateX)));
        }
        if (NebrelState.itemRotateY != 0.0F) {
            matrices.multiply(new Quaternionf().rotationY(
                    (float) Math.toRadians(NebrelState.itemRotateY * side)));
        }
        if (NebrelState.itemRotateZ != 0.0F) {
            matrices.multiply(new Quaternionf().rotationZ(
                    (float) Math.toRadians(NebrelState.itemRotateZ * side)));
        }

        float scale = NebrelState.itemScale;
        if (scale != 1.0F) {
            matrices.scale(scale, scale, scale);
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void nebrel$popTransform(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                     Hand hand, float swingProgress, ItemStack stack,
                                     float equipProgress, MatrixStack matrices,
                                     VertexConsumerProvider consumers, int light,
                                     CallbackInfo info) {
        matrices.pop();
    }
}

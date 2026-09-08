package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelState;
import de.nebrel.client.render.TintingVertexConsumer;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Recolours the enchantment glint for the Glint Colorizer module.
 *
 * <p>Both glint entry points are wrapped, so enchanted items and enchanted
 * armour are tinted consistently. When the module is off the original consumer
 * is returned untouched, which means the vanilla glint path is completely
 * unchanged and costs one field read.</p>
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(method = "getItemGlintConsumer", at = @At("RETURN"), cancellable = true)
    private static void nebrel$tintItemGlint(VertexConsumerProvider provider, RenderLayer layer,
                                             boolean solid, boolean glint,
                                             CallbackInfoReturnable<VertexConsumer> info) {
        nebrel$wrap(glint, info);
    }

    @Inject(method = "getArmorGlintConsumer", at = @At("RETURN"), cancellable = true)
    private static void nebrel$tintArmorGlint(VertexConsumerProvider provider, RenderLayer layer,
                                              boolean glint,
                                              CallbackInfoReturnable<VertexConsumer> info) {
        nebrel$wrap(glint, info);
    }

    private static void nebrel$wrap(boolean glint, CallbackInfoReturnable<VertexConsumer> info) {
        int tint = NebrelState.glintColor;
        if (!glint || tint == 0) {
            return;
        }
        VertexConsumer original = info.getReturnValue();
        if (original == null) {
            return;
        }
        int withOpacity = ColorUtil.withAlpha(tint,
                Math.round(ColorUtil.alpha(tint) * NebrelState.glintOpacity));
        info.setReturnValue(new TintingVertexConsumer(original, withOpacity));
    }
}

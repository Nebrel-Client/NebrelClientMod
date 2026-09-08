package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the FOV Changer module's field of view.
 *
 * <p>Injected at the tail so vanilla has already applied its own sprint and
 * speed modifiers; the module decides whether to keep, scale or replace the
 * result. Returning early when no override is set means the module costs a
 * single NaN check per frame while it is off.</p>
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void nebrel$applyFov(Camera camera, float tickDelta, boolean changingFov,
                                 CallbackInfoReturnable<Double> info) {
        double override = NebrelState.fovOverride;
        if (!Double.isNaN(override)) {
            info.setReturnValue(override);
        }
    }
}

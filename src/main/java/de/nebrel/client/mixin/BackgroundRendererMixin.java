package de.nebrel.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import de.nebrel.client.core.NebrelState;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Implements the No Fog module.
 *
 * <p>Runs after vanilla has configured the fog and pushes the start and end
 * distances out of view, rather than trying to prevent the fog being set up in
 * the first place. That keeps the injection to one point and leaves the sky and
 * water colours exactly as vanilla computed them.</p>
 *
 * <p>Each fog kind is gated separately, because underwater and lava fog carry
 * real information about where the player is and many people want to keep
 * them.</p>
 */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {

    @Inject(method = "applyFog", at = @At("RETURN"))
    private static void nebrel$applyFog(Camera camera, BackgroundRenderer.FogType fogType,
                                        float viewDistance, boolean thickFog, float tickDelta,
                                        CallbackInfo info) {
        CameraSubmersionType submersion = camera.getSubmersionType();

        boolean remove = switch (submersion) {
            case WATER -> NebrelState.disableWaterFog;
            case LAVA -> NebrelState.disableLavaFog;
            case POWDER_SNOW -> NebrelState.disablePowderSnowFog;
            case NONE -> NebrelState.disableTerrainFog;
        };

        if (remove) {
            // Push both planes far enough away that nothing is ever fogged.
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
            return;
        }

        float scale = NebrelState.fogDistanceScale;
        if (scale != 1.0F && submersion == CameraSubmersionType.NONE) {
            // Keep fog, but move it further out so the horizon opens up.
            RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * scale);
            RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * scale);
        }
    }
}

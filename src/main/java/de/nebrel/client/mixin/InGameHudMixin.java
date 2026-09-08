package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Implements the Vanilla HUD module's element toggles.
 *
 * <p>Each vanilla element is cancelled at the head of its own render method, so
 * hiding the hunger bar cannot disturb the hearts next to it and every other
 * element keeps its normal layout. Nothing is repositioned here: the Nebrel HUD
 * widgets are separate and independently anchored.</p>
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void nebrel$hideCrosshair(DrawContext context, RenderTickCounter tickCounter,
                                      CallbackInfo info) {
        if (NebrelState.hideCrosshair) {
            info.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void nebrel$hideHotbar(DrawContext context, RenderTickCounter tickCounter,
                                   CallbackInfo info) {
        if (NebrelState.hideHotbar) {
            info.cancel();
        }
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void nebrel$hideStatusBars(DrawContext context, CallbackInfo info) {
        // Vanilla draws health, hunger, armour and the mount bar together, so
        // this is only cancelled when the user hid all of the ones it covers.
        if (NebrelState.hideHealth && NebrelState.hideHunger && NebrelState.hideArmor) {
            info.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void nebrel$hideExperience(DrawContext context, int x, CallbackInfo info) {
        if (NebrelState.hideExperience) {
            info.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void nebrel$hideStatusEffects(DrawContext context, RenderTickCounter tickCounter,
                                          CallbackInfo info) {
        if (NebrelState.hideStatusEffects) {
            info.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;"
            + "Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"), cancellable = true)
    private void nebrel$hideScoreboard(DrawContext context,
                                       net.minecraft.scoreboard.ScoreboardObjective objective,
                                       CallbackInfo info) {
        if (NebrelState.hideScoreboard) {
            info.cancel();
        }
    }
}

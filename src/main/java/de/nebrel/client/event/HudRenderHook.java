package de.nebrel.client.event;

import net.minecraft.client.gui.DrawContext;

/** Implemented by modules that draw on the in-game HUD. */
public interface HudRenderHook {

    /**
     * @param context   the HUD draw context, already in screen space
     * @param tickDelta partial tick, for smoothing values between ticks
     */
    void onHudRender(DrawContext context, float tickDelta);
}

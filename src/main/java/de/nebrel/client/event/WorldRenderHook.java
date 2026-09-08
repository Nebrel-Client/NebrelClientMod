package de.nebrel.client.event;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

/** Implemented by modules that draw in world space. */
public interface WorldRenderHook {

    void onWorldRender(WorldRenderContext context);
}

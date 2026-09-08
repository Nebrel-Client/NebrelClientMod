package de.nebrel.client.event;

import net.minecraft.client.MinecraftClient;

/**
 * Implemented by modules that need to run logic once per client tick.
 *
 * <p>Only enabled modules that declare this interface are visited, so a module
 * that has nothing to do per tick costs nothing.</p>
 */
public interface ClientTickHook {

    void onTick(MinecraftClient client);
}

package de.nebrel.client;

import de.nebrel.client.config.ConfigLog;
import de.nebrel.client.core.ClientStats;
import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.core.NebrelClientLog;
import de.nebrel.client.core.NebrelState;
import de.nebrel.client.gui.screen.NebrelClientScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Nebrel Client's entry point.
 *
 * <p>Builds the client, then wires the four Fabric events it needs. Everything
 * beyond that is dispatched through {@code ModuleDispatcher}, which only visits
 * the modules that both implement a hook and are currently enabled.</p>
 */
public final class NebrelClientMod implements ClientModInitializer {

    public static final String MOD_ID = "nebrelclient";
    public static final String MOD_NAME = "Nebrel Client";

    @Override
    public void onInitializeClient() {
        // Route the config layer's warnings into the mod's own logger; until
        // this point it falls back to standard error.
        ConfigLog.setSink(NebrelClientLog::warn);

        NebrelClient client;
        try {
            client = NebrelClient.bootstrap();
        } catch (RuntimeException error) {
            NebrelClientLog.error(MOD_NAME + " failed to start; no modules will be available",
                    error);
            return;
        }

        registerEvents(client);
        NebrelClientLog.info(MOD_NAME + " ready with " + client.modules().size()
                + " modules and " + client.hud().widgets().size() + " HUD widgets");
    }

    private void registerEvents(NebrelClient client) {
        ClientStats stats = client.stats();

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            // Input first, so a keybind fired this tick is visible to every
            // module that runs afterwards.
            client.keybinds().tick(minecraft);
            stats.onTick(minecraft);
            client.plus().tick(minecraft);
            client.notifications().tick();
            client.dispatcher().dispatchTick(minecraft);
            client.config().tick();
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            stats.onFrame();
            float tickDelta = tickCounter.getTickDelta(false);
            client.dispatcher().dispatchHudRender(context, tickDelta);
            client.notifications().render(context,
                    MinecraftClient.getInstance().getWindow().getScaledWidth());
        });

        // AFTER_ENTITIES leaves the depth buffer populated, so anything drawn
        // through the lines layer is correctly occluded by terrain and mobs.
        WorldRenderEvents.AFTER_ENTITIES.register(client.dispatcher()::dispatchWorldRender);

        client.keybinds().onMenuKey(() -> {
            MinecraftClient minecraft = MinecraftClient.getInstance();
            if (minecraft.currentScreen == null) {
                minecraft.setScreen(new NebrelClientScreen());
            }
        });

        client.keybinds().onModuleToggled(module ->
                client.notifications().info(module.name(),
                        module.enabled() ? "Enabled" : "Disabled"));

        // Track clicks for the CPS readouts. Polled from the same input
        // snapshot every other consumer sees, so the numbers agree.
        client.keybinds().addTickListener(() -> {
            if (client.keybinds().mouseJustPressed(0)) {
                stats.recordLeftClick();
            }
            if (client.keybinds().mouseJustPressed(1)) {
                stats.recordRightClick();
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
            // Flush anything the debounce is still holding, then drop every
            // override so nothing can outlive the session.
            client.config().saveNow();
            NebrelState.resetAll();
            NebrelClientLog.info(MOD_NAME + " shut down cleanly");
        });
    }
}

package de.nebrel.client.event;

import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes client events to the modules that asked for them.
 *
 * <p>Rather than calling {@code onTick} on every registered module and letting
 * empty implementations return, this keeps one list per hook containing only
 * the modules that both implement it and are currently enabled. The lists are
 * rebuilt when module state changes, not per frame, so the per-frame cost is a
 * single indexed loop over exactly the modules that do work.</p>
 *
 * <p>A module that throws is disabled rather than allowed to spam the log every
 * frame or take the game down with it.</p>
 */
public final class ModuleDispatcher {

    private final ModuleManager modules;

    private List<ClientTickHook> tickHooks = List.of();
    private List<HudRenderHook> hudHooks = List.of();
    private List<WorldRenderHook> worldHooks = List.of();

    private java.util.function.BiConsumer<Module, Throwable> errorHandler = (module, error) -> {
    };

    public ModuleDispatcher(ModuleManager modules) {
        this.modules = modules;
    }

    public void setErrorHandler(java.util.function.BiConsumer<Module, Throwable> handler) {
        if (handler != null) {
            this.errorHandler = handler;
        }
    }

    /** Recomputes the hook lists. Called whenever a module is enabled or disabled. */
    public void rebuild() {
        List<ClientTickHook> ticks = new ArrayList<>();
        List<HudRenderHook> huds = new ArrayList<>();
        List<WorldRenderHook> worlds = new ArrayList<>();

        for (Module module : this.modules.getEnabledModules()) {
            if (module instanceof ClientTickHook hook) {
                ticks.add(hook);
            }
            if (module instanceof HudRenderHook hook) {
                huds.add(hook);
            }
            if (module instanceof WorldRenderHook hook) {
                worlds.add(hook);
            }
        }

        this.tickHooks = List.copyOf(ticks);
        this.hudHooks = List.copyOf(huds);
        this.worldHooks = List.copyOf(worlds);
    }

    // -- dispatch ------------------------------------------------------------

    public void dispatchTick(MinecraftClient client) {
        List<ClientTickHook> hooks = this.tickHooks;
        for (int i = 0; i < hooks.size(); i++) {
            ClientTickHook hook = hooks.get(i);
            try {
                hook.onTick(client);
            } catch (RuntimeException | LinkageError error) {
                fail(hook, error);
                return;
            }
        }
    }

    public void dispatchHudRender(DrawContext context, float tickDelta) {
        List<HudRenderHook> hooks = this.hudHooks;
        for (int i = 0; i < hooks.size(); i++) {
            HudRenderHook hook = hooks.get(i);
            try {
                hook.onHudRender(context, tickDelta);
            } catch (RuntimeException | LinkageError error) {
                fail(hook, error);
                return;
            }
        }
    }

    public void dispatchWorldRender(WorldRenderContext context) {
        List<WorldRenderHook> hooks = this.worldHooks;
        for (int i = 0; i < hooks.size(); i++) {
            WorldRenderHook hook = hooks.get(i);
            try {
                hook.onWorldRender(context);
            } catch (RuntimeException | LinkageError error) {
                fail(hook, error);
                return;
            }
        }
    }

    /**
     * Disables the offending module and reports it.
     *
     * <p>Rebuilding the lists mid-dispatch would invalidate the loop, so the
     * caller returns immediately afterwards; the remaining modules run on the
     * next frame.</p>
     */
    private void fail(Object hook, Throwable error) {
        if (hook instanceof Module module) {
            this.errorHandler.accept(module, error);
            this.modules.setEnabled(module, false);
        }
    }

    public int tickHookCount() {
        return this.tickHooks.size();
    }

    public int hudHookCount() {
        return this.hudHooks.size();
    }

    public int worldHookCount() {
        return this.worldHooks.size();
    }
}

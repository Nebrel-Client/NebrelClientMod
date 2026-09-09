package de.nebrel.client.core;

import de.nebrel.client.config.ClientSettings;
import de.nebrel.client.config.ConfigManager;
import de.nebrel.client.config.KeybindConfigSection;
import de.nebrel.client.config.ModuleConfigSection;
import de.nebrel.client.config.ThemeConfigSection;
import de.nebrel.client.event.ModuleDispatcher;
import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.ThemeManager;
import de.nebrel.client.hud.HudManager;
import de.nebrel.client.keybind.KeybindManager;
import de.nebrel.client.module.ModuleManager;
import de.nebrel.client.module.Modules;
import de.nebrel.client.notification.NotificationManager;
import de.nebrel.client.plus.NebrelPlus;
import de.nebrel.client.plus.PlusSettings;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * The client's composition root.
 *
 * <p>Everything is constructed here, in dependency order, and reached through
 * {@link #get()}. A single well-known entry point keeps the wiring visible in
 * one file instead of scattered statics, and makes the startup order explicit:
 * settings and theme, then the registries, then the config that fills them.</p>
 */
public final class NebrelClient {

    private static NebrelClient instance;

    private final ClientSettings settings;
    private final ThemeManager themes;
    private final ModuleManager modules;
    private final HudManager hud;
    private final ClientStats stats;
    private final NotificationManager notifications;
    private final KeybindManager keybinds;
    private final ModuleDispatcher dispatcher;
    private final ConfigManager config;
    private final UiContext ui;
    private final PlusSettings plusSettings;
    private final NebrelPlus plus;

    private NebrelClient(Path configRoot) {
        this.settings = new ClientSettings();
        this.themes = new ThemeManager();
        this.ui = new UiContext(this.themes, this.settings);

        // Nebrel+ is built before the modules, because the Custom Nametags
        // module installs itself into the nametag coordinator on construction.
        this.plusSettings = new PlusSettings();
        this.plus = new NebrelPlus(this.plusSettings);

        this.modules = new ModuleManager();
        this.hud = new HudManager();
        this.stats = new ClientStats();
        this.notifications = new NotificationManager(this.themes, this.settings);
        this.keybinds = new KeybindManager(this.modules, this.settings);
        this.dispatcher = new ModuleDispatcher(this.modules);

        // Modules and widgets must exist before the config is read, because the
        // config is keyed on their ids and settings.
        Modules.registerAll(this);

        this.config = new ConfigManager(configRoot);
        this.config.register(new ModuleConfigSection(this.modules))
                .register(new KeybindConfigSection(this.modules))
                .register(new ThemeConfigSection(this.themes))
                .register(this.hud)
                .register(this.settings)
                .register(this.plusSettings);

        // Any module state change means the config is stale and the dispatcher's
        // hook lists need rebuilding.
        this.modules.setChangeListener(() -> {
            this.dispatcher.rebuild();
            this.config.markDirty();
        });

        this.dispatcher.setErrorHandler((module, error) -> {
            NebrelClientLog.error("Module " + module.id() + " threw and was disabled", error);
            this.notifications.error(module.name() + " disabled",
                    "The module threw an error and was switched off.");
        });
    }

    /** Builds the singleton. Called once from the mod initialiser. */
    public static NebrelClient bootstrap() {
        if (instance != null) {
            return instance;
        }
        instance = new NebrelClient(FabricLoader.getInstance().getConfigDir());
        instance.load();
        return instance;
    }

    /** Builds against an explicit config root; used by tests and dev tooling. */
    public static NebrelClient bootstrap(Path configRoot) {
        if (instance != null) {
            return instance;
        }
        instance = new NebrelClient(configRoot);
        instance.load();
        return instance;
    }

    public static NebrelClient get() {
        if (instance == null) {
            throw new IllegalStateException("Nebrel Client has not been initialised yet");
        }
        return instance;
    }

    public static boolean ready() {
        return instance != null;
    }

    private void load() {
        this.config.load();
        // Enabled flags were restored without their callbacks; run them now that
        // every registry is populated.
        this.modules.activateLoadedModules();
        this.dispatcher.rebuild();

        // Nebrel+ is built before the config manager, so it is handed the dirty
        // hook rather than reaching for it — and only now, because restoring a
        // stored value is not a user edit and must not mark the file stale.
        this.plus.setChangeListener(this.config::markDirty);
    }

    // -- accessors -----------------------------------------------------------

    public ClientSettings settings() {
        return this.settings;
    }

    public ThemeManager themes() {
        return this.themes;
    }

    public ModuleManager modules() {
        return this.modules;
    }

    public HudManager hud() {
        return this.hud;
    }

    public ClientStats stats() {
        return this.stats;
    }

    public NotificationManager notifications() {
        return this.notifications;
    }

    public KeybindManager keybinds() {
        return this.keybinds;
    }

    public ModuleDispatcher dispatcher() {
        return this.dispatcher;
    }

    public ConfigManager config() {
        return this.config;
    }

    public UiContext ui() {
        return this.ui;
    }

    public NebrelPlus plus() {
        return this.plus;
    }

    public PlusSettings plusSettings() {
        return this.plusSettings;
    }

    /** The accent colour to draw HUD and in-world elements with. */
    public int accent() {
        return this.themes.accent();
    }
}

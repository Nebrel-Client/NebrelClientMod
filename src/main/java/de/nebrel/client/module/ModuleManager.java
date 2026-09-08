package de.nebrel.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The module registry.
 *
 * <p>Holds every module, keyed by its stable id, and maintains a few derived
 * views (per category, enabled set) that the GUI reads every frame. Those views
 * are rebuilt on mutation rather than on read, so drawing the menu allocates
 * nothing.</p>
 *
 * <p>Contains no Minecraft types; dispatching hooks to modules is the
 * responsibility of {@code de.nebrel.client.event.ModuleDispatcher}.</p>
 */
public final class ModuleManager {

    private final Map<String, Module> byId = new LinkedHashMap<>();
    private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<>();
    private final Map<ModuleCategory, List<Module>> byCategory = new EnumMap<>(ModuleCategory.class);

    private List<Module> ordered = List.of();
    private List<Module> enabled = List.of();

    private Runnable changeListener = () -> {
    };

    public ModuleManager() {
        for (ModuleCategory category : ModuleCategory.values()) {
            this.byCategory.put(category, List.of());
        }
    }

    /** Notified whenever a module is enabled, disabled or favourited. */
    public void setChangeListener(Runnable listener) {
        this.changeListener = listener == null ? () -> {
        } : listener;
    }

    // -- registration --------------------------------------------------------

    public <M extends Module> M register(M module) {
        Module previous = this.byId.putIfAbsent(module.id(), module);
        if (previous != null) {
            throw new IllegalStateException("Duplicate module id: " + module.id()
                    + " (" + previous.getClass().getName() + " and " + module.getClass().getName() + ")");
        }
        this.byClass.put(module.getClass(), module);
        rebuildViews();
        return module;
    }

    public void unregister(String id) {
        Module removed = this.byId.remove(id);
        if (removed == null) {
            return;
        }
        this.byClass.remove(removed.getClass());
        if (removed.enabled()) {
            removed.setEnabled(false);
        }
        rebuildViews();
    }

    private void rebuildViews() {
        this.ordered = List.copyOf(this.byId.values());

        Map<ModuleCategory, List<Module>> buckets = new EnumMap<>(ModuleCategory.class);
        for (ModuleCategory category : ModuleCategory.values()) {
            buckets.put(category, new ArrayList<>());
        }
        List<Module> enabledNow = new ArrayList<>();
        for (Module module : this.ordered) {
            buckets.get(module.category()).add(module);
            if (module.enabled()) {
                enabledNow.add(module);
            }
        }
        for (Map.Entry<ModuleCategory, List<Module>> entry : buckets.entrySet()) {
            this.byCategory.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        this.byCategory.put(ModuleCategory.ALL, this.ordered);
        this.enabled = Collections.unmodifiableList(enabledNow);
    }

    // -- state changes -------------------------------------------------------

    public void toggle(String id) {
        Module module = this.byId.get(id);
        if (module != null) {
            setEnabled(module, !module.enabled());
        }
    }

    public void enable(String id) {
        Module module = this.byId.get(id);
        if (module != null) {
            setEnabled(module, true);
        }
    }

    public void disable(String id) {
        Module module = this.byId.get(id);
        if (module != null) {
            setEnabled(module, false);
        }
    }

    public void setEnabled(Module module, boolean value) {
        if (module.enabled() == value) {
            return;
        }
        module.setEnabled(value);
        rebuildViews();
        this.changeListener.run();
    }

    public void setFavorite(Module module, boolean value) {
        if (module.favorite() == value) {
            return;
        }
        module.setFavorite(value);
        this.changeListener.run();
    }

    /**
     * Runs {@code onEnable} for every module the config restored as enabled.
     *
     * <p>Called once the client is far enough along that a module may safely
     * touch game state.</p>
     */
    public void activateLoadedModules() {
        for (Module module : this.ordered) {
            if (module.enabled()) {
                // restoreEnabled() set the flag without the callback; run it now.
                module.restoreEnabled(false);
                module.setEnabled(true);
            }
        }
        rebuildViews();
    }

    /** Applies a persisted enabled flag without firing the lifecycle callback. */
    public void restoreEnabled(Module module, boolean value) {
        module.restoreEnabled(value);
    }

    // -- lookups -------------------------------------------------------------

    public Optional<Module> getById(String id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    @SuppressWarnings("unchecked")
    public <M extends Module> M getByClass(Class<M> type) {
        Module module = this.byClass.get(type);
        if (module == null) {
            throw new IllegalStateException("Module not registered: " + type.getName());
        }
        return (M) module;
    }

    /** All modules in registration order. */
    public List<Module> getModules() {
        return this.ordered;
    }

    public List<Module> getEnabledModules() {
        return this.enabled;
    }

    public List<Module> getByCategory(ModuleCategory category) {
        if (category == ModuleCategory.FAVORITES) {
            return getFavorites();
        }
        return this.byCategory.getOrDefault(category, List.of());
    }

    public List<Module> getFavorites() {
        List<Module> result = new ArrayList<>();
        for (Module module : this.ordered) {
            if (module.favorite()) {
                result.add(module);
            }
        }
        return result;
    }

    /**
     * Category listing filtered by a search query.
     *
     * @param category the sidebar selection, including the virtual entries
     * @param query    free text; blank returns the whole category
     */
    public List<Module> search(ModuleCategory category, String query) {
        List<Module> source = getByCategory(category);
        if (query == null || query.isBlank()) {
            return source;
        }
        List<Module> result = new ArrayList<>();
        for (Module module : source) {
            if (module.matchesSearch(query)) {
                result.add(module);
            }
        }
        return result;
    }

    /** Search across every category, used when the query is non-empty. */
    public List<Module> search(String query) {
        return search(ModuleCategory.ALL, query);
    }

    public int size() {
        return this.byId.size();
    }
}

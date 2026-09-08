package de.nebrel.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.nebrel.client.config.ConfigSection;
import de.nebrel.client.setting.Setting;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the HUD widgets: registration, per-frame layout, drawing and persistence.
 *
 * <p>Doubles as its own {@link ConfigSection}, which is why {@code ConfigManager}
 * never has to know what a widget is.</p>
 */
public final class HudManager implements ConfigSection {

    private static final String WIDGETS = "widgets";
    private static final String OFFSET_X = "x";
    private static final String OFFSET_Y = "y";
    private static final String SETTINGS = "settings";

    private final Map<String, HudWidget> byId = new LinkedHashMap<>();
    private List<HudWidget> ordered = List.of();

    /** Set while the HUD editor is open, so widgets draw even when switched off. */
    private boolean editorPreview;

    public <W extends HudWidget> W register(W widget) {
        if (this.byId.putIfAbsent(widget.id(), widget) != null) {
            throw new IllegalStateException("Duplicate HUD widget id: " + widget.id());
        }
        this.ordered = List.copyOf(this.byId.values());
        return widget;
    }

    public List<HudWidget> widgets() {
        return this.ordered;
    }

    public Optional<HudWidget> byId(String id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    public void setEditorPreview(boolean value) {
        this.editorPreview = value;
    }

    // -- rendering -----------------------------------------------------------

    /**
     * Refreshes and draws every widget.
     *
     * @param accent the client accent colour, for widgets that opt into it
     */
    public void render(DrawContext context, float screenWidth, float screenHeight, int accent) {
        List<HudWidget> all = this.ordered;
        for (int i = 0; i < all.size(); i++) {
            HudWidget widget = all.get(i);
            if (!widget.enabled.get() && !this.editorPreview) {
                continue;
            }
            // One refresh per frame; measurement and drawing then agree.
            widget.update();
            if (!widget.hasContent() && !this.editorPreview) {
                continue;
            }
            widget.render(context, screenWidth, screenHeight, accent, this.editorPreview);
        }
    }

    /**
     * The topmost widget under the pointer.
     *
     * <p>Iterates back to front so the widget drawn last, and therefore on top,
     * wins a click when two overlap.</p>
     */
    public HudWidget widgetAt(double x, double y) {
        List<HudWidget> all = this.ordered;
        for (int i = all.size() - 1; i >= 0; i--) {
            HudWidget widget = all.get(i);
            if (widget.lastWidth() > 0.0F && widget.containsPoint(x, y)) {
                return widget;
            }
        }
        return null;
    }

    /** Restores every widget to its declared default position and style. */
    public void resetAll() {
        for (HudWidget widget : this.ordered) {
            for (Setting<?> setting : widget.settings()) {
                setting.reset();
            }
        }
    }

    public int enabledCount() {
        int count = 0;
        for (HudWidget widget : this.ordered) {
            if (widget.enabled.get()) {
                count++;
            }
        }
        return count;
    }

    // -- persistence ---------------------------------------------------------

    @Override
    public String fileName() {
        return "hud.json";
    }

    @Override
    public void write(JsonObject root) {
        JsonObject all = new JsonObject();
        for (HudWidget widget : this.ordered) {
            JsonObject entry = new JsonObject();
            entry.addProperty(OFFSET_X, widget.offsetX());
            entry.addProperty(OFFSET_Y, widget.offsetY());

            JsonObject settings = new JsonObject();
            for (Setting<?> setting : widget.settings()) {
                settings.add(setting.id(), setting.write());
            }
            entry.add(SETTINGS, settings);
            all.add(widget.id(), entry);
        }
        root.add(WIDGETS, all);
    }

    @Override
    public void read(JsonObject root) {
        if (!root.has(WIDGETS) || !root.get(WIDGETS).isJsonObject()) {
            return;
        }
        JsonObject all = root.getAsJsonObject(WIDGETS);
        for (HudWidget widget : this.ordered) {
            JsonElement stored = all.get(widget.id());
            if (stored == null || !stored.isJsonObject()) {
                // Widget added in a newer build: keep its default placement.
                continue;
            }
            JsonObject entry = stored.getAsJsonObject();
            widget.setOffset(
                    readFloat(entry, OFFSET_X, widget.offsetX()),
                    readFloat(entry, OFFSET_Y, widget.offsetY()));

            if (entry.has(SETTINGS) && entry.get(SETTINGS).isJsonObject()) {
                JsonObject settings = entry.getAsJsonObject(SETTINGS);
                for (Setting<?> setting : widget.settings()) {
                    JsonElement value = settings.get(setting.id());
                    if (value != null) {
                        setting.read(value);
                    }
                }
            }
        }
    }

    private static float readFloat(JsonObject object, String key, float fallback) {
        try {
            JsonElement element = object.get(key);
            if (element == null || !element.isJsonPrimitive()) {
                return fallback;
            }
            float value = element.getAsFloat();
            return Float.isFinite(value) ? value : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    /** Every widget setting, flattened; used by the module's settings view. */
    public List<Setting<?>> allSettings() {
        List<Setting<?>> result = new ArrayList<>();
        for (HudWidget widget : this.ordered) {
            result.addAll(widget.settings());
        }
        return result;
    }
}

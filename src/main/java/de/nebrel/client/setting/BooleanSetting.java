package de.nebrel.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** An on/off switch. */
public final class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String id, String name, String description, boolean defaultValue) {
        super(id, name, description, defaultValue);
    }

    public BooleanSetting(String id, String name, boolean defaultValue) {
        this(id, name, "", defaultValue);
    }

    public void toggle() {
        set(!get());
    }

    @Override
    public JsonElement write() {
        return new JsonPrimitive(get());
    }

    @Override
    public void read(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return;
        }
        try {
            set(element.getAsBoolean());
        } catch (RuntimeException ignored) {
            // keep the previous value on malformed input
        }
    }

    @Override
    public String displayValue() {
        return get() ? "On" : "Off";
    }
}

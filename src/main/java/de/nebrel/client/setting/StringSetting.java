package de.nebrel.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Free text, length limited so a pathological config cannot blow up the UI. */
public final class StringSetting extends Setting<String> {

    private final int maxLength;
    private String placeholder = "";

    public StringSetting(String id, String name, String description, String defaultValue, int maxLength) {
        super(id, name, description, defaultValue);
        this.maxLength = Math.max(1, maxLength);
    }

    public StringSetting(String id, String name, String defaultValue) {
        this(id, name, "", defaultValue, 256);
    }

    public int maxLength() {
        return this.maxLength;
    }

    public StringSetting placeholder(String text) {
        this.placeholder = text == null ? "" : text;
        return this;
    }

    public String placeholder() {
        return this.placeholder;
    }

    public boolean isBlank() {
        return get().isBlank();
    }

    @Override
    protected String sanitise(String candidate) {
        if (candidate == null) {
            return defaultValue();
        }
        return candidate.length() > this.maxLength
                ? candidate.substring(0, this.maxLength)
                : candidate;
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
            set(element.getAsString());
        } catch (RuntimeException ignored) {
            // keep the previous value on malformed input
        }
    }

    @Override
    public String displayValue() {
        return get();
    }
}

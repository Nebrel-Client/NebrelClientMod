package de.nebrel.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Locale;

/**
 * A single choice out of an enum's constants.
 *
 * <p>Persisted by {@link Enum#name()} so reordering the constants later does
 * not silently change anyone's config. An unknown name falls back to the
 * default rather than throwing.</p>
 *
 * @param <E> the enum type
 */
public final class EnumSetting<E extends Enum<E>> extends Setting<E> {

    private final Class<E> type;
    private final List<E> options;

    public EnumSetting(String id, String name, String description, E defaultValue) {
        super(id, name, description, defaultValue);
        this.type = defaultValue.getDeclaringClass();
        this.options = List.of(this.type.getEnumConstants());
    }

    public EnumSetting(String id, String name, E defaultValue) {
        this(id, name, "", defaultValue);
    }

    public List<E> options() {
        return this.options;
    }

    public int selectedIndex() {
        return this.options.indexOf(get());
    }

    /** Advances to the next constant, wrapping around. */
    public void cycle() {
        int next = (selectedIndex() + 1) % this.options.size();
        set(this.options.get(next));
    }

    public void select(int index) {
        if (index >= 0 && index < this.options.size()) {
            set(this.options.get(index));
        }
    }

    public boolean is(E candidate) {
        return get() == candidate;
    }

    /** Presentable label for a constant: {@code EASE_OUT} becomes "Ease Out". */
    public static String label(Enum<?> constant) {
        String raw = constant.toString();
        // An enum that overrides toString() already provides a display name.
        if (!raw.equals(constant.name())) {
            return raw;
        }
        String[] parts = constant.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder(constant.name().length());
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part, 1, part.length());
        }
        return builder.toString();
    }

    @Override
    public JsonElement write() {
        return new JsonPrimitive(get().name());
    }

    @Override
    public void read(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return;
        }
        String name = element.getAsString();
        for (E option : this.options) {
            if (option.name().equals(name)) {
                set(option);
                return;
            }
        }
        // Unknown constant: an option that was removed in a later version.
        // Leave the current value alone instead of failing the whole load.
    }

    @Override
    public String displayValue() {
        return label(get());
    }
}

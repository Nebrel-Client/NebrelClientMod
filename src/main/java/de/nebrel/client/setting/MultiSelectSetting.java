package de.nebrel.client.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Any subset of an enum's constants.
 *
 * <p>Backed by an {@link EnumSet}, so membership tests are a bitmask check.
 * That matters because these are queried per frame (which nametag parts to
 * draw, which particle types to boost).</p>
 *
 * @param <E> the enum type
 */
public final class MultiSelectSetting<E extends Enum<E>> extends Setting<Set<E>> {

    private final Class<E> type;
    private final List<E> options;

    // SafeVarargs: `defaults` is only ever read from, never stored or exposed,
    // so no heap pollution is possible. The nested suppression silences the
    // body-side lint for handing the array to setOf().
    @SafeVarargs
    @SuppressWarnings("varargs")
    public MultiSelectSetting(String id, String name, String description, Class<E> type, E... defaults) {
        super(id, name, description, unmodifiable(setOf(type, defaults)));
        this.type = type;
        this.options = List.of(type.getEnumConstants());
        // Give the live value its own mutable copy of the defaults.
        set(setOf(type, defaults));
    }

    private static <E extends Enum<E>> Set<E> setOf(Class<E> type, E[] values) {
        EnumSet<E> set = EnumSet.noneOf(type);
        if (values != null) {
            for (E value : values) {
                if (value != null) {
                    set.add(value);
                }
            }
        }
        return set;
    }

    private static <E extends Enum<E>> Set<E> unmodifiable(Set<E> set) {
        return Collections.unmodifiableSet(set);
    }

    public List<E> options() {
        return this.options;
    }

    /** Hot path: called per frame by rendering modules. */
    public boolean has(E option) {
        return get().contains(option);
    }

    public void toggle(E option) {
        EnumSet<E> next = EnumSet.copyOf(get().isEmpty() ? EnumSet.noneOf(this.type) : get());
        if (!next.remove(option)) {
            next.add(option);
        }
        set(next);
    }

    public void selectAll() {
        set(EnumSet.allOf(this.type));
    }

    public void clear() {
        set(EnumSet.noneOf(this.type));
    }

    @Override
    protected Set<E> sanitise(Set<E> candidate) {
        if (candidate == null) {
            return EnumSet.noneOf(this.type);
        }
        EnumSet<E> copy = EnumSet.noneOf(this.type);
        copy.addAll(candidate);
        return copy;
    }

    @Override
    public JsonElement write() {
        JsonArray array = new JsonArray();
        // Iterate the declaration order so the file is stable across saves.
        for (E option : this.options) {
            if (get().contains(option)) {
                array.add(option.name());
            }
        }
        return array;
    }

    @Override
    public void read(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return;
        }
        EnumSet<E> parsed = EnumSet.noneOf(this.type);
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonPrimitive()) {
                continue;
            }
            String name = entry.getAsString();
            for (E option : this.options) {
                if (option.name().equals(name)) {
                    parsed.add(option);
                    break;
                }
            }
            // Unknown names are dropped: an option removed in a later version.
        }
        set(parsed);
    }

    @Override
    public String displayValue() {
        Set<E> selected = get();
        if (selected.isEmpty()) {
            return "None";
        }
        if (selected.size() == this.options.size()) {
            return "All";
        }
        if (selected.size() == 1) {
            return EnumSetting.label(selected.iterator().next());
        }
        return selected.size() + " selected";
    }

    /** Ordered, human readable list of the selected options. */
    public List<String> selectedLabels() {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (E option : this.options) {
            if (get().contains(option)) {
                labels.add(EnumSetting.label(option));
            }
        }
        return List.copyOf(labels);
    }
}

package de.nebrel.client.setting;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Base class for every configurable value in the client.
 *
 * <p>A setting owns its value, knows how to serialise itself and can declare a
 * visibility predicate so dependent settings disappear when they do not apply
 * (for example a rainbow speed slider while rainbow mode is off).</p>
 *
 * @param <T> the value type
 */
public abstract class Setting<T> {

    private final String id;
    private final String name;
    private final String description;
    private final T defaultValue;

    private T value;
    private String section = SettingSection.GENERAL;
    private BooleanSupplier visibility = () -> true;
    private final List<Consumer<T>> listeners = new ArrayList<>(0);

    protected Setting(String id, String name, String description, T defaultValue) {
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String description() {
        return this.description;
    }

    public T defaultValue() {
        return this.defaultValue;
    }

    public T get() {
        return this.value;
    }

    public void set(T newValue) {
        T sanitised = sanitise(newValue);
        if (this.value != null && this.value.equals(sanitised)) {
            return;
        }
        this.value = sanitised;
        for (Consumer<T> listener : this.listeners) {
            listener.accept(sanitised);
        }
    }

    public void reset() {
        set(this.defaultValue);
    }

    public boolean isDefault() {
        // Compare against the sanitised default: a subclass may clamp or
        // quantise, and a value that survived that pass is still "the default".
        T normalised = sanitise(this.defaultValue);
        return this.value != null && this.value.equals(normalised);
    }

    /**
     * Hook for subclasses to clamp or reject a value. The default keeps it.
     */
    protected T sanitise(T candidate) {
        return candidate == null ? this.defaultValue : candidate;
    }

    // -- grouping and conditional visibility ---------------------------------

    /** Assigns the settings section this control is rendered under. */
    public Setting<T> section(String sectionName) {
        this.section = sectionName;
        return this;
    }

    public String section() {
        return this.section;
    }

    /**
     * Only shows this setting while {@code predicate} holds. Hidden settings
     * keep their value and are still saved; they are simply not drawn.
     */
    public Setting<T> visibleWhen(BooleanSupplier predicate) {
        this.visibility = predicate == null ? () -> true : predicate;
        return this;
    }

    /** Shorthand for {@code visibleWhen(dependency::get)}. */
    public Setting<T> visibleWhen(BooleanSetting dependency) {
        return visibleWhen(dependency::get);
    }

    public boolean visible() {
        return this.visibility.getAsBoolean();
    }

    public Setting<T> onChange(Consumer<T> listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
        return this;
    }

    // -- persistence ---------------------------------------------------------

    /** Serialises the current value. */
    public abstract JsonElement write();

    /**
     * Restores a value produced by {@link #write()}.
     *
     * <p>Implementations must never throw on malformed input; they fall back to
     * the default so a hand-edited or outdated config cannot crash the game.</p>
     */
    public abstract void read(JsonElement element);

    /** Human readable current value, used for the search index and tooltips. */
    public String displayValue() {
        return String.valueOf(this.value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.id + "=" + displayValue() + "]";
    }
}

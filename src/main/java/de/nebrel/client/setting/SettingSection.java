package de.nebrel.client.setting;

import java.util.List;

/**
 * The canonical section names a module's settings can be grouped under.
 *
 * <p>Sections are plain strings rather than an enum so a module can introduce a
 * bespoke group (for example "Widgets") without touching this class. The
 * settings view renders {@link #ORDER known sections first, in this order}, then
 * any custom section alphabetically, and skips sections that hold no visible
 * setting.</p>
 */
public final class SettingSection {

    public static final String GENERAL = "General";
    public static final String APPEARANCE = "Appearance";
    public static final String BEHAVIOR = "Behavior";
    public static final String ANIMATION = "Animation";
    public static final String ADVANCED = "Advanced";
    public static final String KEYBIND = "Keybind";

    /** Preferred display order for the built-in sections. */
    public static final List<String> ORDER = List.of(
            GENERAL, APPEARANCE, BEHAVIOR, ANIMATION, ADVANCED, KEYBIND);

    private SettingSection() {
    }

    /** Sort key: known sections keep {@link #ORDER}, unknown ones follow. */
    public static int rank(String section) {
        int index = ORDER.indexOf(section);
        return index < 0 ? ORDER.size() : index;
    }
}

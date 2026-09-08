package de.nebrel.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

/**
 * A single physical key, stored as its GLFW key code.
 *
 * <p>The code is kept as a plain int and the display names live in a table here
 * rather than being looked up through {@code InputUtil}, so keybinds can be
 * loaded, saved and shown without the game being initialised. {@link #UNBOUND}
 * ({@code GLFW_KEY_UNKNOWN}) means "no key".</p>
 */
public final class KeybindSetting extends Setting<Integer> {

    /** GLFW_KEY_UNKNOWN. */
    public static final int UNBOUND = -1;

    private static final Map<Integer, String> NAMES = new HashMap<>();

    static {
        NAMES.put(32, "Space");
        NAMES.put(39, "'");
        NAMES.put(44, ",");
        NAMES.put(45, "-");
        NAMES.put(46, ".");
        NAMES.put(47, "/");
        for (int i = 0; i <= 9; i++) {
            NAMES.put(48 + i, String.valueOf(i));
        }
        NAMES.put(59, ";");
        NAMES.put(61, "=");
        for (int i = 0; i < 26; i++) {
            NAMES.put(65 + i, String.valueOf((char) ('A' + i)));
        }
        NAMES.put(91, "[");
        NAMES.put(92, "\\");
        NAMES.put(93, "]");
        NAMES.put(96, "`");
        NAMES.put(256, "Escape");
        NAMES.put(257, "Enter");
        NAMES.put(258, "Tab");
        NAMES.put(259, "Backspace");
        NAMES.put(260, "Insert");
        NAMES.put(261, "Delete");
        NAMES.put(262, "Right");
        NAMES.put(263, "Left");
        NAMES.put(264, "Down");
        NAMES.put(265, "Up");
        NAMES.put(266, "Page Up");
        NAMES.put(267, "Page Down");
        NAMES.put(268, "Home");
        NAMES.put(269, "End");
        NAMES.put(280, "Caps Lock");
        NAMES.put(281, "Scroll Lock");
        NAMES.put(282, "Num Lock");
        NAMES.put(283, "Print Screen");
        NAMES.put(284, "Pause");
        for (int i = 1; i <= 25; i++) {
            NAMES.put(289 + i, "F" + i);
        }
        for (int i = 0; i <= 9; i++) {
            NAMES.put(320 + i, "Num " + i);
        }
        NAMES.put(330, "Num .");
        NAMES.put(331, "Num /");
        NAMES.put(332, "Num *");
        NAMES.put(333, "Num -");
        NAMES.put(334, "Num +");
        NAMES.put(335, "Num Enter");
        NAMES.put(336, "Num =");
        NAMES.put(340, "Left Shift");
        NAMES.put(341, "Left Ctrl");
        NAMES.put(342, "Left Alt");
        NAMES.put(343, "Left Super");
        NAMES.put(344, "Right Shift");
        NAMES.put(345, "Right Ctrl");
        NAMES.put(346, "Right Alt");
        NAMES.put(347, "Right Super");
        NAMES.put(348, "Menu");
    }

    public KeybindSetting(String id, String name, String description, int defaultKey) {
        super(id, name, description, defaultKey);
        section(SettingSection.KEYBIND);
    }

    public KeybindSetting(String id, String name, int defaultKey) {
        this(id, name, "", defaultKey);
    }

    public boolean bound() {
        return get() != UNBOUND;
    }

    public void clear() {
        set(UNBOUND);
    }

    public boolean matches(int keyCode) {
        return bound() && get() == keyCode;
    }

    /** Display name for a GLFW key code, e.g. {@code 344} to "Right Shift". */
    public static String keyName(int keyCode) {
        if (keyCode == UNBOUND) {
            return "None";
        }
        String known = NAMES.get(keyCode);
        return known != null ? known : "Key " + keyCode;
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
            set(element.getAsInt());
        } catch (RuntimeException ignored) {
            // keep the previous value on malformed input
        }
    }

    @Override
    public String displayValue() {
        return keyName(get());
    }
}

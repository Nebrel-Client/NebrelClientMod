package de.nebrel.client.plus.nametag;

import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.Setting;
import de.nebrel.client.plus.PlusSections;
import de.nebrel.client.setting.StringSetting;

import java.util.List;

/**
 * The optional second nametag line.
 *
 * <p>Free text a member writes themselves, which is exactly why it is
 * sanitised rather than trusted. {@link #sanitise} strips control characters,
 * collapses runs of whitespace and caps the length: without that, a section
 * sign could inject vanilla formatting into someone else's view, a newline
 * could break the line layout, and a long string could stretch a nametag across
 * the screen.</p>
 */
public final class AdditionalNametag {

    /** Hard cap, independent of the setting's own limit. */
    public static final int MAX_LENGTH = 32;

    /** Where the extra line sits relative to the player's name. */
    public enum Position {
        ABOVE("Above"),
        BELOW("Below");

        private final String display;

        Position(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    public final BooleanSetting enabled;
    public final StringSetting text;
    public final EnumSetting<Position> position;
    public final ColorSetting color;
    public final NumberSetting scale;
    public final BooleanSetting applyEffects;

    public AdditionalNametag() {
        this.enabled = new BooleanSetting("plus.additional.enabled", "Additional Nametag",
                "Show a second line under your name", false);
        this.enabled.section(PlusSections.ADDITIONAL);

        this.text = new StringSetting("plus.additional.text", "Text",
                "Up to " + MAX_LENGTH + " characters", "", MAX_LENGTH);
        this.text.placeholder("Nebrel Player");
        this.text.section(PlusSections.ADDITIONAL);
        this.text.visibleWhen(this.enabled);

        this.position = new EnumSetting<>("plus.additional.position", "Position",
                "Where the line sits relative to your name", Position.BELOW);
        this.position.section(PlusSections.ADDITIONAL);
        this.position.visibleWhen(this.enabled);

        this.color = new ColorSetting("plus.additional.color", "Color",
                "Colour of the extra line", 0xFF9A9AAB, true);
        this.color.section(PlusSections.ADDITIONAL);
        this.color.visibleWhen(this.enabled);

        this.scale = new NumberSetting("plus.additional.scale", "Scale",
                "Size relative to your name", 0.8D, 0.4D, 1.2D, 0.05D);
        this.scale.section(PlusSections.ADDITIONAL);
        this.scale.visibleWhen(this.enabled);

        this.applyEffects = new BooleanSetting("plus.additional.applyEffects", "Apply Effects",
                "Run the nametag effects over this line too", false);
        this.applyEffects.section(PlusSections.ADDITIONAL);
        this.applyEffects.visibleWhen(this.enabled);
    }

    public List<Setting<?>> settings() {
        return List.of(this.enabled, this.text, this.position, this.color, this.scale,
                this.applyEffects);
    }

    /** True when there is actually a line to draw. */
    public boolean active() {
        return this.enabled.get() && !resolvedText().isEmpty();
    }

    /** The sanitised text, ready to render. */
    public String resolvedText() {
        return sanitise(this.text.get());
    }

    /**
     * Makes user text safe to draw.
     *
     * <p>Removes control characters and the section sign, collapses whitespace
     * runs, trims, and caps the length. Returns an empty string for null.</p>
     */
    public static String sanitise(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(Math.min(raw.length(), MAX_LENGTH));
        boolean lastWasSpace = false;

        for (int i = 0; i < raw.length() && builder.length() < MAX_LENGTH; i++) {
            char c = raw.charAt(i);

            // The section sign starts a vanilla formatting code; letting it
            // through would let this field inject colours and obfuscation.
            if (c == '§') {
                continue;
            }
            // Control characters, including newline and tab, would break the
            // single-line layout.
            if (Character.isISOControl(c)) {
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (lastWasSpace || builder.length() == 0) {
                    continue;
                }
                builder.append(' ');
                lastWasSpace = true;
                continue;
            }
            builder.append(c);
            lastWasSpace = false;
        }

        // A trailing space can survive the loop above.
        int end = builder.length();
        while (end > 0 && builder.charAt(end - 1) == ' ') {
            end--;
        }
        return builder.substring(0, end);
    }

    /** Restores every setting on this line to its default. */
    public void reset() {
        for (Setting<?> setting : settings()) {
            setting.reset();
        }
    }
}

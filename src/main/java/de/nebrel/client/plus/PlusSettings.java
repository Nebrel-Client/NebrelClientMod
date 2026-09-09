package de.nebrel.client.plus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.nebrel.client.config.ConfigSection;
import de.nebrel.client.plus.nametag.NametagProfile;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.Setting;
import de.nebrel.client.setting.SettingSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Every Nebrel+ preference, and the file they live in.
 *
 * <p>Persisted to {@code config/nebrelclient/plus.json}. This file holds
 * presentation choices only — badge style, colours, effect tuning, the extra
 * line. It deliberately holds no account token, no session identifier and no
 * payment information, because a plain JSON file in the game directory is the
 * wrong place for any of those.</p>
 *
 * <p>{@link #developmentMode} is stored here as well. It grants local
 * entitlements so Nebrel+ can be built and looked at before a backend exists;
 * see {@link LocalEntitlementProvider} for why that is not a security hole.</p>
 */
public final class PlusSettings implements ConfigSection {

    /** How the badge is drawn. */
    public enum BadgeStyle {
        /** Rounded plate behind the glyph. */
        PLATE("Plate"),
        /** Outlined plate, no fill. */
        OUTLINE("Outline"),
        /** The glyph alone, no plate, no background. The default. */
        PLAIN("Plain"),
        /** Plate with the glyph in brackets, for very small text. */
        BRACKET("Bracket");

        private final String display;

        BadgeStyle(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    /** Where the badge takes its colour from. */
    public enum BadgeColorMode {
        NEBREL_ACCENT("Nebrel Accent"),
        CUSTOM("Custom"),
        MATCH_NAMETAG("Match Nametag");

        private final String display;

        BadgeColorMode(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    private final List<Setting<?>> settings = new ArrayList<>();

    // -- badge ---------------------------------------------------------------

    public final BooleanSetting badgeEnabled;
    public final EnumSetting<BadgeStyle> badgeStyle;
    public final EnumSetting<BadgeColorMode> badgeColorMode;
    public final ColorSetting badgeCustomColor;
    public final BooleanSetting animateBadge;
    public final NumberSetting badgeGap;
    public final NumberSetting badgeScale;

    // -- where identity is shown ---------------------------------------------

    public final BooleanSetting showInWorld;
    public final BooleanSetting showInTablist;
    public final BooleanSetting showInChat;

    // -- development ---------------------------------------------------------

    public final BooleanSetting developmentMode;

    private final NametagProfile nametag = new NametagProfile();

    public PlusSettings() {
        this.badgeEnabled = add(new BooleanSetting("plus.badge.enabled", "Nebrel+ Badge",
                "Show the N badge in front of your name", true));
        this.badgeEnabled.section(PlusSections.BADGE);

        // Plain by default: the badge sits in front of the name as a coloured
        // glyph with no plate or outline behind it, so it reads as part of the
        // name rather than as a separate chip stamped in front of it.
        this.badgeStyle = add(new EnumSetting<>("plus.badge.style", "Style",
                "How the badge is drawn", BadgeStyle.PLAIN));
        this.badgeStyle.section(PlusSections.BADGE);
        this.badgeStyle.visibleWhen(this.badgeEnabled);

        this.badgeColorMode = add(new EnumSetting<>("plus.badge.colorMode", "Color",
                "Where the badge takes its colour from", BadgeColorMode.NEBREL_ACCENT));
        this.badgeColorMode.section(PlusSections.BADGE);
        this.badgeColorMode.visibleWhen(this.badgeEnabled);

        this.badgeCustomColor = add(new ColorSetting("plus.badge.customColor", "Custom Color",
                "Badge colour when the mode is Custom", 0xFF8B5CF6, false));
        this.badgeCustomColor.section(PlusSections.BADGE);
        this.badgeCustomColor.visibleWhen(() -> this.badgeEnabled.get()
                && this.badgeColorMode.is(BadgeColorMode.CUSTOM));

        // Off by default on purpose. A badge is an identity marker; it has to
        // stay readable, and an animated one is harder to recognise at a glance.
        this.animateBadge = add(new BooleanSetting("plus.badge.animate", "Animate Color",
                "Let colour effects run over the badge as well as the name", false));
        this.animateBadge.section(PlusSections.BADGE);
        this.animateBadge.visibleWhen(this.badgeEnabled);

        this.badgeGap = add(new NumberSetting("plus.badge.gap", "Badge Gap",
                "Space between the badge and your name, in pixels", 2.0D, 0.0D, 8.0D, 0.5D));
        this.badgeGap.section(PlusSections.BADGE);
        this.badgeGap.visibleWhen(this.badgeEnabled);

        this.badgeScale = add(new NumberSetting("plus.badge.scale", "Badge Scale",
                "Badge size relative to your name", 1.0D, 0.7D, 1.3D, 0.05D));
        this.badgeScale.section(PlusSections.BADGE);
        this.badgeScale.visibleWhen(this.badgeEnabled);

        this.showInWorld = add(new BooleanSetting("plus.show.world", "Above Players",
                "Show badges and styling on the world nametag", true));
        this.showInWorld.section(PlusSections.VISIBILITY);

        this.showInTablist = add(new BooleanSetting("plus.show.tablist", "In the Tab List",
                "Show badges and colour styling in the player list", true));
        this.showInTablist.section(PlusSections.VISIBILITY);

        this.showInChat = add(new BooleanSetting("plus.show.chat", "In Chat",
                "Show badges and colour styling on chat messages", true));
        this.showInChat.section(PlusSections.VISIBILITY);

        this.developmentMode = add(new BooleanSetting("plus.developmentMode", "Development Mode",
                "Grant Nebrel+ locally so the features can be tried before accounts exist. "
                        + "Local only; a real membership will come from the Nebrel backend.",
                true));
        this.developmentMode.section(SettingSection.ADVANCED);
    }

    private <S extends Setting<?>> S add(S setting) {
        this.settings.add(setting);
        return setting;
    }

    public NametagProfile nametag() {
        return this.nametag;
    }

    /** Badge and visibility settings only. */
    public List<Setting<?>> badgeSettings() {
        return List.copyOf(this.settings);
    }

    /** Everything persisted by this section. */
    public List<Setting<?>> allSettings() {
        List<Setting<?>> result = new ArrayList<>(this.settings);
        result.addAll(this.nametag.settings());
        return result;
    }

    /** Restores every Nebrel+ preference, styling included, to its default. */
    public void resetAll() {
        for (Setting<?> setting : this.settings) {
            setting.reset();
        }
        this.nametag.reset();
    }

    // -- persistence ---------------------------------------------------------

    @Override
    public String fileName() {
        return "plus.json";
    }

    @Override
    public void write(JsonObject root) {
        JsonObject values = new JsonObject();
        for (Setting<?> setting : allSettings()) {
            values.add(setting.id(), setting.write());
        }
        root.add("plus", values);
    }

    @Override
    public void read(JsonObject root) {
        if (!root.has("plus") || !root.get("plus").isJsonObject()) {
            return;
        }
        JsonObject values = root.getAsJsonObject("plus");
        for (Setting<?> setting : allSettings()) {
            JsonElement stored = values.get(setting.id());
            if (stored != null) {
                setting.read(stored);
            }
        }
    }
}

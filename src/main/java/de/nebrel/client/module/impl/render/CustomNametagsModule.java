package de.nebrel.client.module.impl.render;

import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.plus.render.WorldNametagStyle;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Restyles the name plate above entities.
 *
 * <p>This module describes how the plate should look and what to append to the
 * name; it does not draw anything itself. {@code NametagCoordinator} owns the
 * label render, because Nebrel+ also has something to add there and two
 * independent renderers on the same label would mean drawing it twice.</p>
 *
 * <p>Everything shown comes from data the client already holds for a visible
 * entity, and the coordinator only takes over the label vanilla was already
 * about to draw.</p>
 */
public final class CustomNametagsModule extends Module {

    public final BooleanSetting background;
    public final ColorSetting backgroundColor;
    public final NumberSetting scale;
    public final NumberSetting opacity;
    public final BooleanSetting textShadow;
    public final BooleanSetting showHealth;
    public final BooleanSetting showDistance;
    public final BooleanSetting playersOnly;
    public final NumberSetting maxDistance;

    public CustomNametagsModule() {
        super("custom_nametags", "Custom Nametags", "Restyle the name plates above entities",
                ModuleCategory.RENDER, "🏷");

        this.background = bool("background", "Background", "Draw a plate behind the name", true);
        this.backgroundColor = color("backgroundColor", "Background Color", "Plate colour",
                0x99101016, true, SettingSection.APPEARANCE);
        this.backgroundColor.visibleWhen(this.background);

        this.scale = number("scale", "Scale", "Nametag size", 1.0D, 0.4D, 2.5D, 0.05D,
                SettingSection.APPEARANCE);
        this.opacity = number("opacity", "Opacity", "Overall transparency",
                1.0D, 0.1D, 1.0D, 0.05D, SettingSection.APPEARANCE);
        this.textShadow = bool("shadow", "Text Shadow", "Draw a shadow behind the text", true,
                SettingSection.APPEARANCE);

        this.showHealth = bool("health", "Show Health",
                "Append the entity's remaining health", false, SettingSection.BEHAVIOR);
        this.showDistance = bool("distance", "Show Distance",
                "Append how far away the entity is", false, SettingSection.BEHAVIOR);
        this.playersOnly = bool("playersOnly", "Players Only",
                "Leave mob and item-frame labels alone", true, SettingSection.BEHAVIOR);

        this.maxDistance = number("maxDistance", "Max Distance",
                "Stop restyling labels beyond this many blocks",
                48.0D, 8.0D, 128.0D, 4.0D, SettingSection.BEHAVIOR);
    }

    /**
     * The frame this module wants around a label.
     *
     * <p>Read once per label by the coordinator. When the module is off the
     * {@code active} flag is false and Nebrel+ falls back to a vanilla-like
     * plate, so switching this module off never leaves a half-styled label.</p>
     */
    public WorldNametagStyle style() {
        return new WorldNametagStyle(
                enabled(),
                this.background.get(),
                this.backgroundColor.get(),
                this.scale.getFloat(),
                this.opacity.getFloat(),
                this.textShadow.get(),
                this.maxDistance.get(),
                this.playersOnly.get(),
                "");
    }

    /**
     * Extra text appended to a name, such as health or distance.
     *
     * @return an empty string when this module is off or has nothing to add
     */
    public String suffixFor(Entity entity, double distance) {
        if (!enabled()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (this.showHealth.get() && entity instanceof LivingEntity living) {
            builder.append("  ").append(trim(living.getHealth())).append('/')
                    .append(trim(living.getMaxHealth()));
        }
        if (this.showDistance.get()) {
            builder.append("  ").append(Math.round(distance)).append('m');
        }
        return builder.toString();
    }

    /** True when this module would restyle the given entity's label. */
    public boolean appliesTo(Entity entity) {
        return enabled() && (!this.playersOnly.get() || entity instanceof PlayerEntity);
    }

    private static String trim(float value) {
        return value == Math.floor(value)
                ? String.valueOf((int) value)
                : String.format("%.1f", value);
    }
}

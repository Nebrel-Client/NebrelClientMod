package de.nebrel.client.plus.nametag;

import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.Setting;
import de.nebrel.client.plus.PlusSections;

import java.util.ArrayList;
import java.util.List;

/**
 * How one player's name is styled: base colour, effects and the extra line.
 *
 * <p>Separate from {@code PlusSettings} because a profile is per player.
 * Today only the local player has one, but the world and tablist renderers ask
 * for a profile by player rather than reading a global, so when a backend
 * starts sending other members' styles nothing downstream changes.</p>
 */
public final class NametagProfile {

    public final ColorSetting baseColor;
    public final BooleanSetting useNameColor;
    public final NametagEffectPipeline effects = new NametagEffectPipeline();
    public final AdditionalNametag additional = new AdditionalNametag();

    public NametagProfile() {
        this.useNameColor = new BooleanSetting("plus.nametag.useNameColor", "Keep Original Color",
                "Start from the colour the server gave your name", true);
        this.useNameColor.section(PlusSections.NAMETAG);

        this.baseColor = new ColorSetting("plus.nametag.baseColor", "Base Color",
                "Colour the effects start from", 0xFFFFFFFF, true);
        this.baseColor.section(PlusSections.NAMETAG);
        this.baseColor.visibleWhen(() -> !this.useNameColor.get());
    }

    /**
     * The colour effects start from.
     *
     * @param serverColor the colour the name already had, used when the user
     *                    chose to keep it
     */
    public int resolveBaseColor(int serverColor) {
        return this.useNameColor.get() ? serverColor : this.baseColor.get();
    }

    /** True when this profile changes the name's appearance at all. */
    public boolean modifiesName() {
        return this.effects.anyEnabled() || !this.useNameColor.get();
    }

    /** Every setting in the profile, for the designer and for config. */
    public List<Setting<?>> settings() {
        List<Setting<?>> result = new ArrayList<>();
        result.add(this.useNameColor);
        result.add(this.baseColor);
        result.addAll(this.effects.allSettings());
        result.addAll(this.additional.settings());
        return result;
    }

    /**
     * Restores the whole profile to defaults: every effect off, the base colour
     * back to the server's, and the extra line cleared.
     */
    public void reset() {
        this.useNameColor.reset();
        this.baseColor.reset();
        this.effects.reset();
        this.additional.reset();
    }

    /** Resets only the effects, leaving the extra line alone. */
    public void resetEffects() {
        this.effects.reset();
    }
}

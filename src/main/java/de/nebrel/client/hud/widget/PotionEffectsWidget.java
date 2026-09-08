package de.nebrel.client.hud.widget;

import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Active status effects with their remaining duration.
 *
 * <p>Rendered as text rather than sprites: it stays legible at any scale, needs
 * no texture binding, and shows modded effects that have no icon.</p>
 */
public final class PotionEffectsWidget extends LabeledHudWidget {

    private static final String[] ROMAN = {"", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public final BooleanSetting showDuration;
    public final BooleanSetting showAmplifier;
    public final BooleanSetting hideAmbient;

    /** Reused between frames to avoid allocating while sorting. */
    private final List<StatusEffectInstance> sorted = new ArrayList<>(8);

    public PotionEffectsWidget() {
        super("effects", "Potion Effects", HudAnchor.MIDDLE_RIGHT, -4.0F, 0.0F, false);

        this.showDuration = add(new BooleanSetting("hud.effects.duration", "Duration",
                "Show the remaining time", true));
        this.showDuration.section(SettingSection.APPEARANCE);

        this.showAmplifier = add(new BooleanSetting("hud.effects.amplifier", "Level",
                "Append the effect level", true));
        this.showAmplifier.section(SettingSection.APPEARANCE);

        this.hideAmbient = add(new BooleanSetting("hud.effects.hideAmbient", "Hide Beacon Effects",
                "Skip ambient effects from beacons and conduits", false));
        this.hideAmbient.section(SettingSection.BEHAVIOR);
    }

    @Override
    protected void buildRows() {
        ClientPlayerEntity player = client().player;
        if (player == null) {
            return;
        }
        this.sorted.clear();
        for (StatusEffectInstance instance : player.getStatusEffects()) {
            if (this.hideAmbient.get() && instance.isAmbient()) {
                continue;
            }
            this.sorted.add(instance);
        }
        // Shortest remaining first: the one about to run out is the one that matters.
        this.sorted.sort(Comparator.comparingInt(StatusEffectInstance::getDuration));

        for (int i = 0; i < this.sorted.size(); i++) {
            StatusEffectInstance instance = this.sorted.get(i);
            String name = instance.getEffectType().value().getName().getString();
            if (this.showAmplifier.get()) {
                int amplifier = instance.getAmplifier();
                if (amplifier > 0 && amplifier < ROMAN.length) {
                    name = name + " " + ROMAN[amplifier];
                } else if (amplifier >= ROMAN.length) {
                    name = name + " " + (amplifier + 1);
                }
            }
            row(name, this.showDuration.get() ? formatDuration(instance) : "");
        }
    }

    private static String formatDuration(StatusEffectInstance instance) {
        if (instance.isInfinite()) {
            return "∞";
        }
        int seconds = instance.getDuration() / 20;
        int minutes = seconds / 60;
        return String.format("%d:%02d", minutes, seconds % 60);
    }
}

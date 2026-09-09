package de.nebrel.client.plus.nametag.effect;

import de.nebrel.client.plus.nametag.EffectStage;
import de.nebrel.client.plus.nametag.NametagEffect;
import de.nebrel.client.plus.nametag.NametagRenderContext;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;

/** A sine wave travelling along the name. */
public final class WavingEffect extends NametagEffect {

    public final NumberSetting speed;
    public final NumberSetting amplitude;
    public final NumberSetting frequency;
    public final BooleanSetting horizontal;

    public WavingEffect() {
        super("waving", "Waving", "Ripple the name like a banner", EffectStage.POSITION);

        this.speed = number("speed", "Speed", "How fast the wave travels",
                1.5D, 0.1D, 6.0D, 0.1D);
        this.amplitude = number("amplitude", "Amplitude", "Wave height in pixels",
                1.2D, 0.1D, 6.0D, 0.1D);
        this.frequency = number("frequency", "Frequency",
                "How many wave crests fit across the name", 1.0D, 0.2D, 4.0D, 0.1D);
        this.horizontal = bool("horizontal", "Horizontal",
                "Also sway sideways, a quarter phase behind", false);
    }

    @Override
    public void apply(NametagRenderContext context) {
        // Character index drives the wave along the word; time moves it.
        double phase = context.charIndex() * this.frequency.get() * 0.6D
                - context.time() * this.speed.get() * Math.PI * 2.0D;

        float y = (float) Math.sin(phase) * this.amplitude.getFloat();
        // A quarter turn behind the vertical wave gives a circular sway rather
        // than a diagonal one.
        float x = this.horizontal.get()
                ? (float) Math.cos(phase) * this.amplitude.getFloat() * 0.4F
                : 0.0F;

        context.addOffset(x, y);
    }
}

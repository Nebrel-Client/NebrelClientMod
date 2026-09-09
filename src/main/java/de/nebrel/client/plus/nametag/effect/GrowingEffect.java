package de.nebrel.client.plus.nametag.effect;

import de.nebrel.client.plus.nametag.EffectStage;
import de.nebrel.client.plus.nametag.NametagEffect;
import de.nebrel.client.plus.nametag.NametagRenderContext;
import de.nebrel.client.setting.NumberSetting;

/** A size pulse that travels along the name. */
public final class GrowingEffect extends NametagEffect {

    public final NumberSetting minimumScale;
    public final NumberSetting maximumScale;
    public final NumberSetting speed;
    public final NumberSetting characterOffset;

    public GrowingEffect() {
        super("growing", "Growing", "Pulse the size of each character", EffectStage.TRANSFORM);

        this.minimumScale = number("min", "Minimum Scale", "Smallest size in the pulse",
                0.85D, 0.4D, 1.5D, 0.01D);
        this.maximumScale = number("max", "Maximum Scale", "Largest size in the pulse",
                1.15D, 0.4D, 2.0D, 0.01D);
        this.speed = number("speed", "Speed", "Pulses per second", 1.0D, 0.1D, 5.0D, 0.1D);
        this.characterOffset = number("offset", "Character Offset",
                "How far apart the characters pulse; 0 pulses the whole name together",
                0.2D, 0.0D, 1.0D, 0.05D);
    }

    @Override
    public void apply(NametagRenderContext context) {
        double phase = context.time() * this.speed.get()
                + context.charIndex() * this.characterOffset.get();
        float wave = (float) (0.5D + 0.5D * Math.sin(phase * Math.PI * 2.0D));

        float low = this.minimumScale.getFloat();
        float high = this.maximumScale.getFloat();
        // Crossed sliders shrink instead of inverting the pulse.
        if (low > high) {
            float swap = low;
            low = high;
            high = swap;
        }

        context.multiplyScale(low + (high - low) * wave);
    }
}

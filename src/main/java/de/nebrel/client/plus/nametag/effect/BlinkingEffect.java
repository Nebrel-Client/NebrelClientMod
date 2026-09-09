package de.nebrel.client.plus.nametag.effect;

import de.nebrel.client.plus.nametag.EffectStage;
import de.nebrel.client.plus.nametag.NametagEffect;
import de.nebrel.client.plus.nametag.NametagRenderContext;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.util.NebrelMath;

/**
 * A soft pulse in and out.
 *
 * <p>Driven by a sine rather than a square wave, so it fades rather than
 * flickering. A hard on/off blink at any readable speed is unpleasant to look
 * at and makes the name hard to read, which defeats the point of a nametag.</p>
 */
public final class BlinkingEffect extends NametagEffect {

    public final NumberSetting speed;
    public final NumberSetting minimumAlpha;
    public final NumberSetting maximumAlpha;
    public final BooleanSetting perCharacter;
    public final NumberSetting characterOffset;

    public BlinkingEffect() {
        super("blinking", "Blinking", "Fade the name in and out", EffectStage.ALPHA);

        this.speed = number("speed", "Speed", "Pulses per second", 1.0D, 0.1D, 5.0D, 0.1D);
        this.minimumAlpha = number("min", "Minimum Alpha", "Faintest point of the pulse",
                0.25D, 0.0D, 1.0D, 0.05D);
        this.maximumAlpha = number("max", "Maximum Alpha", "Brightest point of the pulse",
                1.0D, 0.0D, 1.0D, 0.05D);

        this.perCharacter = bool("perCharacter", "Per Character",
                "Offset each character so the pulse travels along the name", false);
        this.characterOffset = number("offset", "Character Offset",
                "How far apart the characters pulse", 0.15D, 0.0D, 1.0D, 0.05D);
        this.characterOffset.visibleWhen(this.perCharacter);
    }

    @Override
    public void apply(NametagRenderContext context) {
        float phase = context.time() * this.speed.getFloat();
        if (this.perCharacter.get()) {
            phase += context.charIndex() * this.characterOffset.getFloat();
        }

        // Sine mapped from -1..1 onto 0..1, then onto the chosen alpha band.
        float wave = (float) (0.5D + 0.5D * Math.sin(phase * Math.PI * 2.0D));

        float low = this.minimumAlpha.getFloat();
        float high = this.maximumAlpha.getFloat();
        // Tolerate the sliders being crossed rather than inverting the pulse.
        if (low > high) {
            float swap = low;
            low = high;
            high = swap;
        }

        context.multiplyAlpha(NebrelMath.clamp(low + (high - low) * wave, 0.0F, 1.0F));
    }
}

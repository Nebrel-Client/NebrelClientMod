package de.nebrel.client.plus.nametag.effect;

import de.nebrel.client.plus.nametag.EffectStage;
import de.nebrel.client.plus.nametag.NametagEffect;
import de.nebrel.client.plus.nametag.NametagRenderContext;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;

/**
 * A chromatic shimmer: the three channels drift out of phase with each other.
 *
 * <p>Distinct from rainbow. Rainbow replaces the colour outright; this keeps the
 * name's own colour and pushes the red, green and blue channels apart, which
 * reads as a metallic sheen rather than a hue cycle. Because it modulates the
 * existing colour it also composes with rainbow rather than overwriting it.</p>
 */
public final class ChromaticEffect extends NametagEffect {

    public final NumberSetting strength;
    public final NumberSetting speed;
    public final NumberSetting offset;

    public ChromaticEffect() {
        super("chromatic", "Chromatic", "Split the colour channels for a metallic sheen",
                EffectStage.COLOR);

        this.strength = number("strength", "Strength", "How far the channels separate",
                0.4D, 0.05D, 1.0D, 0.05D);
        this.speed = number("speed", "Speed", "How fast the sheen travels",
                0.6D, 0.05D, 3.0D, 0.05D);
        this.offset = number("offset", "Offset",
                "Phase difference between the channels", 0.33D, 0.0D, 1.0D, 0.01D);
    }

    @Override
    public void apply(NametagRenderContext context) {
        int source = context.color();
        float phase = context.time() * this.speed.getFloat() + context.progress();
        float channelOffset = this.offset.getFloat();
        float amount = this.strength.getFloat();

        // Each channel rides its own sine, a fixed phase apart.
        float red = modulate(ColorUtil.red(source), phase, amount);
        float green = modulate(ColorUtil.green(source), phase + channelOffset, amount);
        float blue = modulate(ColorUtil.blue(source), phase + channelOffset * 2.0F, amount);

        context.setColor(ColorUtil.argb(
                ColorUtil.alpha(source),
                Math.round(red), Math.round(green), Math.round(blue)));
    }

    /** Scales one channel by a sine, clamped to a valid byte. */
    private static float modulate(int channel, float phase, float amount) {
        float wave = (float) Math.sin(phase * Math.PI * 2.0D);
        // Centre on 1.0 so a strength of 0 leaves the channel untouched.
        float factor = 1.0F + wave * amount;
        return NebrelMath.clamp(channel * factor, 0.0F, 255.0F);
    }
}

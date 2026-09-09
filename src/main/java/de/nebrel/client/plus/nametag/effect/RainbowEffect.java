package de.nebrel.client.plus.nametag.effect;

import de.nebrel.client.plus.nametag.EffectStage;
import de.nebrel.client.plus.nametag.NametagEffect;
import de.nebrel.client.plus.nametag.NametagRenderContext;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.util.ColorUtil;

/** A hue that cycles over time and spreads along the name. */
public final class RainbowEffect extends NametagEffect {

    public final NumberSetting speed;
    public final NumberSetting spread;
    public final NumberSetting saturation;
    public final NumberSetting brightness;

    public RainbowEffect() {
        super("rainbow", "Rainbow", "Cycle the name through the spectrum", EffectStage.COLOR);

        this.speed = number("speed", "Speed", "Full colour cycles per second",
                0.3D, 0.05D, 3.0D, 0.05D);
        this.spread = number("spread", "Spread",
                "How much of the spectrum the name covers at once; 0 colours it uniformly",
                0.5D, 0.0D, 2.0D, 0.05D);
        this.saturation = number("saturation", "Saturation", "Colour intensity",
                0.8D, 0.1D, 1.0D, 0.05D);
        this.brightness = number("brightness", "Brightness", "Colour lightness",
                1.0D, 0.3D, 1.0D, 0.05D);
    }

    @Override
    public void apply(NametagRenderContext context) {
        // Time advances the whole rainbow; progress fans it out across the word.
        float hue = context.time() * this.speed.getFloat()
                + context.progress() * this.spread.getFloat();
        hue -= (float) Math.floor(hue);

        int rgb = ColorUtil.hsbToRgb(hue, this.saturation.getFloat(), this.brightness.getFloat());
        // Keep whatever alpha the base colour carried; blinking owns alpha.
        context.setColor(ColorUtil.withAlpha(rgb, ColorUtil.alpha(context.color())));
    }
}

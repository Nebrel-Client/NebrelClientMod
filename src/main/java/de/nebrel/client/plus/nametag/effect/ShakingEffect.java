package de.nebrel.client.plus.nametag.effect;

import de.nebrel.client.plus.nametag.EffectStage;
import de.nebrel.client.plus.nametag.NametagEffect;
import de.nebrel.client.plus.nametag.NametagRenderContext;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;

/**
 * Jitters each character.
 *
 * <p>Uses smooth deterministic noise rather than a random number generator.
 * Calling {@code Math.random()} per glyph per frame would make the text
 * strobe — every frame an unrelated position — and would make the designer
 * preview disagree with the real nametag. Hashed noise interpolated between
 * whole steps looks irregular but is the same every time it is evaluated, so
 * the motion is a shake rather than a flicker.</p>
 */
public final class ShakingEffect extends NametagEffect {

    public final NumberSetting strength;
    public final NumberSetting speed;
    public final BooleanSetting vertical;

    public ShakingEffect() {
        super("shaking", "Shaking", "Jitter each character", EffectStage.POSITION);

        this.strength = number("strength", "Strength", "How far characters move, in pixels",
                0.8D, 0.1D, 4.0D, 0.1D);
        this.speed = number("speed", "Speed", "How fast the jitter changes",
                8.0D, 1.0D, 30.0D, 0.5D);
        this.vertical = bool("vertical", "Vertical", "Shake up and down as well as sideways",
                true);
    }

    @Override
    public void apply(NametagRenderContext context) {
        float amount = this.strength.getFloat();
        // Each character walks its own noise sequence, offset far enough apart
        // that neighbours never move in step.
        float seed = context.time() * this.speed.getFloat() + context.charIndex() * 37.0F;

        float x = noise(seed, context.charIndex() * 2 + 1) * amount;
        float y = this.vertical.get()
                ? noise(seed, context.charIndex() * 2 + 2) * amount
                : 0.0F;

        context.addOffset(x, y);
    }
}

package de.nebrel.client.plus.nametag.effect;

import de.nebrel.client.plus.nametag.EffectStage;
import de.nebrel.client.plus.nametag.NametagEffect;
import de.nebrel.client.plus.nametag.NametagRenderContext;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;

/**
 * Leans each character back and forth.
 *
 * <p>Reported as a skew amount rather than applied to a matrix here. The
 * renderer owns the matrix stack and is responsible for pushing and popping
 * around each glyph; an effect that pushed a transform of its own would leave
 * the stack unbalanced the moment two transform effects were combined.</p>
 */
public final class SkewingEffect extends NametagEffect {

    public final NumberSetting strength;
    public final NumberSetting speed;
    public final BooleanSetting alternate;

    public SkewingEffect() {
        super("skewing", "Skewing", "Lean the characters back and forth", EffectStage.TRANSFORM);

        this.strength = number("strength", "Strength", "How far the characters lean",
                0.25D, 0.02D, 1.0D, 0.01D);
        this.speed = number("speed", "Speed", "How fast the lean oscillates",
                1.0D, 0.1D, 5.0D, 0.1D);
        this.alternate = bool("alternate", "Alternate",
                "Lean neighbouring characters in opposite directions", false);
    }

    @Override
    public void apply(NametagRenderContext context) {
        double phase = context.time() * this.speed.get() * Math.PI * 2.0D
                + context.progress() * Math.PI;

        float skew = (float) Math.sin(phase) * this.strength.getFloat();
        if (this.alternate.get() && (context.charIndex() & 1) == 1) {
            skew = -skew;
        }
        context.addSkew(skew);
    }
}

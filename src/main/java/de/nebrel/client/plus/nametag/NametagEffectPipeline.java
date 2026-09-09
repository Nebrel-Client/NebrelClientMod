package de.nebrel.client.plus.nametag;

import de.nebrel.client.plus.nametag.effect.BlinkingEffect;
import de.nebrel.client.plus.nametag.effect.ChromaticEffect;
import de.nebrel.client.plus.nametag.effect.GrowingEffect;
import de.nebrel.client.plus.nametag.effect.RainbowEffect;
import de.nebrel.client.plus.nametag.effect.ShakingEffect;
import de.nebrel.client.plus.nametag.effect.SkewingEffect;
import de.nebrel.client.plus.nametag.effect.WavingEffect;
import de.nebrel.client.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the effects over a glyph, in stage order.
 *
 * <p>Effects are held in a fixed list and the per-glyph pass walks it once,
 * skipping the ones that are off. Sorting or filtering per glyph would allocate
 * on the hottest path in the client — every character of every visible nametag,
 * every frame — so the ordering is baked in at construction and the pass is a
 * plain indexed loop.</p>
 *
 * <p>Effects are declared in stage order here, which is what makes combining
 * them predictable: colour is decided first, alpha then fades that colour,
 * position moves the glyph and transform scales it.</p>
 */
public final class NametagEffectPipeline {

    public final RainbowEffect rainbow = new RainbowEffect();
    public final ChromaticEffect chromatic = new ChromaticEffect();
    public final BlinkingEffect blinking = new BlinkingEffect();
    public final ShakingEffect shaking = new ShakingEffect();
    public final WavingEffect waving = new WavingEffect();
    public final SkewingEffect skewing = new SkewingEffect();
    public final GrowingEffect growing = new GrowingEffect();

    /** In stage order: COLOR, ALPHA, POSITION, TRANSFORM. */
    private final List<NametagEffect> effects = List.of(
            this.rainbow,
            this.chromatic,
            this.blinking,
            this.shaking,
            this.waving,
            this.skewing,
            this.growing);

    private final NametagRenderContext context = new NametagRenderContext();

    public NametagEffectPipeline() {
        assertStageOrder();
    }

    /**
     * Guards the invariant the class depends on.
     *
     * <p>The declaration order above <em>is</em> the execution order. If an
     * effect were added in the wrong place, combinations would misbehave in
     * ways that are tedious to diagnose from a screenshot, so it fails loudly
     * at construction instead.</p>
     */
    private void assertStageOrder() {
        int previous = -1;
        for (NametagEffect effect : this.effects) {
            int stage = effect.stage().ordinal();
            if (stage < previous) {
                throw new IllegalStateException(
                        "Effect " + effect.id() + " (" + effect.stage()
                                + ") is declared after a later stage; the pipeline list must be"
                                + " in EffectStage order");
            }
            previous = stage;
        }
    }

    public List<NametagEffect> effects() {
        return this.effects;
    }

    /** True when at least one effect is switched on. */
    public boolean anyEnabled() {
        for (int i = 0; i < this.effects.size(); i++) {
            if (this.effects.get(i).enabled()) {
                return true;
            }
        }
        return false;
    }

    /** True when an enabled effect moves, scales or skews glyphs. */
    public boolean anyGeometricEnabled() {
        for (int i = 0; i < this.effects.size(); i++) {
            NametagEffect effect = this.effects.get(i);
            if (effect.enabled() && effect.geometric()) {
                return true;
            }
        }
        return false;
    }

    /** True when an enabled effect changes colour or alpha. */
    public boolean anyColorEnabled() {
        for (int i = 0; i < this.effects.size(); i++) {
            NametagEffect effect = this.effects.get(i);
            if (effect.enabled() && !effect.geometric()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluates every enabled effect for one glyph.
     *
     * <p>Returns the shared context, valid until the next call. Callers read
     * what they need and draw; they must not retain it.</p>
     */
    public NametagRenderContext evaluate(float timeSeconds, int charIndex, int charCount,
                                         char character, int baseColor) {
        this.context.begin(timeSeconds, charIndex, charCount, character, baseColor);

        List<NametagEffect> all = this.effects;
        for (int i = 0; i < all.size(); i++) {
            NametagEffect effect = all.get(i);
            if (effect.enabled()) {
                effect.apply(this.context);
            }
        }
        return this.context;
    }

    /** Every effect's settings, flattened, for the designer and for config. */
    public List<Setting<?>> allSettings() {
        List<Setting<?>> result = new ArrayList<>();
        for (NametagEffect effect : this.effects) {
            result.addAll(effect.settings());
        }
        return result;
    }

    /** Switches every effect off and restores their settings to defaults. */
    public void reset() {
        for (NametagEffect effect : this.effects) {
            effect.reset();
        }
    }

    /** Switches every effect off, leaving their tuning alone. */
    public void disableAll() {
        for (NametagEffect effect : this.effects) {
            effect.setEnabled(false);
        }
    }

    public int enabledCount() {
        int count = 0;
        for (NametagEffect effect : this.effects) {
            if (effect.enabled()) {
                count++;
            }
        }
        return count;
    }
}

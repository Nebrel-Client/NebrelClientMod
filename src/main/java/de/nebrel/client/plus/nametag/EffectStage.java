package de.nebrel.client.plus.nametag;

/**
 * The order effects run in.
 *
 * <p>Stages exist so effects compose predictably rather than by registration
 * accident. Rainbow decides a colour, blinking then fades that colour rather
 * than fading white; waving moves the glyph, growing scales it, and both happen
 * after the colour is settled so neither can be undone by a later pass.</p>
 *
 * <p>Within a stage, effects apply in declaration order and each builds on the
 * previous one's result, so two colour effects blend rather than fight.</p>
 */
public enum EffectStage {

    /** Chooses the glyph's colour. */
    COLOR,

    /** Adjusts transparency, on top of whatever colour was chosen. */
    ALPHA,

    /** Moves the glyph. */
    POSITION,

    /** Scales or skews the glyph. */
    TRANSFORM
}

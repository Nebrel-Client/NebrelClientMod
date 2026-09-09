package de.nebrel.client.plus.nametag;

import de.nebrel.client.util.NebrelMath;

/**
 * The per-glyph state an effect reads and writes.
 *
 * <p>One instance is reused for every glyph of every nametag, reset by
 * {@link #begin}. Nametags are drawn for every visible player every frame, so
 * allocating a context per character would produce garbage in the thousands per
 * second for no benefit.</p>
 *
 * <p>Deliberately free of any Minecraft type: the whole effect pipeline is
 * plain maths, which is what lets it be tested without the game.</p>
 */
public final class NametagRenderContext {

    // -- inputs, set by begin() ----------------------------------------------

    private float timeSeconds;
    private int charIndex;
    private int charCount;
    private char character;
    private int baseColor;

    // -- outputs, mutated by effects -----------------------------------------

    private int color;
    private float alpha;
    private float offsetX;
    private float offsetY;
    private float scale;
    private float skew;

    /**
     * Resets the context for one glyph.
     *
     * @param timeSeconds a monotonic animation clock; the same value for every
     *                    glyph of a frame, so effects stay in phase
     * @param charIndex   position of this glyph within the text
     * @param charCount   total glyphs, so effects can spread across the word
     * @param character   the glyph itself
     * @param baseColor   packed ARGB before any effect runs
     */
    public void begin(float timeSeconds, int charIndex, int charCount, char character,
                      int baseColor) {
        this.timeSeconds = timeSeconds;
        this.charIndex = charIndex;
        this.charCount = Math.max(1, charCount);
        this.character = character;
        this.baseColor = baseColor;

        this.color = baseColor;
        this.alpha = 1.0F;
        this.offsetX = 0.0F;
        this.offsetY = 0.0F;
        this.scale = 1.0F;
        this.skew = 0.0F;
    }

    // -- inputs ---------------------------------------------------------------

    public float time() {
        return this.timeSeconds;
    }

    public int charIndex() {
        return this.charIndex;
    }

    public int charCount() {
        return this.charCount;
    }

    public char character() {
        return this.character;
    }

    public int baseColor() {
        return this.baseColor;
    }

    /**
     * This glyph's position along the text, 0.0 at the first and 1.0 at the last.
     *
     * <p>What effects use to spread themselves across a word rather than
     * applying identically to every character.</p>
     */
    public float progress() {
        return this.charCount <= 1 ? 0.0F : this.charIndex / (float) (this.charCount - 1);
    }

    // -- outputs --------------------------------------------------------------

    public int color() {
        return this.color;
    }

    public void setColor(int value) {
        this.color = value;
    }

    public float alpha() {
        return this.alpha;
    }

    /** Multiplies the current alpha, so two alpha effects combine. */
    public void multiplyAlpha(float factor) {
        this.alpha = NebrelMath.clamp(this.alpha * factor, 0.0F, 1.0F);
    }

    public float offsetX() {
        return this.offsetX;
    }

    public float offsetY() {
        return this.offsetY;
    }

    /** Adds to the offset, so two position effects sum rather than overwrite. */
    public void addOffset(float x, float y) {
        this.offsetX += x;
        this.offsetY += y;
    }

    public float scale() {
        return this.scale;
    }

    /** Multiplies the scale, so two transform effects compound. */
    public void multiplyScale(float factor) {
        this.scale *= factor;
    }

    public float skew() {
        return this.skew;
    }

    public void addSkew(float amount) {
        this.skew += amount;
    }

    /** True when nothing moved, scaled or skewed this glyph. */
    public boolean geometryUntouched() {
        return this.offsetX == 0.0F && this.offsetY == 0.0F
                && this.scale == 1.0F && this.skew == 0.0F;
    }
}

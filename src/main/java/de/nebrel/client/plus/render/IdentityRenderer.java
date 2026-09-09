package de.nebrel.client.plus.render;

import de.nebrel.client.plus.PlusSettings;
import de.nebrel.client.plus.badge.BadgeService;
import de.nebrel.client.plus.badge.NebrelBadge;
import de.nebrel.client.plus.nametag.NametagEffectPipeline;
import de.nebrel.client.plus.nametag.NametagRenderContext;
import de.nebrel.client.util.ColorUtil;

/**
 * Draws a badge and a styled name onto any {@link GlyphSink}.
 *
 * <p>The one place badge and name composition lives. The world nametag, the tab
 * list, the chat and the designer preview all come through here, which is what
 * makes the preview trustworthy: it is not a mock-up of the nametag, it is the
 * same code drawing to a different surface.</p>
 */
public final class IdentityRenderer {

    /** Padding inside the badge plate, per side. */
    private static final float BADGE_PADDING_X = 2.5F;

    private final PlusSettings settings;
    private final BadgeService badges;

    public IdentityRenderer(PlusSettings settings, BadgeService badges) {
        this.settings = settings;
        this.badges = badges;
    }

    // -- measurement ---------------------------------------------------------

    /** Total width of the badge including its trailing gap, or 0 when absent. */
    public float badgeWidth(GlyphSink sink, NebrelBadge badge) {
        if (badge == null || !this.settings.badgeEnabled.get()) {
            return 0.0F;
        }
        float scale = this.settings.badgeScale.getFloat();
        float glyph = sink.width(badge.glyph()) * scale;
        float plate = plated() ? BADGE_PADDING_X * 2.0F : 0.0F;
        return glyph + plate + this.settings.badgeGap.getFloat();
    }

    /** Width of badge plus name, as it will be drawn. */
    public float totalWidth(GlyphSink sink, NebrelBadge badge, String name) {
        return badgeWidth(sink, badge) + sink.width(name);
    }

    private boolean plated() {
        PlusSettings.BadgeStyle style = this.settings.badgeStyle.get();
        return style == PlusSettings.BadgeStyle.PLATE
                || style == PlusSettings.BadgeStyle.OUTLINE;
    }

    // -- drawing -------------------------------------------------------------

    /**
     * Draws the badge.
     *
     * @param accent       the client accent, for the accent colour mode
     * @param nametagColor the name's first resolved colour, for "match nametag"
     * @param pipeline     effects, applied to the badge only when the user asked
     * @param time         animation clock in seconds
     * @return the width consumed, including the trailing gap
     */
    public float drawBadge(GlyphSink sink, NebrelBadge badge, float x, float y,
                           int accent, int nametagColor,
                           NametagEffectPipeline pipeline, float time) {
        if (badge == null || !this.settings.badgeEnabled.get()) {
            return 0.0F;
        }

        int color = this.badges.resolveColor(badge, accent, nametagColor);

        // Colour effects may run over the badge, but only when the user opted
        // in, and position effects never do: a badge that jitters stops being a
        // recognisable marker, which is the whole point of having one.
        if (this.settings.animateBadge.get() && pipeline != null && pipeline.anyColorEnabled()) {
            NametagRenderContext context =
                    pipeline.evaluate(time, 0, 1, badge.glyph().charAt(0), color);
            color = ColorUtil.fadeAlpha(context.color(), context.alpha());
        }

        String glyph = badge.glyph();
        PlusSettings.BadgeStyle style = this.settings.badgeStyle.get();
        float scale = this.settings.badgeScale.getFloat();
        float glyphWidth = sink.width(glyph) * scale;
        float height = sink.lineHeight();

        float plateWidth = glyphWidth + (plated() ? BADGE_PADDING_X * 2.0F : 0.0F);
        float glyphX = x + (plated() ? BADGE_PADDING_X : 0.0F);

        switch (style) {
            case PLATE -> {
                int plate = ColorUtil.withAlpha(color, 56);
                if (sink.supportsPlates()) {
                    sink.plate(x, y - 1.0F, plateWidth, height + 1.0F, 2.5F, plate);
                    sink.glyph(glyph, glyphX, y, scale, 0.0F, color, 0);
                } else {
                    // No geometry available: the text background stands in.
                    sink.glyph(glyph, glyphX, y, scale, 0.0F, color, plate);
                }
            }
            case OUTLINE -> {
                if (sink.supportsPlates()) {
                    sink.plateOutline(x, y - 1.0F, plateWidth, height + 1.0F, 2.5F,
                            ColorUtil.withAlpha(color, 150));
                    sink.glyph(glyph, glyphX, y, scale, 0.0F, color, 0);
                } else {
                    // World space cannot outline; fall back to a faint plate
                    // rather than silently dropping the style.
                    sink.glyph(glyph, glyphX, y, scale, 0.0F, color,
                            ColorUtil.withAlpha(color, 40));
                }
            }
            case BRACKET -> sink.glyph("[" + glyph + "]", x, y, scale, 0.0F, color, 0);
            case PLAIN -> sink.glyph(glyph, x, y, scale, 0.0F, color, 0);
        }

        float consumed = style == PlusSettings.BadgeStyle.BRACKET
                ? sink.width("[" + glyph + "]") * scale
                : plateWidth;
        return consumed + this.settings.badgeGap.getFloat();
    }

    /**
     * Draws text with the effect pipeline applied glyph by glyph.
     *
     * <p>When no effect is on this falls through to a single string draw, so an
     * unstyled name costs exactly what it did before Nebrel+ existed.</p>
     *
     * @return the width consumed
     */
    public float drawStyledText(GlyphSink sink, String text, float x, float y,
                                int baseColor, NametagEffectPipeline pipeline, float time) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        if (pipeline == null || !pipeline.anyEnabled()) {
            sink.glyph(text, x, y, 1.0F, 0.0F, baseColor, 0);
            return sink.width(text);
        }

        int count = text.length();
        float cursor = x;

        for (int i = 0; i < count; i++) {
            char character = text.charAt(i);
            String glyph = String.valueOf(character);

            NametagRenderContext context = pipeline.evaluate(time, i, count, character, baseColor);
            int color = ColorUtil.fadeAlpha(context.color(), context.alpha());

            sink.glyph(glyph,
                    cursor + context.offsetX(),
                    y + context.offsetY(),
                    context.scale(),
                    context.skew(),
                    color,
                    0);

            // Advance by the untransformed width so scaling a glyph does not
            // push its neighbours around; the pulse happens in place.
            cursor += sink.width(glyph);
        }
        return cursor - x;
    }

    /**
     * The colour the first glyph of the name resolves to.
     *
     * <p>What the "match nametag" badge mode follows. Evaluated separately so
     * the badge can be drawn before the name without drawing it twice.</p>
     */
    public int resolveFirstNameColor(String name, int baseColor,
                                     NametagEffectPipeline pipeline, float time) {
        if (name == null || name.isEmpty() || pipeline == null || !pipeline.anyColorEnabled()) {
            return baseColor;
        }
        NametagRenderContext context =
                pipeline.evaluate(time, 0, name.length(), name.charAt(0), baseColor);
        return ColorUtil.fadeAlpha(context.color(), context.alpha());
    }
}

package de.nebrel.client.plus.badge;

import de.nebrel.client.plus.NebrelEntitlement;

/**
 * A marker shown in front of a player's name.
 *
 * <p>Nebrel+ grants exactly one badge today, the N. The type is a full model
 * rather than a boolean because badges are the kind of thing that multiply —
 * staff, founder, partner — and a second one should not require rewriting the
 * renderers.</p>
 *
 * <p>{@code priority} orders badges when a player holds several: lower sorts
 * first, so it reads as "rank". {@link BadgeService} decides whether to show
 * one or all of them.</p>
 *
 * @param id          stable identifier, used in config
 * @param displayName human readable name
 * @param glyph       the character drawn in the badge; "N" for Nebrel+
 * @param color       packed ARGB, or 0 to use the client accent
 * @param priority    sort order; lower comes first
 * @param entitlement the entitlement that grants this badge
 */
public record NebrelBadge(
        String id,
        String displayName,
        String glyph,
        int color,
        int priority,
        NebrelEntitlement entitlement) {

    /** Sentinel colour meaning "follow the client accent". */
    public static final int ACCENT_COLOR = 0;

    /** The Nebrel+ badge. */
    public static final NebrelBadge NEBREL_PLUS = new NebrelBadge(
            "nebrel_plus",
            "Nebrel+",
            "N",
            ACCENT_COLOR,
            100,
            NebrelEntitlement.NEBREL_PLUS_BADGE);

    /** True when this badge follows the client accent rather than a fixed colour. */
    public boolean followsAccent() {
        return this.color == ACCENT_COLOR;
    }

    /** The colour to draw with, given the current accent. */
    public int resolveColor(int accent) {
        return followsAccent() ? accent : this.color;
    }
}

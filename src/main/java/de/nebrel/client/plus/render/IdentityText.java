package de.nebrel.client.plus.render;

import de.nebrel.client.plus.PlusSettings;
import de.nebrel.client.plus.badge.BadgeService;
import de.nebrel.client.plus.badge.NebrelBadge;
import de.nebrel.client.plus.nametag.NametagEffectPipeline;
import de.nebrel.client.plus.nametag.NametagProfile;
import de.nebrel.client.plus.nametag.NametagRenderContext;
import de.nebrel.client.plus.profile.NebrelPlayerProfile;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

/**
 * Builds badges and styled names as text components.
 *
 * <p>The tab list and the chat are not surfaces this client draws; the game
 * renders a {@code Text} for both. So rather than taking over their rendering,
 * this produces the component they were going to draw anyway, with a badge
 * sibling in front and per-character colours where they are wanted.</p>
 *
 * <p>That has a real consequence worth being straight about: a component can
 * carry a colour but not a position. <b>Rainbow, chromatic and blinking work in
 * the tab list and chat; shaking, waving, skewing and growing do not</b>, because
 * there is nowhere in a text component to put an offset. They still work above
 * the player, where this client does own the drawing. The designer's Tab and
 * Chat previews use this same code, so what the preview shows is what those
 * surfaces will actually look like.</p>
 *
 * <p>Original components are always kept as intact children. Nothing here
 * concatenates strings, so click and hover events, message signatures and
 * server formatting survive untouched.</p>
 */
public final class IdentityText {

    private final PlusSettings settings;
    private final BadgeService badges;

    public IdentityText(PlusSettings settings, BadgeService badges) {
        this.settings = settings;
        this.badges = badges;
    }

    // -- badge ---------------------------------------------------------------

    /**
     * The badge as its own component, including its trailing space.
     *
     * @return null when there is no badge to show
     */
    public Text badgeComponent(NebrelBadge badge, int accent, int nametagColor) {
        if (badge == null || !this.settings.badgeEnabled.get()) {
            return null;
        }
        int color = this.badges.resolveColor(badge, accent, nametagColor);
        String glyph = switch (this.settings.badgeStyle.get()) {
            case BRACKET -> "[" + badge.glyph() + "]";
            case PLATE, OUTLINE, PLAIN -> badge.glyph();
        };

        // A component cannot carry a plate, so the plated styles render as the
        // bare glyph here. The world nametag draws the real plate.
        return Text.literal(glyph)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF)))
                .append(Text.literal(" ").setStyle(Style.EMPTY));
    }

    // -- names ---------------------------------------------------------------

    /**
     * Colours a name character by character.
     *
     * <p>Only called when a colour effect is actually on. Building one component
     * per character is far more expensive than a single literal, and doing it
     * for an unstyled name would cost the tab list dearly for no visible
     * difference.</p>
     */
    public Text styledName(String name, NametagProfile profile, int baseColor, float time) {
        NametagEffectPipeline pipeline = profile.effects;
        if (name == null || name.isEmpty()) {
            return Text.empty();
        }
        if (!pipeline.anyColorEnabled()) {
            return Text.literal(name)
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(baseColor & 0xFFFFFF)));
        }

        MutableText result = Text.empty();
        int count = name.length();
        for (int i = 0; i < count; i++) {
            char character = name.charAt(i);
            NametagRenderContext context = pipeline.evaluate(time, i, count, character, baseColor);
            // Alpha is folded into the colour: text components have no alpha
            // channel, so a blink is expressed as a fade towards black.
            int rgb = ColorUtil.fadeAlpha(context.color(), context.alpha()) & 0xFFFFFF;
            result.append(Text.literal(String.valueOf(character))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return result;
    }

    /**
     * Prefixes a name with its badge, and restyles it when effects ask for it.
     *
     * <p>When no colour effect is on, {@code original} is appended unchanged, so
     * team colours, spectator italics and server formatting are all preserved.
     * Only an active colour effect replaces it, and that is the user explicitly
     * asking for their name to look different.</p>
     *
     * @param original the component the game was going to draw
     * @return a new component, or {@code original} when nothing applies
     */
    public Text decorate(NebrelPlayerProfile profile, Text original, int accent, float time) {
        if (profile == null || original == null || profile.isPlain()) {
            return original;
        }

        NametagProfile nametag = profile.hasNametagStyle() ? profile.nametag() : null;
        int baseColor = 0xFFFFFFFF;
        int firstColor = baseColor;

        boolean restyle = nametag != null && nametag.effects.anyColorEnabled();
        if (nametag != null) {
            baseColor = nametag.resolveBaseColor(0xFFFFFFFF);
            firstColor = restyle
                    ? firstStyledColor(original.getString(), nametag, baseColor, time)
                    : baseColor;
        }

        Text badge = badgeComponent(profile.primaryBadge(), accent, firstColor);
        if (badge == null && !restyle) {
            return original;
        }

        MutableText result = Text.empty();
        if (badge != null) {
            result.append(badge);
        }
        if (restyle) {
            result.append(styledName(original.getString(), nametag, baseColor, time));
        } else {
            // Keep the original component tree intact.
            result.append(original);
        }
        return result;
    }

    private int firstStyledColor(String name, NametagProfile profile, int baseColor, float time) {
        if (name.isEmpty()) {
            return baseColor;
        }
        NametagRenderContext context =
                profile.effects.evaluate(time, 0, name.length(), name.charAt(0), baseColor);
        return ColorUtil.fadeAlpha(context.color(), context.alpha());
    }
}

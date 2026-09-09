package de.nebrel.client.plus.render;

/**
 * The frame around a world nametag: plate, size, opacity and extra readouts.
 *
 * <p>Separate from Nebrel+ because the Custom Nametags module already owned all
 * of it. Rather than Nebrel+ growing a second nametag renderer beside that one —
 * two things fighting over the same label, with whichever ran last winning —
 * the module now describes how the plate should look and
 * {@link NametagCoordinator} does the drawing for both.</p>
 *
 * @param active         whether the module wants to restyle labels at all
 * @param background     draw a plate behind the text
 * @param backgroundColor plate colour
 * @param scale          size multiplier
 * @param opacity        overall transparency, 0-1
 * @param textShadow     draw the text with a shadow
 * @param maxDistance    beyond this many blocks the module leaves labels alone
 * @param playersOnly    skip labels on non-player entities
 * @param suffix         text appended to the name, such as health or distance
 */
public record WorldNametagStyle(
        boolean active,
        boolean background,
        int backgroundColor,
        float scale,
        float opacity,
        boolean textShadow,
        double maxDistance,
        boolean playersOnly,
        String suffix) {

    /** What Nebrel+ uses when the Custom Nametags module is switched off. */
    public static final WorldNametagStyle VANILLA_LIKE = new WorldNametagStyle(
            false, true, 0x40000000, 1.0F, 1.0F, false, 64.0D, false, "");

    /** A copy carrying a different suffix. */
    public WorldNametagStyle withSuffix(String newSuffix) {
        return new WorldNametagStyle(this.active, this.background, this.backgroundColor,
                this.scale, this.opacity, this.textShadow, this.maxDistance, this.playersOnly,
                newSuffix == null ? "" : newSuffix);
    }
}

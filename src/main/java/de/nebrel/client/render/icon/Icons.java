package de.nebrel.client.render.icon;

import java.util.Map;

/**
 * Every hand-drawn icon the client ships, all 9×9.
 *
 * <p>Original shapes, drawn for this client — no icon font, no imported icon
 * pack, no image traced from another client's UI. The sidebar and the
 * highest-traffic module cards use these; a module without an entry here
 * keeps its existing single-glyph icon (see {@link #forModuleId}), which is
 * a real fallback, not a placeholder — most of the 32 modules are still on
 * it and will stay there.</p>
 */
public final class Icons {

    private Icons() {
    }

    // -- navigation ------------------------------------------------------

    public static final PixelIcon ALL = new PixelIcon("all",
            ".........",
            ".###.###.",
            ".###.###.",
            ".###.###.",
            ".........",
            ".###.###.",
            ".###.###.",
            ".###.###.",
            ".........");

    public static final PixelIcon FAVORITES = new PixelIcon("favorites",
            "....#....",
            "...###...",
            "..#####..",
            "#########",
            ".#######.",
            "..#####..",
            ".##...##.",
            "##.....##",
            ".........");

    public static final PixelIcon HUD = new PixelIcon("hud",
            "##.....##",
            "#.......#",
            ".........",
            "...###...",
            "...###...",
            "...###...",
            ".........",
            "#.......#",
            "##.....##");

    public static final PixelIcon VISUAL = new PixelIcon("visual",
            ".........",
            "..#####..",
            ".##...##.",
            "#..###..#",
            "#..###..#",
            "#..###..#",
            ".##...##.",
            "..#####..",
            ".........");

    public static final PixelIcon PLAYER = new PixelIcon("player",
            ".........",
            "..#####..",
            ".#######.",
            ".#######.",
            "..#####..",
            ".........",
            "#.......#",
            "##.....##",
            "#########");

    public static final PixelIcon WORLD = new PixelIcon("world",
            "..#####..",
            ".##.#.##.",
            "####.####",
            "####.####",
            "#.......#",
            "####.####",
            "####.####",
            ".##.#.##.",
            "..#####..");

    public static final PixelIcon RENDER = new PixelIcon("render",
            "....#....",
            "...###...",
            "..#####..",
            ".#######.",
            ".........",
            "..#####..",
            ".#######.",
            ".........",
            ".........");

    public static final PixelIcon UTILITY = new PixelIcon("utility",
            ".###.....",
            "#########",
            ".###.....",
            "....###..",
            "#########",
            "....###..",
            "..###....",
            "#########",
            "..###....");

    public static final PixelIcon SETTINGS = new PixelIcon("settings",
            "....#....",
            ".#..#..#.",
            "..#####..",
            "..#####..",
            "#########",
            "..#####..",
            "..#####..",
            ".#..#..#.",
            "....#....");

    // -- module cards ------------------------------------------------------

    public static final PixelIcon TAG = new PixelIcon("tag",
            ".........",
            "...######",
            "..#######",
            ".########",
            "#########",
            ".########",
            "..#######",
            "...######",
            ".........");

    public static final PixelIcon SUN = new PixelIcon("sun",
            "....#....",
            "#.......#",
            ".........",
            "...###...",
            "#..###..#",
            "...###...",
            ".........",
            "#.......#",
            "....#....");

    public static final PixelIcon CHAT = new PixelIcon("chat",
            "#########",
            "#.......#",
            "#.......#",
            "#.......#",
            "#########",
            "..##.....",
            "...#.....",
            ".........",
            ".........");

    public static final PixelIcon BARS = new PixelIcon("bars",
            ".........",
            "......##.",
            "......##.",
            "......##.",
            "...##.##.",
            "...##.##.",
            "##.##.##.",
            "##.##.##.",
            "##.##.##.");

    public static final PixelIcon BOUNDS = new PixelIcon("bounds",
            "#########",
            "#.......#",
            "#.......#",
            "#.......#",
            "#.......#",
            "#.......#",
            "#.......#",
            "#.......#",
            "#########");

    public static final PixelIcon WASD = new PixelIcon("wasd",
            "...###...",
            "...###...",
            "...###...",
            ".........",
            "##.###.##",
            "##.###.##",
            "##.###.##",
            ".........",
            ".........");

    /** Every icon, keyed by {@link PixelIcon#id()}, for the self-test to walk. */
    public static final Map<String, PixelIcon> ALL_ICONS = Map.ofEntries(
            Map.entry(ALL.id(), ALL),
            Map.entry(FAVORITES.id(), FAVORITES),
            Map.entry(HUD.id(), HUD),
            Map.entry(VISUAL.id(), VISUAL),
            Map.entry(PLAYER.id(), PLAYER),
            Map.entry(WORLD.id(), WORLD),
            Map.entry(RENDER.id(), RENDER),
            Map.entry(UTILITY.id(), UTILITY),
            Map.entry(SETTINGS.id(), SETTINGS),
            Map.entry(TAG.id(), TAG),
            Map.entry(SUN.id(), SUN),
            Map.entry(CHAT.id(), CHAT),
            Map.entry(BARS.id(), BARS),
            Map.entry(BOUNDS.id(), BOUNDS),
            Map.entry(WASD.id(), WASD));

    /**
     * The bespoke icon for a module, or {@code null}.
     *
     * <p>Only the modules this shipped with a hand-drawn icon are listed; a
     * {@code null} here is the normal case, and the caller falls back to
     * {@link de.nebrel.client.module.Module#icon()}'s single glyph, exactly
     * as it always has.</p>
     */
    public static PixelIcon forModuleId(String moduleId) {
        return switch (moduleId) {
            case "nebrel_hud" -> HUD;
            case "custom_nametags" -> TAG;
            case "fullbright" -> SUN;
            case "freelook" -> VISUAL;
            case "auto_text" -> CHAT;
            case "tiers" -> BARS;
            case "hitbox" -> BOUNDS;
            case "keystrokes" -> WASD;
            default -> null;
        };
    }
}

package de.nebrel.client.render.icon;

import java.util.Map;

/**
 * Every hand-drawn icon the client ships, all 16×16.
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

    /**
     * Four outlined tiles in a 2x2 grid: "all your modules", drawn the same
     * monoline way as every other navigation icon rather than the two filled
     * blobs this used to be.
     */
    public static final PixelIcon ALL = new PixelIcon("all",
            "................",
            "................",
            "..#####..#####..",
            "..#...#..#...#..",
            "..#...#..#...#..",
            "..#...#..#...#..",
            "..#####..#####..",
            "................",
            "................",
            "..#####..#####..",
            "..#...#..#...#..",
            "..#...#..#...#..",
            "..#...#..#...#..",
            "..#####..#####..",
            "................",
            "................");

    public static final PixelIcon FAVORITES = new PixelIcon("favorites",
            "................",
            "................",
            ".......##.......",
            ".......##.......",
            ".......##.......",
            "......####......",
            "..############..",
            "...##########...",
            ".....######.....",
            ".....######.....",
            ".....######.....",
            "....###..###....",
            "....##....##....",
            "................",
            "................",
            "................");

    public static final PixelIcon HUD = new PixelIcon("hud",
            "................",
            ".#####....#####.",
            ".#............#.",
            ".#............#.",
            ".#............#.",
            ".#............#.",
            "......####......",
            "......####......",
            "......####......",
            "......####......",
            ".#............#.",
            ".#............#.",
            ".#............#.",
            ".#............#.",
            ".#####....#####.",
            "................");

    public static final PixelIcon VISUAL = new PixelIcon("visual",
            "................",
            "................",
            "................",
            "................",
            "....########....",
            "..####....####..",
            ".##...####...##.",
            ".##...####...##.",
            ".##...####...##.",
            ".##...####...##.",
            "..####....####..",
            "....########....",
            "................",
            "................",
            "................",
            "................");

    public static final PixelIcon PLAYER = new PixelIcon("player",
            "................",
            "................",
            "......####......",
            ".....######.....",
            ".....######.....",
            "....########....",
            ".....######.....",
            ".....######.....",
            "......####......",
            "................",
            "....########....",
            "...#########....",
            "...##########...",
            "...##########...",
            "..###########...",
            "..############..");

    public static final PixelIcon WORLD = new PixelIcon("world",
            "................",
            ".....##.###.....",
            "....###.####....",
            "...####.#####...",
            "..#####.######..",
            ".######.#######.",
            ".######.#######.",
            ".######.#######.",
            ".######.#######.",
            ".######.#######.",
            ".######.#######.",
            "..#####.######..",
            "...####.#####...",
            "....###.####....",
            ".....##.###.....",
            "................");

    public static final PixelIcon RENDER = new PixelIcon("render",
            "................",
            ".......##.......",
            "......####......",
            ".....######.....",
            "....########....",
            "..############..",
            ".##############.",
            "...##########...",
            "....#######.....",
            "......####......",
            "................",
            "................",
            "..############..",
            "..############..",
            "..###########...",
            "................");

    public static final PixelIcon UTILITY = new PixelIcon("utility",
            "................",
            "....##..........",
            "...####.........",
            ".##############.",
            "...####.........",
            "....##....##....",
            ".........####...",
            ".##############.",
            ".........####...",
            "......##..##....",
            ".....####.......",
            ".##############.",
            ".....####.......",
            "......##........",
            "................",
            "................");

    /**
     * The Nebrel+ brand mark: a slanted monoline "N", not a NoRisk-style bolt
     * and not the plain unicode star the sidebar used to draw for this row.
     */
    public static final PixelIcon NEBREL = new PixelIcon("nebrel",
            "................",
            "................",
            "..####......##..",
            "..####......##..",
            "..##.##.....##..",
            "..##.##.....##..",
            "..##..##....##..",
            "..##...##...##..",
            "..##...##...##..",
            "..##....##..##..",
            "..##.....##.##..",
            "..##.....##.##..",
            "..##......####..",
            "..##......####..",
            "................",
            "................");

    /** A hollow reticle with a small centre dot, not a detailed target. */
    public static final PixelIcon CROSSHAIR = new PixelIcon("crosshair",
            "................",
            "................",
            ".......##.......",
            ".......##.......",
            ".......##.......",
            ".......##.......",
            "................",
            "..####.##.####..",
            "..####.##.####..",
            "................",
            ".......##.......",
            ".......##.......",
            ".......##.......",
            ".......##.......",
            "................",
            "................");

    /** A left-pointing chevron, used everywhere a view has a "back" action. */
    public static final PixelIcon BACK = new PixelIcon("back",
            "................",
            "................",
            "................",
            "..........##....",
            "........##......",
            ".......##.......",
            "......##........",
            ".....##.........",
            ".....##.........",
            "......##........",
            ".......##.......",
            "........##......",
            "..........##....",
            "................",
            "................",
            "................");

    /**
     * Two heads over a shared shoulder bar. Prepared for a future friends list;
     * nothing in the menu links to it yet, so it is registered but not wired
     * into any real navigation row (there is no friends feature to point at).
     */
    public static final PixelIcon FRIENDS = new PixelIcon("friends",
            "................",
            "................",
            "....##....##....",
            "...#..#..#..#...",
            "...#..#..#..#...",
            "....##....##....",
            "................",
            "................",
            "..############..",
            "..#..........#..",
            "..#..........#..",
            "..#..........#..",
            "..#..........#..",
            "..############..",
            "................",
            "................");

    public static final PixelIcon SETTINGS = new PixelIcon("settings",
            ".......##.......",
            "......####......",
            "..###..##..###..",
            "..###.####.###..",
            "..############..",
            "....###..###....",
            ".#.###....###.#.",
            "#####......#####",
            "#####......#####",
            ".#.###....###.#.",
            "....###..###....",
            "..############..",
            "..###.####.###..",
            "..###..##..###..",
            "......####......",
            ".......##.......");

    // -- module cards ------------------------------------------------------

    public static final PixelIcon TAG = new PixelIcon("tag",
            "................",
            "......#########.",
            ".....##########.",
            "....######..###.",
            "....######..###.",
            "...#######..###.",
            "...############.",
            "..#############.",
            ".##############.",
            ".#############..",
            "..###########...",
            "...#########....",
            "....######......",
            ".....####.......",
            "......##........",
            "................");

    /** A hollow ring rather than a filled disc, to match the outline icons around it. */
    public static final PixelIcon SUN = new PixelIcon("sun",
            ".......#........",
            ".......#........",
            "..#....#.....#..",
            "...#........#...",
            "....#..##..#....",
            ".....#....#.....",
            ".....#....#.....",
            "....#......#....",
            "###.#......#.###",
            ".....#....#.....",
            ".....#....#.....",
            "....#..##..#....",
            "...#........#...",
            "..#.....#....#..",
            "........#.......",
            "........#.......");

    public static final PixelIcon CHAT = new PixelIcon("chat",
            "................",
            ".#############..",
            ".#............#.",
            ".#............#.",
            ".#............#.",
            ".#............#.",
            ".#............#.",
            ".#............#.",
            ".#............#.",
            ".#..............",
            "..###########...",
            "...###..........",
            "...##...........",
            "...#............",
            "................",
            "................");

    public static final PixelIcon BARS = new PixelIcon("bars",
            "................",
            "................",
            "...........####.",
            "...........####.",
            "...........####.",
            "...........####.",
            "......####.####.",
            "......####.####.",
            "......####.####.",
            "......####.####.",
            ".####.####.####.",
            ".####.####.####.",
            ".####.####.####.",
            ".####.####.####.",
            ".###..###..###..",
            "................");

    public static final PixelIcon BOUNDS = new PixelIcon("bounds",
            "................",
            ".#############..",
            ".#############..",
            ".##..........##.",
            ".##..........##.",
            ".##..........##.",
            ".##..........##.",
            ".##..........##.",
            ".##..........##.",
            ".##..........##.",
            ".##..........##.",
            ".##..........##.",
            ".##..........#..",
            ".#############..",
            "...#########....",
            "................");

    /** Three horizontal haze layers crossed out, for "No Fog". */
    public static final PixelIcon NO_FOG = new PixelIcon("no_fog",
            "................",
            "................",
            "..##............",
            "...##...........",
            "....##..........",
            "...##########...",
            "......##........",
            ".......##.......",
            "..############..",
            ".........##.....",
            "..........##....",
            "...##########...",
            "............##..",
            ".............##.",
            "................",
            "................");

    /** A small filled heart with a readout bar beneath it. */
    public static final PixelIcon HEART_BAR = new PixelIcon("heart_bar",
            "................",
            "................",
            "................",
            "...####..####...",
            "..#####..#####..",
            "..############..",
            "..############..",
            "...##########...",
            "....########....",
            ".....######.....",
            "......####......",
            ".......##.......",
            "................",
            "....########....",
            "................",
            "................");

    /** The same heart with a small impact spark instead of a bar. */
    public static final PixelIcon HEART_FLASH = new PixelIcon("heart_flash",
            ".............#..",
            "............###.",
            ".............#..",
            "...####..####...",
            "..#####..#####..",
            "..############..",
            "..############..",
            "...##########...",
            "....########....",
            ".....######.....",
            "......####......",
            ".......##.......",
            "................",
            "................",
            "................",
            "................");

    /** A clipboard with a tab and three list lines, for Quests. */
    public static final PixelIcon CLIPBOARD = new PixelIcon("clipboard",
            "................",
            "................",
            ".....#####......",
            "...##########...",
            "...#........#...",
            "...#.#####..#...",
            "...#........#...",
            "...#.#####..#...",
            "...#........#...",
            "...#.#####..#...",
            "...#........#...",
            "...#........#...",
            "...##########...",
            "................",
            "................",
            "................");

    /** A right-pointing chevron; mirrors {@link #BACK}. */
    public static final PixelIcon FORWARD = new PixelIcon("forward",
            "................",
            "................",
            "................",
            "....##..........",
            "......##........",
            ".......##.......",
            "........##......",
            ".........##.....",
            ".........##.....",
            "........##......",
            ".......##.......",
            "......##........",
            "....##..........",
            "................",
            "................",
            "................");

    /** Three small rings cascading diagonally, for a soft/blurred surface. */
    public static final PixelIcon BLUR_RINGS = new PixelIcon("blur_rings",
            "................",
            "................",
            "...##...........",
            "..#..#..........",
            "..#..#..........",
            "...##.##........",
            ".....#..#.......",
            ".....#..#.......",
            "......##.##.....",
            "........#..#....",
            "........#..#....",
            ".........##.....",
            "................",
            "................",
            "................",
            "................");

    /** A diagonal pencil stroke with a point, for edit actions. */
    public static final PixelIcon EDIT = new PixelIcon("edit",
            "................",
            "................",
            "............##..",
            "...........##...",
            "..........##....",
            ".........##.....",
            "........##......",
            ".......##.......",
            "......##........",
            ".....##.........",
            "....##..........",
            "...##...........",
            "..##............",
            "..#.............",
            "................",
            "................");

    public static final PixelIcon WASD = new PixelIcon("wasd",
            "................",
            "......####......",
            "......####......",
            "......####......",
            "......####......",
            "......###.......",
            "................",
            "................",
            ".####.####.####.",
            ".####.####.####.",
            ".####.####.####.",
            ".####.####.####.",
            ".####.####.####.",
            ".###..###..###..",
            "................",
            "................");

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
            Map.entry(NEBREL.id(), NEBREL),
            Map.entry(CROSSHAIR.id(), CROSSHAIR),
            Map.entry(BACK.id(), BACK),
            Map.entry(FORWARD.id(), FORWARD),
            Map.entry(FRIENDS.id(), FRIENDS),
            Map.entry(NO_FOG.id(), NO_FOG),
            Map.entry(HEART_BAR.id(), HEART_BAR),
            Map.entry(HEART_FLASH.id(), HEART_FLASH),
            Map.entry(CLIPBOARD.id(), CLIPBOARD),
            Map.entry(BLUR_RINGS.id(), BLUR_RINGS),
            Map.entry(EDIT.id(), EDIT),
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
            case "crosshair" -> CROSSHAIR;
            case "no_fog" -> NO_FOG;
            case "health_indicators" -> HEART_BAR;
            case "damage_tint" -> HEART_FLASH;
            case "quests" -> CLIPBOARD;
            case "toggle_sprint" -> FORWARD;
            case "blur" -> BLUR_RINGS;
            default -> null;
        };
    }
}

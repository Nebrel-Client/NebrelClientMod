package de.nebrel.client.core;

/**
 * The narrow contract between modules and mixins.
 *
 * <p>Mixins run on Minecraft's hottest paths. Instead of having each one look
 * its module up in a registry and read a setting object every frame, modules
 * push their current values here whenever they change, and the mixins read
 * plain fields. That keeps the injected code to a field read and a branch.</p>
 *
 * <p>Every override uses a sentinel meaning "not active", and every module is
 * responsible for clearing its own entries in {@code onDisable} so nothing is
 * left applied after the module is switched off.</p>
 */
public final class NebrelState {

    private NebrelState() {
    }

    // -- camera --------------------------------------------------------------

    /** Field of view in degrees, or NaN to leave vanilla alone. */
    public static volatile double fovOverride = Double.NaN;

    /** True while free look is holding the camera still. */
    public static volatile boolean freeLookActive;
    public static volatile float freeLookYaw;
    public static volatile float freeLookPitch;

    // -- lighting ------------------------------------------------------------

    /** Gamma to force, or NaN for the user's own brightness setting. */
    public static volatile double gammaOverride = Double.NaN;

    // -- fog -----------------------------------------------------------------

    public static volatile boolean disableTerrainFog;
    public static volatile boolean disableWaterFog;
    public static volatile boolean disableLavaFog;
    public static volatile boolean disablePowderSnowFog;
    /** Multiplier applied to the fog end distance when fog is left on. */
    public static volatile float fogDistanceScale = 1.0F;

    // -- world ---------------------------------------------------------------

    /** Time of day in ticks to report, or -1 to use the server's. */
    public static volatile long timeOverride = -1L;

    /** Rain strength 0.0-1.0, or NaN to use the server's. */
    public static volatile float rainOverride = Float.NaN;
    /** Thunder strength 0.0-1.0, or NaN to use the server's. */
    public static volatile float thunderOverride = Float.NaN;

    // -- item rendering ------------------------------------------------------

    /** Enchantment glint tint, or 0 to leave the vanilla glint alone. */
    public static volatile int glintColor;
    /** Glint opacity multiplier, 0.0-1.0. */
    public static volatile float glintOpacity = 1.0F;

    /** First-person item transform. All zero and scale 1 means untouched. */
    public static volatile boolean itemModelActive;
    public static volatile float itemScale = 1.0F;
    public static volatile float itemOffsetX;
    public static volatile float itemOffsetY;
    public static volatile float itemOffsetZ;
    public static volatile float itemRotateX;
    public static volatile float itemRotateY;
    public static volatile float itemRotateZ;

    /** Hand swing duration in ticks, or -1 for the vanilla six. */
    public static volatile int swingDurationTicks = -1;

    // -- vanilla HUD ---------------------------------------------------------

    public static volatile boolean hideCrosshair;
    public static volatile boolean hideHotbar;
    public static volatile boolean hideHealth;
    public static volatile boolean hideHunger;
    public static volatile boolean hideArmor;
    public static volatile boolean hideExperience;
    public static volatile boolean hideBossBar;
    public static volatile boolean hideScoreboard;
    public static volatile boolean hideActionBar;
    public static volatile boolean hideStatusEffects;

    // -- particles -----------------------------------------------------------

    /** Extra copies spawned per particle; 1 means vanilla behaviour. */
    public static volatile int particleMultiplier = 1;
    /** Hard ceiling on live particles, regardless of the multiplier. */
    public static volatile int particleCap = 16_384;
    /** True while the Overflow Particles module is on. */
    public static volatile boolean particleBoostActive;
    /**
     * Running estimate of live particles.
     *
     * <p>Maintained by the module, which increments it as it spawns and decays
     * it each tick. The particle manager buckets particles per texture sheet,
     * so counting them exactly on every spawn would mean walking those buckets
     * on a very hot path; an estimate is enough to enforce a ceiling.</p>
     */
    public static volatile int particleLiveEstimate;

    /**
     * Clears every override.
     *
     * <p>Called when the client shuts down or leaves a world, so nothing
     * survives into the next session if a module failed to clean up.</p>
     */
    public static void resetAll() {
        fovOverride = Double.NaN;
        freeLookActive = false;
        gammaOverride = Double.NaN;
        disableTerrainFog = false;
        disableWaterFog = false;
        disableLavaFog = false;
        disablePowderSnowFog = false;
        fogDistanceScale = 1.0F;
        timeOverride = -1L;
        rainOverride = Float.NaN;
        thunderOverride = Float.NaN;
        glintColor = 0;
        glintOpacity = 1.0F;
        itemModelActive = false;
        itemScale = 1.0F;
        itemOffsetX = 0.0F;
        itemOffsetY = 0.0F;
        itemOffsetZ = 0.0F;
        itemRotateX = 0.0F;
        itemRotateY = 0.0F;
        itemRotateZ = 0.0F;
        swingDurationTicks = -1;
        hideCrosshair = false;
        hideHotbar = false;
        hideHealth = false;
        hideHunger = false;
        hideArmor = false;
        hideExperience = false;
        hideBossBar = false;
        hideScoreboard = false;
        hideActionBar = false;
        hideStatusEffects = false;
        particleMultiplier = 1;
        particleCap = 16_384;
        particleBoostActive = false;
        particleLiveEstimate = 0;
    }
}

package de.nebrel.client.plus.nametag;

import de.nebrel.client.plus.PlusSections;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * One animated treatment applied to a nametag, glyph by glyph.
 *
 * <p>Each effect is a small class with its own settings and one
 * {@link #apply} method. That is the point: the alternative, a single method
 * that branches on which effects are on, grows quadratically as effects are
 * added and makes combining them a matter of luck.</p>
 *
 * <p>Effects must be <b>deterministic functions of time</b>. Nothing may call a
 * random number generator per frame: the same glyph at the same instant has to
 * produce the same result, or the text shimmers instead of animating, and the
 * world nametag and the designer preview disagree.</p>
 */
public abstract class NametagEffect {

    private final String id;
    private final String displayName;
    private final String description;
    private final EffectStage stage;

    private final List<Setting<?>> settings = new ArrayList<>();
    private final BooleanSetting enabled;

    protected NametagEffect(String id, String displayName, String description, EffectStage stage) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.stage = stage;
        this.enabled = new BooleanSetting(key("enabled"), displayName, description, false);
        this.enabled.section(PlusSections.EFFECTS);
        this.settings.add(this.enabled);
    }

    protected final String key(String suffix) {
        return "plus.effect." + this.id + "." + suffix;
    }

    /** Registers a slider and makes it visible only while the effect is on. */
    protected final NumberSetting number(String suffix, String name, String description,
                                         double value, double min, double max, double step) {
        NumberSetting setting = new NumberSetting(key(suffix), name, description,
                value, min, max, step);
        setting.section(this.displayName);
        setting.visibleWhen(this.enabled);
        this.settings.add(setting);
        return setting;
    }

    /** Registers a switch and makes it visible only while the effect is on. */
    protected final BooleanSetting bool(String suffix, String name, String description,
                                        boolean value) {
        BooleanSetting setting = new BooleanSetting(key(suffix), name, description, value);
        setting.section(this.displayName);
        setting.visibleWhen(this.enabled);
        this.settings.add(setting);
        return setting;
    }

    public final String id() {
        return this.id;
    }

    public final String displayName() {
        return this.displayName;
    }

    public final String description() {
        return this.description;
    }

    public final EffectStage stage() {
        return this.stage;
    }

    public final BooleanSetting enabledSetting() {
        return this.enabled;
    }

    public final boolean enabled() {
        return this.enabled.get();
    }

    public final void setEnabled(boolean value) {
        this.enabled.set(value);
    }

    /** The effect's own switch plus its settings, for the designer and config. */
    public final List<Setting<?>> settings() {
        return List.copyOf(this.settings);
    }

    /** The settings excluding the switch, for laying out a compact section. */
    public final List<Setting<?>> tuningSettings() {
        List<Setting<?>> result = new ArrayList<>(this.settings.size() - 1);
        for (Setting<?> setting : this.settings) {
            if (setting != this.enabled) {
                result.add(setting);
            }
        }
        return result;
    }

    /** Restores this effect's settings, switch included, to their defaults. */
    public final void reset() {
        for (Setting<?> setting : this.settings) {
            setting.reset();
        }
    }

    /**
     * Whether this effect touches the glyph's geometry rather than its colour.
     *
     * <p>Surfaces that can only recolour text — the tablist and the chat, which
     * are text components rather than a custom draw — use this to say honestly
     * which effects they cannot show.</p>
     */
    public final boolean geometric() {
        return this.stage == EffectStage.POSITION || this.stage == EffectStage.TRANSFORM;
    }

    /**
     * Applies this effect to one glyph.
     *
     * <p>Must be a pure function of the context: same inputs, same outputs.</p>
     */
    public abstract void apply(NametagRenderContext context);

    /**
     * Smooth, repeatable pseudo-noise in the range -1 to 1.
     *
     * <p>Shared by effects that want to look irregular without being random.
     * Hashing the seed and interpolating between whole steps gives motion that
     * is unpredictable to the eye but identical every time it is evaluated,
     * which is what keeps a shaking nametag from strobing.</p>
     *
     * @param seed the whole-number step
     * @param salt distinguishes independent noise channels, e.g. x from y
     */
    protected static float noise(float seed, int salt) {
        int whole = (int) Math.floor(seed);
        float fraction = seed - whole;
        float from = hashToUnit(whole, salt);
        float to = hashToUnit(whole + 1, salt);
        // Smoothstep between the two, so the motion has no corners.
        float eased = fraction * fraction * (3.0F - 2.0F * fraction);
        return from + (to - from) * eased;
    }

    /** Deterministic hash of (value, salt) mapped onto -1 to 1. */
    private static float hashToUnit(int value, int salt) {
        int hash = value * 374_761_393 + salt * 668_265_263;
        hash = (hash ^ (hash >>> 13)) * 1_274_126_177;
        hash = hash ^ (hash >>> 16);
        return (hash & 0xFFFF) / 32_768.0F - 1.0F;
    }

    @Override
    public String toString() {
        return "NametagEffect[" + this.id + (enabled() ? ", on]" : ", off]");
    }
}

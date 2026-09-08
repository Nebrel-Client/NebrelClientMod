package de.nebrel.client.module.impl.visual;

import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.mixin.SimpleOptionAccessor;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Raises the game's brightness past the slider's own ceiling.
 *
 * <p>Writes the gamma option's backing field directly, because the option's
 * validator clamps to 0.0-1.0. That is the least invasive approach available:
 * no lightmap mixin and no render hook, so shaders, resource packs and every
 * other lighting mod keep working normally.</p>
 *
 * <p>The player's own brightness is captured on enable and written back on
 * disable, so switching the module off never leaves the game altered.</p>
 */
public final class FullBrightModule extends Module implements ClientTickHook {

    public final NumberSetting brightness;
    public final BooleanSetting smoothToggle;
    public final NumberSetting fadeSpeed;

    private Double savedGamma;
    private double appliedGamma;

    public FullBrightModule() {
        super("fullbright", "Full Bright", "Light up dark areas without a potion",
                ModuleCategory.VISUAL, "☀");

        this.brightness = number("brightness", "Brightness",
                "How far past the vanilla maximum to push the gamma",
                10.0D, 1.0D, 20.0D, 0.5D);

        this.smoothToggle = bool("smooth", "Smooth Toggle",
                "Fade the brightness in and out instead of snapping", true,
                SettingSection.ANIMATION);

        this.fadeSpeed = number("fadeSpeed", "Fade Speed", "How quickly the brightness changes",
                0.15D, 0.02D, 1.0D, 0.01D, SettingSection.ANIMATION);
        this.fadeSpeed.visibleWhen(this.smoothToggle);
    }

    @Override
    protected void onEnable() {
        SimpleOption<Double> gamma = MinecraftClient.getInstance().options.getGamma();
        // Remember the user's own setting so it can be restored exactly.
        this.savedGamma = gamma.getValue();
        this.appliedGamma = this.savedGamma;
    }

    @Override
    protected void onDisable() {
        if (this.savedGamma != null) {
            writeGamma(this.savedGamma);
            this.savedGamma = null;
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        double target = this.brightness.get();
        if (this.smoothToggle.get()) {
            this.appliedGamma = NebrelMath.lerp(this.appliedGamma, target,
                    NebrelMath.clamp(this.fadeSpeed.get(), 0.0D, 1.0D));
        } else {
            this.appliedGamma = target;
        }
        writeGamma(this.appliedGamma);
    }

    @SuppressWarnings("unchecked")
    private static void writeGamma(double value) {
        SimpleOption<Double> gamma = MinecraftClient.getInstance().options.getGamma();
        ((SimpleOptionAccessor<Double>) (Object) gamma).nebrel$setValue(value);
    }
}

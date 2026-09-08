package de.nebrel.client.module.impl.render;

import de.nebrel.client.core.NebrelState;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;

/**
 * Recolours the enchantment glint.
 *
 * <p>The tint is applied to the glint pass only, as a per-vertex colour
 * multiplier, so the item's own texture is untouched and an unenchanted item
 * looks exactly as it did.</p>
 */
public final class GlintColorizerModule extends Module implements ClientTickHook {

    public final ColorSetting glintColor;
    public final BooleanSetting rainbow;
    public final NumberSetting rainbowSpeed;
    public final NumberSetting opacity;
    public final NumberSetting intensity;

    public GlintColorizerModule() {
        super("glint_colorizer", "Glint Colorizer", "Choose the colour of the enchantment shimmer",
                ModuleCategory.RENDER, "✦");

        this.glintColor = color("color", "Color", "Glint tint", 0xFF8B5CF6, false);

        this.rainbow = bool("rainbow", "Rainbow", "Cycle the glint colour", false,
                SettingSection.APPEARANCE);
        this.rainbowSpeed = number("rainbowSpeed", "Rainbow Speed", "Colour cycles per second",
                0.25D, 0.05D, 2.0D, 0.05D, SettingSection.APPEARANCE);
        this.rainbowSpeed.visibleWhen(this.rainbow);

        this.opacity = number("opacity", "Opacity", "How strong the shimmer is",
                1.0D, 0.0D, 1.0D, 0.05D, SettingSection.APPEARANCE);

        this.intensity = number("intensity", "Intensity",
                "Brightens or dims the tint before it is applied",
                1.0D, 0.2D, 2.0D, 0.05D, SettingSection.APPEARANCE);
    }

    @Override
    protected void onEnable() {
        apply();
    }

    @Override
    protected void onDisable() {
        // Zero means "leave the vanilla glint alone" to the mixin.
        NebrelState.glintColor = 0;
        NebrelState.glintOpacity = 1.0F;
    }

    @Override
    public void onTick(MinecraftClient client) {
        apply();
    }

    private void apply() {
        int base = this.rainbow.get()
                ? ColorUtil.rainbow(System.currentTimeMillis(), this.rainbowSpeed.getFloat(), 0.0F)
                : this.glintColor.get();

        float boost = this.intensity.getFloat();
        int scaled = ColorUtil.argb(255,
                clampChannel(ColorUtil.red(base) * boost),
                clampChannel(ColorUtil.green(base) * boost),
                clampChannel(ColorUtil.blue(base) * boost));

        // Never let the tint become exactly zero: that is the mixin's sentinel
        // for "module off", and a fully black glint would be indistinguishable.
        NebrelState.glintColor = scaled == 0 ? 0xFF010101 : scaled;
        NebrelState.glintOpacity = this.opacity.getFloat();
    }

    private static int clampChannel(float value) {
        return (int) Math.max(0.0F, Math.min(255.0F, value));
    }
}

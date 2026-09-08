package de.nebrel.client.module.impl.visual;

import de.nebrel.client.core.NebrelState;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.MinecraftClient;

/**
 * Adjusts the first-person animations.
 *
 * <p>Everything here is animation timing and placement. Swing speed changes how
 * long the arm takes to complete its arc; it does not change attack cooldown,
 * damage or reach, all of which the server owns. Equip and item-switch timing
 * likewise only affect when the model finishes moving.</p>
 */
public final class AnimationsModule extends Module implements ClientTickHook {

    public final BooleanSetting customSwing;
    public final NumberSetting swingSpeed;
    public final BooleanSetting fastEquip;
    public final NumberSetting equipSpeed;
    public final BooleanSetting smoothSwitching;
    public final NumberSetting handOffsetX;
    public final NumberSetting handOffsetY;
    public final NumberSetting handOffsetZ;
    public final BooleanSetting steadyEating;
    public final BooleanSetting oldSneaking;

    public AnimationsModule() {
        super("animations", "Animations", "Tune the first-person swing, equip and hand position",
                ModuleCategory.VISUAL, "✋");

        this.customSwing = bool("customSwing", "Custom Swing Speed",
                "Override how long a swing takes", false);
        this.swingSpeed = number("swingSpeed", "Swing Speed",
                "Swing duration in ticks; vanilla is 6", 6.0D, 1.0D, 20.0D, 1.0D);
        this.swingSpeed.visibleWhen(this.customSwing);

        this.fastEquip = bool("fastEquip", "Fast Equip",
                "Shorten the raise animation when switching items", false,
                SettingSection.ANIMATION);
        this.equipSpeed = number("equipSpeed", "Equip Speed",
                "Multiplier for the raise animation", 1.5D, 1.0D, 4.0D, 0.1D,
                SettingSection.ANIMATION);
        this.equipSpeed.visibleWhen(this.fastEquip);

        this.smoothSwitching = bool("smoothSwitching", "Smooth Item Switching",
                "Ease between hotbar slots instead of cutting", true, SettingSection.ANIMATION);

        this.steadyEating = bool("steadyEating", "Steady Eating",
                "Reduce the shake while eating and drinking", false, SettingSection.ANIMATION);

        this.oldSneaking = bool("oldSneaking", "Classic Sneak Offset",
                "Use the older, lower hand position while sneaking", false,
                SettingSection.ANIMATION);

        this.handOffsetX = number("handX", "Hand Offset X",
                "Sideways nudge for the held item", 0.0D, -0.3D, 0.3D, 0.01D,
                SettingSection.APPEARANCE);
        this.handOffsetY = number("handY", "Hand Offset Y",
                "Vertical nudge for the held item", 0.0D, -0.3D, 0.3D, 0.01D,
                SettingSection.APPEARANCE);
        this.handOffsetZ = number("handZ", "Hand Offset Z",
                "Depth nudge for the held item", 0.0D, -0.3D, 0.3D, 0.01D,
                SettingSection.APPEARANCE);
    }

    @Override
    protected void onEnable() {
        apply();
    }

    @Override
    protected void onDisable() {
        NebrelState.swingDurationTicks = -1;
        // The hand offset shares the item transform channel with the Item Model
        // module; clear only the part this module owns.
        if (!NebrelState.itemModelActive) {
            NebrelState.itemOffsetX = 0.0F;
            NebrelState.itemOffsetY = 0.0F;
            NebrelState.itemOffsetZ = 0.0F;
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        apply();
    }

    private void apply() {
        NebrelState.swingDurationTicks = this.customSwing.get()
                ? (int) Math.round(this.swingSpeed.get())
                : -1;

        // Only claim the transform channel when the Item Model module is not
        // already driving it, so the two never fight over the same fields.
        boolean handOffsetUsed = this.handOffsetX.get() != 0.0D
                || this.handOffsetY.get() != 0.0D
                || this.handOffsetZ.get() != 0.0D;
        if (handOffsetUsed && !NebrelState.itemModelActive) {
            NebrelState.itemOffsetX = this.handOffsetX.getFloat();
            NebrelState.itemOffsetY = this.handOffsetY.getFloat();
            NebrelState.itemOffsetZ = this.handOffsetZ.getFloat();
            NebrelState.itemModelActive = true;
        }
    }

    /** True when the swing timing is currently overridden. */
    public boolean overridingSwing() {
        return this.customSwing.get();
    }
}

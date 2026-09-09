package de.nebrel.client.module.impl.render;

import de.nebrel.client.core.NebrelState;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.MinecraftClient;

/**
 * Repositions the item held in first person.
 *
 * <p>Every value is a pure render transform applied around vanilla's own swing
 * and equip animation, so the item still behaves exactly as it did; it is just
 * drawn somewhere else. Nothing about reach, timing or hit detection is
 * touched.</p>
 */
public final class ItemModelModule extends Module implements ClientTickHook {

    public final NumberSetting scale;
    public final NumberSetting offsetX;
    public final NumberSetting offsetY;
    public final NumberSetting offsetZ;
    public final NumberSetting rotateX;
    public final NumberSetting rotateY;
    public final NumberSetting rotateZ;

    public ItemModelModule() {
        super("item_model", "Item Model", "Move and resize the item in your hand",
                ModuleCategory.RENDER, "◇");

        this.scale = number("scale", "Scale", "Item size", 1.0D, 0.4D, 2.0D, 0.01D);

        this.offsetX = number("x", "Offset X",
                "Sideways offset; positive moves the main hand outward",
                0.0D, -0.6D, 0.6D, 0.01D, SettingSection.APPEARANCE);
        this.offsetY = number("y", "Offset Y", "Vertical offset",
                0.0D, -0.6D, 0.6D, 0.01D, SettingSection.APPEARANCE);
        this.offsetZ = number("z", "Offset Z", "Depth offset; negative pushes it away",
                0.0D, -0.6D, 0.6D, 0.01D, SettingSection.APPEARANCE);

        this.rotateX = number("rotX", "Rotation X", "Pitch in degrees",
                0.0D, -180.0D, 180.0D, 1.0D, SettingSection.APPEARANCE);
        this.rotateY = number("rotY", "Rotation Y", "Yaw in degrees",
                0.0D, -180.0D, 180.0D, 1.0D, SettingSection.APPEARANCE);
        this.rotateZ = number("rotZ", "Rotation Z", "Roll in degrees",
                0.0D, -180.0D, 180.0D, 1.0D, SettingSection.APPEARANCE);
    }

    @Override
    protected void onEnable() {
        apply();
    }

    @Override
    protected void onDisable() {
        NebrelState.itemModelActive = false;
        NebrelState.itemScale = 1.0F;
        NebrelState.itemOffsetX = 0.0F;
        NebrelState.itemOffsetY = 0.0F;
        NebrelState.itemOffsetZ = 0.0F;
        NebrelState.itemRotateX = 0.0F;
        NebrelState.itemRotateY = 0.0F;
        NebrelState.itemRotateZ = 0.0F;
    }

    @Override
    public void onTick(MinecraftClient client) {
        apply();
    }

    private void apply() {
        NebrelState.itemScale = this.scale.getFloat();
        NebrelState.itemOffsetX = this.offsetX.getFloat();
        NebrelState.itemOffsetY = this.offsetY.getFloat();
        NebrelState.itemOffsetZ = this.offsetZ.getFloat();
        NebrelState.itemRotateX = this.rotateX.getFloat();
        NebrelState.itemRotateY = this.rotateY.getFloat();
        NebrelState.itemRotateZ = this.rotateZ.getFloat();
        // The mixin skips all matrix work while this is false, so an enabled
        // module left at its defaults costs nothing.
        NebrelState.itemModelActive = NebrelState.itemScale != 1.0F
                || NebrelState.itemOffsetX != 0.0F
                || NebrelState.itemOffsetY != 0.0F
                || NebrelState.itemOffsetZ != 0.0F
                || NebrelState.itemRotateX != 0.0F
                || NebrelState.itemRotateY != 0.0F
                || NebrelState.itemRotateZ != 0.0F;
    }
}

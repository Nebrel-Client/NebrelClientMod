package de.nebrel.client.module.impl.render;

import de.nebrel.client.core.NebrelState;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.MinecraftClient;

/**
 * Removes or pushes back the various kinds of fog.
 *
 * <p>Each fog kind is separate on purpose. Terrain fog is almost always
 * unwanted; underwater and lava fog carry real information about where the
 * player is, so they are left on by default.</p>
 *
 * <p>This only affects rendering. Render distance, chunk loading and what the
 * server sends are all unchanged, so nothing becomes visible that was not
 * already being drawn.</p>
 */
public final class NoFogModule extends Module implements ClientTickHook {

    public final BooleanSetting terrain;
    public final BooleanSetting water;
    public final BooleanSetting lava;
    public final BooleanSetting powderSnow;
    public final NumberSetting distanceScale;

    public NoFogModule() {
        super("no_fog", "No Fog", "Clear the haze from the horizon", ModuleCategory.RENDER, "≋");

        this.terrain = bool("terrain", "Terrain Fog", "Remove the distance haze", true);
        this.water = bool("water", "Water Fog", "Remove the underwater haze", false);
        this.lava = bool("lava", "Lava Fog", "See through lava", false);
        this.powderSnow = bool("powderSnow", "Powder Snow Fog",
                "See through powder snow", false);

        this.distanceScale = number("distance", "Fog Distance",
                "Multiplier for how far away fog begins, when it is left on",
                1.0D, 1.0D, 8.0D, 0.25D, SettingSection.ADVANCED);
        // Only meaningful while terrain fog is still being drawn.
        this.distanceScale.visibleWhen(() -> !this.terrain.get());
    }

    @Override
    protected void onEnable() {
        apply();
    }

    @Override
    protected void onDisable() {
        NebrelState.disableTerrainFog = false;
        NebrelState.disableWaterFog = false;
        NebrelState.disableLavaFog = false;
        NebrelState.disablePowderSnowFog = false;
        NebrelState.fogDistanceScale = 1.0F;
    }

    @Override
    public void onTick(MinecraftClient client) {
        apply();
    }

    private void apply() {
        NebrelState.disableTerrainFog = this.terrain.get();
        NebrelState.disableWaterFog = this.water.get();
        NebrelState.disableLavaFog = this.lava.get();
        NebrelState.disablePowderSnowFog = this.powderSnow.get();
        NebrelState.fogDistanceScale = this.terrain.get() ? 1.0F : this.distanceScale.getFloat();
    }
}

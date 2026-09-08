package de.nebrel.client.module.impl.hud;

import de.nebrel.client.event.WorldRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.WorldRenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Vec3d;

/**
 * A countdown over primed TNT.
 *
 * <p>The fuse is part of the entity's tracked data, so this shows something the
 * client already knows and the player could already see by counting the flashes.
 * Nothing hidden is revealed, and the label is depth tested along with the rest
 * of the world, so TNT behind a wall is not visible through it.</p>
 */
public final class TntTimerModule extends Module implements WorldRenderHook {

    public final BooleanSetting numeric;
    public final BooleanSetting showDistance;
    public final BooleanSetting progressBar;
    public final ColorSetting safeColor;
    public final ColorSetting dangerColor;
    public final NumberSetting scale;
    public final NumberSetting maxDistance;
    public final BooleanSetting background;
    public final NumberSetting dangerThreshold;

    public TntTimerModule() {
        super("tnt_timer", "TNT Timer", "Count down the fuse on primed TNT",
                ModuleCategory.HUD, "⏱");

        this.numeric = bool("numeric", "Show Seconds", "Print the remaining fuse in seconds", true);
        this.showDistance = bool("distance", "Show Distance",
                "Append how far away the TNT is", false);
        this.progressBar = bool("progressBar", "Progress Bar",
                "Draw a bar that empties as the fuse burns", true);

        this.safeColor = color("safeColor", "Safe Color", "Colour while there is time left",
                0xFF3DD68C, true, SettingSection.APPEARANCE);
        this.dangerColor = color("dangerColor", "Danger Color",
                "Colour once the fuse is nearly out", 0xFFF2555A, true, SettingSection.APPEARANCE);

        this.dangerThreshold = number("dangerThreshold", "Danger Threshold",
                "Seconds remaining at which the colour turns", 1.0D, 0.2D, 3.0D, 0.1D,
                SettingSection.APPEARANCE);

        this.scale = number("scale", "Scale", "Label size", 1.0D, 0.4D, 3.0D, 0.1D,
                SettingSection.APPEARANCE);
        this.background = bool("background", "Background", "Draw a plate behind the label",
                true, SettingSection.APPEARANCE);

        this.maxDistance = number("maxDistance", "Max Distance",
                "Stop labelling TNT beyond this many blocks",
                48.0D, 8.0D, 128.0D, 4.0D, SettingSection.BEHAVIOR);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        double limit = this.maxDistance.get();
        double limitSquared = limit * limit;
        float tickDelta = client.getRenderTickCounter().getTickDelta(false);

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof TntEntity tnt) || !tnt.isAlive()) {
                continue;
            }
            // Interpolated position, so the label does not stutter behind the model.
            Vec3d position = tnt.getLerpedPos(tickDelta).add(0.0D, 1.0D, 0.0D);
            if (WorldRenderUtil.squaredDistanceToCamera(context, position) > limitSquared) {
                continue;
            }

            float seconds = tnt.getFuse() / 20.0F;
            float fraction = NebrelMath.clamp(seconds / 4.0F, 0.0F, 1.0F);
            int color = seconds <= this.dangerThreshold.get()
                    ? this.dangerColor.get()
                    : ColorUtil.lerp(this.dangerColor.get(), this.safeColor.get(), fraction);

            StringBuilder label = new StringBuilder();
            if (this.numeric.get()) {
                label.append(String.format("%.1fs", seconds));
            }
            if (this.progressBar.get()) {
                if (label.length() > 0) {
                    label.append(' ');
                }
                label.append(bar(fraction));
            }
            if (this.showDistance.get()) {
                double distance = Math.sqrt(
                        WorldRenderUtil.squaredDistanceToCamera(context, position));
                if (label.length() > 0) {
                    label.append(' ');
                }
                label.append('(').append(Math.round(distance)).append("m)");
            }
            if (label.length() == 0) {
                continue;
            }

            WorldRenderUtil.drawWorldText(context, label.toString(), position, color,
                    this.scale.getFloat(), this.background.get() ? 0x60000000 : 0);
        }
    }

    /** An eight-cell bar built from block characters. */
    private static String bar(float fraction) {
        int filled = Math.round(fraction * 8.0F);
        StringBuilder builder = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            builder.append(i < filled ? '█' : '░');
        }
        return builder.toString();
    }
}

package de.nebrel.client.hud.widget;

import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.network.ClientPlayerEntity;

/** Facing direction as a compass point, optionally with the axis and yaw. */
public final class DirectionWidget extends LabeledHudWidget {

    private static final String[] COMPASS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private static final String[] AXIS = {"+Z", "-X +Z", "-X", "-X -Z", "-Z", "+X -Z", "+X", "+X +Z"};

    public final BooleanSetting showAxis;
    public final BooleanSetting showYaw;

    public DirectionWidget() {
        super("direction", "Direction", HudAnchor.TOP_LEFT, 4.0F, 46.0F, false);

        this.showAxis = add(new BooleanSetting("hud.direction.axis", "Show Axis",
                "Append the signed axis, e.g. +X", true));
        this.showAxis.section(SettingSection.APPEARANCE);

        this.showYaw = add(new BooleanSetting("hud.direction.yaw", "Show Yaw",
                "Append the raw yaw in degrees", false));
        this.showYaw.section(SettingSection.APPEARANCE);
    }

    @Override
    protected void buildRows() {
        ClientPlayerEntity player = client().player;
        if (player == null) {
            row("Facing", "-");
            return;
        }
        float yaw = player.getYaw();
        // Normalise to [0, 360) then bucket into eight 45 degree sectors.
        float normalised = ((yaw % 360.0F) + 360.0F) % 360.0F;
        int sector = Math.round(normalised / 45.0F) & 7;

        StringBuilder value = new StringBuilder(COMPASS[sector]);
        if (this.showAxis.get()) {
            value.append(" (").append(AXIS[sector]).append(')');
        }
        if (this.showYaw.get()) {
            value.append(' ').append(Math.round(normalised)).append('°');
        }
        row("Facing", value.toString());
    }
}

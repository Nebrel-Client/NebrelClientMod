package de.nebrel.client.hud.widget;

import de.nebrel.client.core.ClientStats;
import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.SettingSection;

/** Horizontal movement speed in blocks per second. */
public final class SpeedWidget extends LabeledHudWidget {

    private final ClientStats stats;

    public final BooleanSetting showVertical;

    public SpeedWidget(ClientStats stats) {
        super("speed", "Speed", HudAnchor.TOP_LEFT, 4.0F, 60.0F, false);
        this.stats = stats;

        this.showVertical = add(new BooleanSetting("hud.speed.vertical", "Vertical Speed",
                "Also show the vertical component", false));
        this.showVertical.section(SettingSection.APPEARANCE);
    }

    @Override
    protected void buildRows() {
        row("Speed", String.format("%.2f b/s", this.stats.speed()));
        if (this.showVertical.get()) {
            row("Vertical", String.format("%.2f b/s", this.stats.verticalSpeed()));
        }
    }
}

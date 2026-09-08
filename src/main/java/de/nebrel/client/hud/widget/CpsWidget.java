package de.nebrel.client.hud.widget;

import de.nebrel.client.core.ClientStats;
import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.SettingSection;

/**
 * Clicks per second for the mouse buttons.
 *
 * <p>Purely an observation of input the user produced. Nothing in Nebrel
 * generates clicks.</p>
 */
public final class CpsWidget extends LabeledHudWidget {

    private final ClientStats stats;

    public final BooleanSetting showLeft;
    public final BooleanSetting showRight;
    public final BooleanSetting combined;

    public CpsWidget(ClientStats stats) {
        super("cps", "CPS", HudAnchor.BOTTOM_LEFT, 4.0F, -4.0F, false);
        this.stats = stats;

        this.showLeft = add(new BooleanSetting("hud.cps.left", "Left Button", "Show left click rate", true));
        this.showLeft.section(SettingSection.APPEARANCE);

        this.showRight = add(new BooleanSetting("hud.cps.right", "Right Button", "Show right click rate", true));
        this.showRight.section(SettingSection.APPEARANCE);

        this.combined = add(new BooleanSetting("hud.cps.combined", "Single Row",
                "Show both rates on one line", true));
        this.combined.section(SettingSection.APPEARANCE);
    }

    @Override
    protected void buildRows() {
        boolean left = this.showLeft.get();
        boolean right = this.showRight.get();
        if (!left && !right) {
            return;
        }
        if (this.combined.get()) {
            String value = left && right
                    ? this.stats.leftCps() + " | " + this.stats.rightCps()
                    : String.valueOf(left ? this.stats.leftCps() : this.stats.rightCps());
            row("CPS", value);
            return;
        }
        if (left) {
            row("LMB", String.valueOf(this.stats.leftCps()));
        }
        if (right) {
            row("RMB", String.valueOf(this.stats.rightCps()));
        }
    }
}

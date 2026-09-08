package de.nebrel.client.hud.widget;

import de.nebrel.client.core.ClientStats;
import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.SettingSection;

/** Java heap usage. */
public final class MemoryWidget extends LabeledHudWidget {

    public final BooleanSetting asPercentage;
    public final BooleanSetting showMax;

    public MemoryWidget() {
        super("memory", "Memory", HudAnchor.TOP_RIGHT, -4.0F, 18.0F, false);

        this.asPercentage = add(new BooleanSetting("hud.memory.percentage", "Percentage",
                "Show a percentage instead of megabytes", false));
        this.asPercentage.section(SettingSection.APPEARANCE);

        this.showMax = add(new BooleanSetting("hud.memory.showMax", "Show Maximum",
                "Append the heap ceiling", true));
        this.showMax.section(SettingSection.APPEARANCE);
        this.showMax.visibleWhen(() -> !this.asPercentage.get());
    }

    @Override
    protected void buildRows() {
        if (this.asPercentage.get()) {
            row("Memory", Math.round(ClientStats.memoryFraction() * 100.0F) + "%");
            return;
        }
        long used = ClientStats.usedMemoryMb();
        row("Memory", this.showMax.get()
                ? used + " / " + ClientStats.maxMemoryMb() + " MB"
                : used + " MB");
    }
}

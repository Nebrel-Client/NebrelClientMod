package de.nebrel.client.hud.widget;

import de.nebrel.client.core.ClientStats;
import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;

/** Frames per second, counted by the client itself. */
public final class FpsWidget extends LabeledHudWidget {

    private final ClientStats stats;

    public FpsWidget(ClientStats stats) {
        super("fps", "FPS", HudAnchor.TOP_LEFT, 4.0F, 4.0F, true);
        this.stats = stats;
    }

    @Override
    protected void buildRows() {
        row("FPS", String.valueOf(this.stats.fps()));
    }
}

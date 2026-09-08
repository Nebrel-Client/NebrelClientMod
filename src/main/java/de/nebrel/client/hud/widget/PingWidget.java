package de.nebrel.client.hud.widget;

import de.nebrel.client.core.ClientStats;
import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;

/** Round-trip time to the current server. Hidden in single player. */
public final class PingWidget extends LabeledHudWidget {

    private final ClientStats stats;

    public PingWidget(ClientStats stats) {
        super("ping", "Ping", HudAnchor.TOP_LEFT, 4.0F, 18.0F, true);
        this.stats = stats;
    }

    @Override
    protected void buildRows() {
        int ping = this.stats.ping(client());
        if (ping < 0) {
            // No row at all, rather than a meaningless "0 ms" in single player.
            return;
        }
        row("Ping", ping + "ms");
    }
}

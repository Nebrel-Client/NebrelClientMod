package de.nebrel.client.hud.widget;

import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

/** The server address, or a single-player marker. */
public final class ServerWidget extends LabeledHudWidget {

    public final BooleanSetting hideInSingleplayer;
    public final BooleanSetting stripPort;

    public ServerWidget() {
        super("server", "Server", HudAnchor.TOP_RIGHT, -4.0F, 4.0F, false);

        this.hideInSingleplayer = add(new BooleanSetting("hud.server.hideSingleplayer",
                "Hide in Singleplayer", "Show nothing when not connected to a server", true));
        this.hideInSingleplayer.section(SettingSection.BEHAVIOR);

        this.stripPort = add(new BooleanSetting("hud.server.stripPort", "Hide Port",
                "Drop the :port suffix from the address", true));
        this.stripPort.section(SettingSection.APPEARANCE);
    }

    @Override
    protected void buildRows() {
        MinecraftClient client = client();
        ServerInfo info = client.getCurrentServerEntry();
        if (info == null) {
            if (!this.hideInSingleplayer.get()) {
                row("Server", client.isInSingleplayer() ? "Singleplayer" : "Local");
            }
            return;
        }
        String address = info.address == null ? "Unknown" : info.address;
        if (this.stripPort.get()) {
            int colon = address.lastIndexOf(':');
            // Guard against IPv6 literals, where colons are part of the host.
            if (colon > 0 && address.indexOf(':') == colon) {
                address = address.substring(0, colon);
            }
        }
        row("Server", address);
    }
}

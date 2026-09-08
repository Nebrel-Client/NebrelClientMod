package de.nebrel.client.hud.widget;

import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.network.ClientPlayerEntity;

/** Player position, on one line or three. */
public final class CoordinatesWidget extends LabeledHudWidget {

    public final BooleanSetting separateLines;
    public final NumberSetting decimals;
    public final BooleanSetting showNetherCoords;

    public CoordinatesWidget() {
        super("coordinates", "Coordinates", HudAnchor.TOP_LEFT, 4.0F, 32.0F, true);

        this.separateLines = add(new BooleanSetting("hud.coordinates.separateLines", "Separate Lines",
                "One axis per line instead of a single row", false));
        this.separateLines.section(SettingSection.APPEARANCE);

        this.decimals = add(new NumberSetting("hud.coordinates.decimals", "Decimals",
                "Digits after the decimal point", 1.0D, 0.0D, 3.0D, 1.0D));
        this.decimals.section(SettingSection.APPEARANCE);

        this.showNetherCoords = add(new BooleanSetting("hud.coordinates.nether", "Nether Conversion",
                "Also show the matching coordinates in the other dimension", false));
        this.showNetherCoords.section(SettingSection.BEHAVIOR);
    }

    @Override
    protected void buildRows() {
        ClientPlayerEntity player = client().player;
        if (player == null) {
            row("XYZ", "0 0 0");
            return;
        }
        int places = this.decimals.getInt();
        String x = format(player.getX(), places);
        String y = format(player.getY(), places);
        String z = format(player.getZ(), places);

        if (this.separateLines.get()) {
            row("X", x);
            row("Y", y);
            row("Z", z);
        } else {
            row("XYZ", x + " " + y + " " + z);
        }

        if (this.showNetherCoords.get()) {
            boolean inNether = player.getWorld() != null
                    && player.getWorld().getRegistryKey() == net.minecraft.world.World.NETHER;
            // Overworld to Nether is a divide by eight; the other way multiplies.
            double factor = inNether ? 8.0D : 0.125D;
            row(inNether ? "Overworld" : "Nether",
                    format(player.getX() * factor, 0) + " " + format(player.getZ() * factor, 0));
        }
    }

    private static String format(double value, int places) {
        if (places <= 0) {
            return String.valueOf((int) Math.floor(value));
        }
        return String.format("%." + places + "f", value);
    }
}

package de.nebrel.client.hud.widget;

import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.network.ClientPlayerEntity;

/** Name, health, hunger and experience level for the local player. */
public final class PlayerInfoWidget extends LabeledHudWidget {

    public final BooleanSetting showName;
    public final BooleanSetting showHealth;
    public final BooleanSetting showHunger;
    public final BooleanSetting showLevel;

    public PlayerInfoWidget() {
        super("playerinfo", "Player Info", HudAnchor.BOTTOM_RIGHT, -4.0F, -4.0F, false);

        this.showName = add(new BooleanSetting("hud.playerinfo.name", "Name", "Show the player name", true));
        this.showName.section(SettingSection.APPEARANCE);
        this.showHealth = add(new BooleanSetting("hud.playerinfo.health", "Health", "Show health points", true));
        this.showHealth.section(SettingSection.APPEARANCE);
        this.showHunger = add(new BooleanSetting("hud.playerinfo.hunger", "Hunger", "Show hunger points", true));
        this.showHunger.section(SettingSection.APPEARANCE);
        this.showLevel = add(new BooleanSetting("hud.playerinfo.level", "Level", "Show the XP level", false));
        this.showLevel.section(SettingSection.APPEARANCE);
    }

    @Override
    protected void buildRows() {
        ClientPlayerEntity player = client().player;
        if (player == null) {
            return;
        }
        if (this.showName.get()) {
            row("Name", player.getGameProfile().getName());
        }
        if (this.showHealth.get()) {
            float health = player.getHealth();
            float absorption = player.getAbsorptionAmount();
            String value = trim(health) + " / " + trim(player.getMaxHealth());
            if (absorption > 0.0F) {
                value += " (+" + trim(absorption) + ")";
            }
            row("Health", value);
        }
        if (this.showHunger.get()) {
            row("Hunger", player.getHungerManager().getFoodLevel() + " / 20");
        }
        if (this.showLevel.get()) {
            row("Level", String.valueOf(player.experienceLevel));
        }
    }

    /** Drops a trailing ".0" so whole numbers read cleanly. */
    private static String trim(float value) {
        return value == Math.floor(value)
                ? String.valueOf((int) value)
                : String.format("%.1f", value);
    }
}

package de.nebrel.client.module.impl.hud;

import de.nebrel.client.core.NebrelState;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

/**
 * Switches individual vanilla HUD elements off.
 *
 * <p>Each element is cancelled at its own render call, so hiding one never
 * disturbs the layout of the others. Health, hunger and armour share a single
 * vanilla method, so they are only actually hidden when all three are switched
 * off; the settings say so rather than pretending otherwise.</p>
 */
public final class VanillaHudModule extends Module implements ClientTickHook {

    public final BooleanSetting hotbar;
    public final BooleanSetting health;
    public final BooleanSetting hunger;
    public final BooleanSetting armor;
    public final BooleanSetting experience;
    public final BooleanSetting statusEffects;
    public final BooleanSetting scoreboard;
    public final BooleanSetting crosshair;

    public VanillaHudModule() {
        super("vanilla_hud", "Vanilla HUD", "Hide the parts of the default HUD you do not want",
                ModuleCategory.HUD, "▣");

        this.hotbar = bool("hotbar", "Hide Hotbar", "Hide the item bar and its slots", false);
        this.crosshair = bool("crosshair", "Hide Crosshair", "Hide the vanilla crosshair", false);
        this.experience = bool("experience", "Hide Experience Bar",
                "Hide the XP bar and level", false);
        this.statusEffects = bool("statusEffects", "Hide Effect Icons",
                "Hide the status effect icons in the corner", false);
        this.scoreboard = bool("scoreboard", "Hide Scoreboard",
                "Hide the sidebar scoreboard", false);

        this.health = bool("health", "Hide Health", "Hide the hearts", false);
        this.hunger = bool("hunger", "Hide Hunger", "Hide the hunger bar", false);
        this.armor = bool("armor", "Hide Armor", "Hide the armour bar", false);
    }

    @Override
    protected void onEnable() {
        apply();
    }

    @Override
    protected void onDisable() {
        NebrelState.hideHotbar = false;
        NebrelState.hideHealth = false;
        NebrelState.hideHunger = false;
        NebrelState.hideArmor = false;
        NebrelState.hideExperience = false;
        NebrelState.hideStatusEffects = false;
        NebrelState.hideScoreboard = false;
        // The crosshair flag is shared with the Crosshair module, which owns it
        // whenever that module is on; clearing here is safe because that module
        // reasserts the flag every tick.
        NebrelState.hideCrosshair = false;
    }

    @Override
    public void onTick(MinecraftClient client) {
        apply();
    }

    private void apply() {
        NebrelState.hideHotbar = this.hotbar.get();
        NebrelState.hideHealth = this.health.get();
        NebrelState.hideHunger = this.hunger.get();
        NebrelState.hideArmor = this.armor.get();
        NebrelState.hideExperience = this.experience.get();
        NebrelState.hideStatusEffects = this.statusEffects.get();
        NebrelState.hideScoreboard = this.scoreboard.get();
        if (this.crosshair.get()) {
            NebrelState.hideCrosshair = true;
        }
    }
}

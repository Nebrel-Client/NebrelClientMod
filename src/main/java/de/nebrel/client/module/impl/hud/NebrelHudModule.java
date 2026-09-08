package de.nebrel.client.module.impl.hud;

import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.event.HudRenderHook;
import de.nebrel.client.hud.HudManager;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * The client's own HUD: a set of anchored, individually styled widgets.
 *
 * <p>The module owns nothing but the switch and a couple of global rules. Each
 * widget carries its own placement and style, edited in the HUD editor rather
 * than through a long settings list, which is why this class exposes the widget
 * toggles and leaves the rest to the editor.</p>
 */
public final class NebrelHudModule extends Module implements HudRenderHook {

    public final BooleanSetting hideInScreens;
    public final BooleanSetting hideWithVanillaHud;
    public final BooleanSetting hideInDebug;

    private final HudManager hud;

    public NebrelHudModule(HudManager hud) {
        super("nebrel_hud", "Nebrel HUD", "Frame rate, position, ping and more, where you want them",
                ModuleCategory.HUD, "▤");
        this.hud = hud;

        this.hideInScreens = bool("hideInScreens", "Hide in Menus",
                "Hide the widgets while a screen is open", true, SettingSection.BEHAVIOR);
        this.hideWithVanillaHud = bool("hideWithVanillaHud", "Follow F1",
                "Hide the widgets when the vanilla HUD is hidden", true, SettingSection.BEHAVIOR);
        this.hideInDebug = bool("hideInDebug", "Hide in F3",
                "Hide the widgets while the debug screen is open", true, SettingSection.BEHAVIOR);

        // Surface each widget's own on/off switch here, grouped under Widgets,
        // so a widget can be enabled without opening the editor. These are the
        // same setting objects the widgets own, so they are written to both
        // modules.json and hud.json; that is redundant but self-consistent,
        // and it keeps hud.json complete enough to share on its own.
        for (var widget : hud.widgets()) {
            BooleanSetting toggle = widget.enabled;
            toggle.section("Widgets");
            register(toggle);
        }
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (this.hideWithVanillaHud.get() && client.options.hudHidden) {
            return;
        }
        if (this.hideInScreens.get() && client.currentScreen != null) {
            return;
        }
        if (this.hideInDebug.get() && client.getDebugHud().shouldShowDebugHud()) {
            return;
        }

        this.hud.render(context,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight(),
                NebrelClient.get().accent());
    }
}

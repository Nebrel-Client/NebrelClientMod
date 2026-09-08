package de.nebrel.client.module.impl.utility;

import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.event.HudRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourcePackProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Quality-of-life around resource packs.
 *
 * <p>A bind to reload packs without opening the options menu, a toast when the
 * reload finishes, and an on-screen list of what is actually enabled, which is
 * otherwise three menus deep.</p>
 */
public final class PackTweaksModule extends Module implements ClientTickHook, HudRenderHook {

    public final KeybindSetting reloadKey;
    public final BooleanSetting reloadNotification;
    public final BooleanSetting showPackList;
    public final KeybindSetting packListKey;
    public final BooleanSetting showDescriptions;
    public final NumberSetting listDuration;

    private long packListShownAt;
    private boolean reloadPending;
    private long reloadStartedAt;

    public PackTweaksModule() {
        super("pack_tweaks", "Pack Tweaks", "Reload and inspect resource packs without the menus",
                ModuleCategory.UTILITY, "▦");

        this.reloadKey = register(new KeybindSetting("pack_tweaks.reloadKey", "Reload Key",
                "Reloads every resource pack", KeybindSetting.UNBOUND));

        this.reloadNotification = bool("reloadNotification", "Reload Notification",
                "Show a toast when a reload finishes", true, SettingSection.BEHAVIOR);

        this.packListKey = register(new KeybindSetting("pack_tweaks.packListKey", "Pack List Key",
                "Shows the enabled packs on screen", KeybindSetting.UNBOUND));

        this.showPackList = bool("showPackList", "Pack List",
                "Allow the pack list overlay", true, SettingSection.APPEARANCE);

        this.showDescriptions = bool("descriptions", "Pack Descriptions",
                "Include each pack's description in the list", false, SettingSection.APPEARANCE);
        this.showDescriptions.visibleWhen(this.showPackList);

        this.listDuration = number("listDuration", "List Duration",
                "How long the pack list stays on screen, in seconds",
                4.0D, 1.0D, 15.0D, 0.5D, SettingSection.APPEARANCE);
        this.listDuration.visibleWhen(this.showPackList);
    }

    @Override
    protected void onDisable() {
        this.packListShownAt = 0L;
        this.reloadPending = false;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.currentScreen != null) {
            return;
        }
        var keybinds = NebrelClient.get().keybinds();

        if (this.reloadKey.bound() && keybinds.justPressed(this.reloadKey.get())
                && !this.reloadPending) {
            this.reloadPending = true;
            this.reloadStartedAt = System.currentTimeMillis();
            if (this.reloadNotification.get()) {
                NebrelClient.get().notifications().info("Resource packs", "Reloading...");
            }
            // reloadResources returns a future; report on it rather than
            // guessing when the reload has finished.
            client.reloadResources().whenComplete((result, error) -> {
                this.reloadPending = false;
                if (!this.reloadNotification.get()) {
                    return;
                }
                long millis = System.currentTimeMillis() - this.reloadStartedAt;
                if (error != null) {
                    NebrelClient.get().notifications().error("Resource packs",
                            "Reload failed: " + error.getClass().getSimpleName());
                } else {
                    NebrelClient.get().notifications().success("Resource packs",
                            "Reloaded in " + millis + " ms");
                }
            });
        }

        if (this.showPackList.get() && this.packListKey.bound()
                && keybinds.justPressed(this.packListKey.get())) {
            this.packListShownAt = System.currentTimeMillis();
        }
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (this.packListShownAt == 0L) {
            return;
        }
        long elapsed = System.currentTimeMillis() - this.packListShownAt;
        long lifetime = Math.round(this.listDuration.get() * 1000.0D);
        if (elapsed > lifetime) {
            this.packListShownAt = 0L;
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.currentScreen != null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        for (ResourcePackProfile profile : client.getResourcePackManager().getEnabledProfiles()) {
            lines.add(profile.getDisplayName().getString());
            if (this.showDescriptions.get()) {
                String description = profile.getDescription().getString();
                if (!description.isBlank()) {
                    lines.add("   " + description);
                }
            }
        }
        if (lines.isEmpty()) {
            lines.add("No resource packs enabled");
        }

        // Fade out over the last 400 ms rather than vanishing.
        float alpha = elapsed > lifetime - 400L
                ? Math.max(0.0F, (lifetime - elapsed) / 400.0F)
                : 1.0F;

        int lineHeight = RenderUtil.lineHeight();
        float width = 0.0F;
        for (String line : lines) {
            width = Math.max(width, RenderUtil.textWidth(line));
        }
        float boxWidth = width + 16.0F;
        float boxHeight = lines.size() * (lineHeight + 2.0F) + 18.0F;
        float x = 8.0F;
        float y = (client.getWindow().getScaledHeight() - boxHeight) / 2.0F;

        var theme = NebrelClient.get().ui().theme();
        RenderUtil.roundedRect(context, x, y, boxWidth, boxHeight, 7.0F,
                ColorUtil.fadeAlpha(theme.surfaceElevated, alpha * 0.95F));
        RenderUtil.roundedOutline(context, x, y, boxWidth, boxHeight, 7.0F,
                ColorUtil.fadeAlpha(theme.border, alpha));

        RenderUtil.textFlat(context, "Resource Packs", x + 8.0F, y + 6.0F,
                ColorUtil.fadeAlpha(theme.accent, alpha));

        for (int i = 0; i < lines.size(); i++) {
            RenderUtil.textFlat(context, lines.get(i), x + 8.0F,
                    y + 20.0F + i * (lineHeight + 2.0F),
                    ColorUtil.fadeAlpha(theme.textSecondary, alpha));
        }
    }
}

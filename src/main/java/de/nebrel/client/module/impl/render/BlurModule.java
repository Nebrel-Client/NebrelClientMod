package de.nebrel.client.module.impl.render;

import de.nebrel.client.config.ClientSettings;
import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.event.HudRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

/**
 * Dims and softens the world behind open screens.
 *
 * <p>What this does, precisely: it lays a themed, animated scrim over the world
 * behind a screen, and switches the client menu's own background style to
 * match. It does <em>not</em> run a Gaussian blur shader. A real blur needs a
 * framebuffer post-process pass, and adding one would mean owning shader
 * lifetime, resize handling and compatibility with every shader mod; the scrim
 * gets most of the readability benefit for none of that risk, and the setting
 * names say so rather than promising a blur that is not there.</p>
 *
 * <p>Where the game already applies its own menu backdrop, selecting "Blur" in
 * the client's background setting hands the job to that instead.</p>
 */
public final class BlurModule extends Module implements HudRenderHook {

    public final BooleanSetting inventory;
    public final BooleanSetting chat;
    public final BooleanSetting otherScreens;
    public final BooleanSetting useVanillaBackdrop;
    public final NumberSetting strength;
    public final ColorSetting scrimColor;

    private final Animation fade;

    public BlurModule() {
        super("blur", "Screen Dim", "Soften the world behind open screens so the UI reads clearly",
                ModuleCategory.RENDER, "◍");

        this.inventory = bool("inventory", "Inventory Screens",
                "Dim behind the inventory, chests and other containers", true);
        this.chat = bool("chat", "Chat", "Dim behind the chat box", false);
        this.otherScreens = bool("other", "Other Screens",
                "Dim behind every other screen, such as pause and options", true);

        this.useVanillaBackdrop = bool("vanillaBackdrop", "Use Game Backdrop",
                "Let the game draw its own menu backdrop under the client menu", false,
                SettingSection.BEHAVIOR);

        this.strength = number("strength", "Strength", "How dark the scrim is",
                0.5D, 0.05D, 1.0D, 0.05D, SettingSection.APPEARANCE);

        this.scrimColor = color("color", "Scrim Color",
                "Colour laid over the world; alpha is taken from Strength",
                0xFF07070B, false, SettingSection.APPEARANCE);

        // Fixed duration. An Animation's length is set once at construction,
        // and modules are built while the client singleton is still being
        // assembled, so the client-wide animation speed is not readable here.
        this.fade = new Animation(0.0F, 150L, Easing.EASE_OUT_CUBIC);
    }

    @Override
    protected void onEnable() {
        syncMenuBackground();
    }

    @Override
    protected void onDisable() {
        this.fade.set(0.0F);
        // Hand the client menu's background setting back to plain dimming.
        ClientSettings settings = NebrelClient.get().settings();
        if (settings.backgroundStyle.get() == ClientSettings.BackgroundStyle.BLUR) {
            settings.backgroundStyle.set(ClientSettings.BackgroundStyle.DIM);
        }
    }

    private void syncMenuBackground() {
        ClientSettings settings = NebrelClient.get().settings();
        settings.backgroundStyle.set(this.useVanillaBackdrop.get()
                ? ClientSettings.BackgroundStyle.BLUR
                : ClientSettings.BackgroundStyle.DIM);
    }

    /**
     * Drawn from the HUD hook, which runs while a screen is open too, so the
     * scrim sits under the screen's own contents.
     */
    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;

        this.fade.animateTo(shouldDim(screen));
        float amount = this.fade.value();
        if (amount <= 0.01F) {
            return;
        }

        int alpha = Math.round(this.strength.getFloat() * 255.0F * amount);
        RenderUtil.rect(context, 0.0F, 0.0F,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight(),
                ColorUtil.withAlpha(this.scrimColor.get(), alpha));
    }

    private boolean shouldDim(Screen screen) {
        if (screen == null) {
            return false;
        }
        // The client's own screens draw their own background; dimming twice
        // would make the panel sit on a much darker ground than intended.
        if (screen instanceof de.nebrel.client.gui.screen.NebrelClientScreen
                || screen instanceof de.nebrel.client.gui.screen.HudEditorScreen) {
            return false;
        }
        if (screen instanceof ChatScreen) {
            return this.chat.get();
        }
        if (screen instanceof HandledScreen<?>) {
            return this.inventory.get();
        }
        return this.otherScreens.get();
    }
}

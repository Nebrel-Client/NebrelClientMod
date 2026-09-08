package de.nebrel.client.module.impl.player;

import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.event.HudRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Sprint and sneak without holding the key down.
 *
 * <p>Implemented exactly the way vanilla's own "Sprint: Toggle" option is: the
 * sprint key's pressed state is held down while the toggle is active. No
 * movement value is altered and nothing is sent that the player could not
 * produce by holding the key, so this changes ergonomics, not capability.</p>
 */
public final class ToggleSprintModule extends Module implements ClientTickHook, HudRenderHook {

    /** How the toggle is armed. */
    public enum Mode {
        TOGGLE("Toggle"),
        HOLD("Hold"),
        ALWAYS("Always Sprint");

        private final String display;

        Mode(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    public final EnumSetting<Mode> sprintMode;
    public final BooleanSetting toggleSneak;
    public final BooleanSetting stopOnHunger;
    public final BooleanSetting hudIndicator;
    public final ColorSetting activeColor;
    public final ColorSetting inactiveColor;
    public final NumberSetting indicatorOffset;

    private boolean sprintToggled;
    private boolean sneakToggled;
    private boolean sprintKeyWasDown;
    private boolean sneakKeyWasDown;

    public ToggleSprintModule() {
        super("toggle_sprint", "Toggle Sprint", "Sprint and sneak without holding the key",
                ModuleCategory.PLAYER, "»");

        this.sprintMode = choice("mode", "Key Mode", "How sprinting is armed", Mode.TOGGLE);

        this.toggleSneak = bool("toggleSneak", "Toggle Sneak",
                "Apply the same behaviour to sneaking", false, SettingSection.BEHAVIOR);

        this.stopOnHunger = bool("stopOnHunger", "Stop When Hungry",
                "Release the toggle when hunger is too low to sprint", true,
                SettingSection.BEHAVIOR);

        this.hudIndicator = bool("indicator", "HUD Indicator",
                "Show the sprint state above the hotbar", true, SettingSection.APPEARANCE);

        this.activeColor = color("activeColor", "Active Color",
                "Colour while the toggle is on", 0xFF3DD68C, true, SettingSection.APPEARANCE);
        this.activeColor.visibleWhen(this.hudIndicator);

        this.inactiveColor = color("inactiveColor", "Inactive Color",
                "Colour while the toggle is off", 0xB09A9AAB, true, SettingSection.APPEARANCE);
        this.inactiveColor.visibleWhen(this.hudIndicator);

        this.indicatorOffset = number("offset", "Indicator Offset",
                "Distance above the hotbar in pixels", 58.0D, 20.0D, 140.0D, 1.0D,
                SettingSection.APPEARANCE);
        this.indicatorOffset.visibleWhen(this.hudIndicator);
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        // Never leave a key stuck down when the module goes away.
        if (client.options != null) {
            client.options.sprintKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
        }
        this.sprintToggled = false;
        this.sneakToggled = false;
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) {
            return;
        }

        boolean sprintDown = client.options.sprintKey.isPressed();
        // Rising edge only: holding the key must not flip the toggle every tick.
        boolean sprintTapped = sprintDown && !this.sprintKeyWasDown;
        this.sprintKeyWasDown = sprintDown;

        Mode mode = this.sprintMode.get();
        if (mode == Mode.ALWAYS) {
            this.sprintToggled = true;
        } else if (mode == Mode.TOGGLE && sprintTapped) {
            this.sprintToggled = !this.sprintToggled;
        } else if (mode == Mode.HOLD) {
            this.sprintToggled = sprintDown;
        }

        if (this.stopOnHunger.get() && player.getHungerManager().getFoodLevel() <= 6) {
            // Vanilla refuses to sprint here anyway; releasing keeps the
            // indicator honest instead of showing a sprint that is not happening.
            this.sprintToggled = false;
        }

        if (this.sprintToggled) {
            client.options.sprintKey.setPressed(true);
        }

        if (this.toggleSneak.get()) {
            boolean sneakDown = client.options.sneakKey.isPressed();
            boolean sneakTapped = sneakDown && !this.sneakKeyWasDown;
            this.sneakKeyWasDown = sneakDown;
            if (sneakTapped) {
                this.sneakToggled = !this.sneakToggled;
            }
            if (this.sneakToggled) {
                client.options.sneakKey.setPressed(true);
            }
        }
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (!this.hudIndicator.get()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.currentScreen != null) {
            return;
        }

        boolean sprinting = client.player.isSprinting();
        String label = switch (this.sprintMode.get()) {
            case ALWAYS -> "Always Sprint";
            case HOLD -> sprinting ? "Sprinting" : "Sprint";
            case TOGGLE -> this.sprintToggled ? "Sprint [Toggled]" : "Sprint";
        };
        int color = (this.sprintToggled || sprinting)
                ? this.activeColor.get()
                : this.inactiveColor.get();

        float centerX = client.getWindow().getScaledWidth() / 2.0F;
        float y = client.getWindow().getScaledHeight() - this.indicatorOffset.getFloat();
        RenderUtil.textCentered(context, label, centerX, y, color);

        if (this.toggleSneak.get() && this.sneakToggled) {
            RenderUtil.textCentered(context, "Sneak [Toggled]", centerX,
                    y + RenderUtil.lineHeight() + 1.0F,
                    ColorUtil.withAlpha(this.activeColor.get(), 220));
        }
    }
}

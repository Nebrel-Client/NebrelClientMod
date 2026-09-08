package de.nebrel.client.module.impl.hud;

import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.event.HudRenderHook;
import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.keybind.KeybindManager;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A live display of the movement and mouse keys.
 *
 * <p>Reads the player's actual vanilla bindings rather than assuming WASD, so
 * it is correct on a non-QWERTY layout or a remapped setup. Each key animates
 * its own press so a fast tap still registers visibly.</p>
 *
 * <p>Purely an observation of input the player produced. Nothing here presses
 * anything.</p>
 */
public final class KeystrokesModule extends Module implements HudRenderHook {

    /** One drawn key. */
    private static final class Key {
        final String label;
        final int column;
        final int row;
        final int widthUnits;
        final java.util.function.BooleanSupplier pressed;
        final Animation animation;

        Key(String label, int column, int row, int widthUnits,
            java.util.function.BooleanSupplier pressed, long duration) {
            this.label = label;
            this.column = column;
            this.row = row;
            this.widthUnits = widthUnits;
            this.pressed = pressed;
            this.animation = new Animation(0.0F, duration, Easing.EASE_OUT);
        }
    }

    public final EnumSetting<HudAnchor> anchor;
    public final NumberSetting offsetX;
    public final NumberSetting offsetY;
    public final NumberSetting scale;
    public final NumberSetting keySize;
    public final NumberSetting keyGap;
    public final NumberSetting radius;
    public final NumberSetting opacity;
    public final BooleanSetting showMouse;
    public final BooleanSetting showSpace;
    public final BooleanSetting showShift;
    public final BooleanSetting showCps;
    public final ColorSetting activeColor;
    public final ColorSetting inactiveColor;
    public final ColorSetting textColor;
    public final ColorSetting activeTextColor;
    public final BooleanSetting useAccent;

    private final List<Key> keys = new ArrayList<>(8);
    private boolean built;

    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", "Show your movement and mouse input on screen",
                ModuleCategory.HUD, "⌨");

        this.anchor = choice("anchor", "Anchor", "Screen corner to measure from",
                HudAnchor.BOTTOM_LEFT);
        this.offsetX = number("x", "Offset X", "Horizontal offset from the anchor",
                8.0D, -400.0D, 400.0D, 1.0D);
        this.offsetY = number("y", "Offset Y", "Vertical offset from the anchor",
                -60.0D, -400.0D, 400.0D, 1.0D);

        this.scale = number("scale", "Scale", "Overall size", 1.0D, 0.5D, 2.5D, 0.05D,
                SettingSection.APPEARANCE);
        this.keySize = number("keySize", "Key Size", "Size of one key square in pixels",
                18.0D, 10.0D, 32.0D, 1.0D, SettingSection.APPEARANCE);
        this.keyGap = number("keyGap", "Key Gap", "Space between keys in pixels",
                2.0D, 0.0D, 8.0D, 1.0D, SettingSection.APPEARANCE);
        this.radius = number("radius", "Corner Radius", "Key corner rounding",
                3.0D, 0.0D, 10.0D, 1.0D, SettingSection.APPEARANCE);
        this.opacity = number("opacity", "Opacity", "Overall transparency",
                1.0D, 0.1D, 1.0D, 0.05D, SettingSection.APPEARANCE);

        this.showMouse = bool("showMouse", "Mouse Buttons", "Show the left and right buttons",
                true, SettingSection.BEHAVIOR);
        this.showSpace = bool("showSpace", "Space Bar", "Show the jump key", true,
                SettingSection.BEHAVIOR);
        this.showShift = bool("showShift", "Sneak Key", "Show the sneak key", false,
                SettingSection.BEHAVIOR);
        this.showCps = bool("showCps", "Clicks Per Second",
                "Show the click rate inside the mouse keys", true, SettingSection.BEHAVIOR);
        this.showCps.visibleWhen(this.showMouse);

        this.inactiveColor = color("inactiveColor", "Inactive Color", "Key colour when up",
                0x8C16161D, true, SettingSection.APPEARANCE);
        this.activeColor = color("activeColor", "Active Color", "Key colour when pressed",
                0xE6FFFFFF, true, SettingSection.APPEARANCE);
        this.activeColor.visibleWhen(() -> !this.useAccentValue());
        this.textColor = color("textColor", "Text Color", "Label colour when up",
                0xFFECECF2, true, SettingSection.APPEARANCE);
        this.activeTextColor = color("activeTextColor", "Active Text Color",
                "Label colour when pressed", 0xFF16161D, true, SettingSection.APPEARANCE);
        this.useAccent = bool("useAccent", "Use Accent Color",
                "Use the client accent for pressed keys", false, SettingSection.APPEARANCE);
    }

    private boolean useAccentValue() {
        return this.useAccent != null && this.useAccent.get();
    }

    /** Builds the key list lazily: the vanilla bindings are not ready at construction. */
    private void buildKeys(MinecraftClient client) {
        this.keys.clear();
        var options = client.options;
        KeybindManager keybinds = NebrelClient.get().keybinds();
        long duration = 90L;

        this.keys.add(new Key(labelFor(options.forwardKey), 1, 0, 1,
                options.forwardKey::isPressed, duration));
        this.keys.add(new Key(labelFor(options.leftKey), 0, 1, 1,
                options.leftKey::isPressed, duration));
        this.keys.add(new Key(labelFor(options.backKey), 1, 1, 1,
                options.backKey::isPressed, duration));
        this.keys.add(new Key(labelFor(options.rightKey), 2, 1, 1,
                options.rightKey::isPressed, duration));

        int row = 2;
        if (this.showMouse.get()) {
            this.keys.add(new Key("LMB", 0, row, 1, () -> keybinds.isMouseDown(0), duration));
            this.keys.add(new Key("RMB", 2, row, 1, () -> keybinds.isMouseDown(1), duration));
            // The middle cell of the mouse row is left empty on purpose: it
            // keeps the block symmetrical under the WASD cluster.
            row++;
        }
        if (this.showSpace.get()) {
            this.keys.add(new Key("", 0, row, 3, options.jumpKey::isPressed, duration));
            row++;
        }
        if (this.showShift.get()) {
            this.keys.add(new Key(labelFor(options.sneakKey), 0, row, 3,
                    options.sneakKey::isPressed, duration));
        }
        this.built = true;
    }

    private static String labelFor(KeyBinding binding) {
        int code = KeybindManager.vanillaKeyCode(binding);
        String name = de.nebrel.client.setting.KeybindSetting.keyName(code);
        // Single characters read best; longer names are shortened to fit a key.
        return name.length() <= 2 ? name : name.substring(0, Math.min(4, name.length()));
    }

    @Override
    protected void onEnable() {
        this.built = false;
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.currentScreen != null) {
            return;
        }
        if (!this.built) {
            buildKeys(client);
        }

        float unit = this.keySize.getFloat();
        float gap = this.keyGap.getFloat();
        float scaleValue = this.scale.getFloat();

        int columns = 3;
        int rows = 2 + (this.showMouse.get() ? 1 : 0)
                + (this.showSpace.get() ? 1 : 0)
                + (this.showShift.get() ? 1 : 0);

        float blockWidth = columns * unit + (columns - 1) * gap;
        float blockHeight = rows * unit + (rows - 1) * gap;
        float totalWidth = blockWidth * scaleValue;
        float totalHeight = blockHeight * scaleValue;

        float screenWidth = client.getWindow().getScaledWidth();
        float screenHeight = client.getWindow().getScaledHeight();
        HudAnchor currentAnchor = this.anchor.get();
        float left = currentAnchor.resolveX(this.offsetX.getFloat(), screenWidth, totalWidth);
        float top = currentAnchor.resolveY(this.offsetY.getFloat(), screenHeight, totalHeight);

        float alpha = this.opacity.getFloat();
        int accent = NebrelClient.get().accent();

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(left, top, 0.0F);
        matrices.scale(scaleValue, scaleValue, 1.0F);

        for (Key key : this.keys) {
            boolean down = key.pressed.getAsBoolean();
            key.animation.animateTo(down);
            float pressAmount = key.animation.value();

            float x = key.column * (unit + gap);
            float y = key.row * (unit + gap);
            float width = key.widthUnits * unit + (key.widthUnits - 1) * gap;

            int pressedColor = this.useAccent.get() ? accent : this.activeColor.get();
            int background = ColorUtil.lerp(this.inactiveColor.get(), pressedColor, pressAmount);
            int label = ColorUtil.lerp(this.textColor.get(), this.activeTextColor.get(), pressAmount);

            RenderUtil.roundedRect(context, x, y, width, unit, this.radius.getFloat(),
                    ColorUtil.fadeAlpha(background, alpha));

            String text = key.label;
            if (this.showCps.get() && ("LMB".equals(key.label) || "RMB".equals(key.label))) {
                int cps = "LMB".equals(key.label)
                        ? NebrelClient.get().stats().leftCps()
                        : NebrelClient.get().stats().rightCps();
                text = cps + " CPS";
            }

            if (!text.isEmpty()) {
                float textScale = RenderUtil.textWidth(text) > width - 4.0F ? 0.7F : 1.0F;
                RenderUtil.textScaledCentered(context, text, x + width / 2.0F,
                        y + (unit - RenderUtil.lineHeight() * textScale) / 2.0F + 1.0F,
                        textScale, ColorUtil.fadeAlpha(label, alpha));
            }
        }

        matrices.pop();
    }
}

package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleManager;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * A module tile: icon, name, description, favourite star and an on/off switch.
 *
 * <p>An enabled card is marked by the switch and a restrained accent border
 * rather than by filling the card with colour, so a grid with many modules on
 * still reads as a list rather than a wall of violet.</p>
 */
public final class ModuleCardComponent extends Component {

    public static final float HEIGHT = 52.0F;
    private static final float PADDING = 10.0F;
    private static final float ICON_SIZE = 22.0F;

    private final Module module;
    private final ModuleManager modules;
    private final Consumer<Module> onOpenSettings;

    private final ToggleComponent toggle;
    private final Animation hover;
    private final Animation enabledAmount;

    private boolean lastEnabled;

    public ModuleCardComponent(UiContext ui, Module module, ModuleManager modules,
                               Consumer<Module> onOpenSettings) {
        super(ui);
        this.module = module;
        this.modules = modules;
        this.onOpenSettings = onOpenSettings;
        this.toggle = new ToggleComponent(ui, module::enabled,
                value -> modules.setEnabled(module, value));
        this.hover = new Animation(0.0F, ui.duration(150L), Easing.EASE_OUT);
        this.lastEnabled = module.enabled();
        this.enabledAmount = new Animation(this.lastEnabled ? 1.0F : 0.0F,
                ui.duration(180L), Easing.EASE_OUT_CUBIC);
        this.height = HEIGHT;
    }

    public Module module() {
        return this.module;
    }

    @Override
    public float preferredHeight() {
        return HEIGHT;
    }

    private float starCenterX() {
        return this.x + this.width - PADDING - 6.0F;
    }

    private float starCenterY() {
        return this.y + PADDING + 4.0F;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();

        boolean enabled = this.module.enabled();
        if (enabled != this.lastEnabled) {
            this.lastEnabled = enabled;
            this.enabledAmount.animateTo(enabled);
        }
        boolean hovered = isHovered(mouseX, mouseY);
        this.hover.animateTo(hovered);

        float hoverAmount = this.hover.value();
        float onAmount = this.enabledAmount.value();

        // Lift on hover: one surface step, plus a small shadow.
        if (hoverAmount > 0.01F) {
            RenderUtil.shadow(context, this.x, this.y, this.width, this.height, 8.0F,
                    ColorUtil.fadeAlpha(theme.shadow, hoverAmount * 0.7F), 3);
        }
        int background = ColorUtil.lerp(theme.surface, theme.surfaceHover, hoverAmount);
        RenderUtil.roundedRect(context, this.x, this.y, this.width, this.height, 8.0F, background);

        int borderColor = ColorUtil.lerp(theme.border,
                ColorUtil.withAlpha(theme.accent, 130), onAmount);
        RenderUtil.roundedOutline(context, this.x, this.y, this.width, this.height, 8.0F, borderColor);

        // Icon tile, tinted by the accent once the module is on.
        float iconX = this.x + PADDING;
        float iconY = this.y + (this.height - ICON_SIZE) / 2.0F;
        int iconBackground = ColorUtil.lerp(theme.surfaceElevated, theme.accentSoft, onAmount);
        RenderUtil.roundedRect(context, iconX, iconY, ICON_SIZE, ICON_SIZE, 6.0F, iconBackground);
        int iconColor = ColorUtil.lerp(theme.textSecondary, theme.accent, onAmount);
        RenderUtil.textCentered(context, this.module.icon(), iconX + ICON_SIZE / 2.0F,
                iconY + (ICON_SIZE - RenderUtil.lineHeight()) / 2.0F + 1.0F, iconColor);

        float textX = iconX + ICON_SIZE + 9.0F;
        float textRight = this.x + this.width - PADDING - ToggleComponent.TRACK_WIDTH - 10.0F;
        int textBudget = (int) Math.max(20.0F, textRight - textX);

        boolean showDescription = this.ui.settings().showDescriptions.get()
                && !this.module.description().isEmpty();
        float nameY = showDescription
                ? this.y + PADDING + 2.0F
                : this.y + (this.height - RenderUtil.lineHeight()) / 2.0F;

        RenderUtil.textFlat(context, RenderUtil.truncate(this.module.name(), textBudget),
                textX, nameY, theme.textPrimary);

        if (showDescription) {
            RenderUtil.textScaled(context,
                    RenderUtil.truncate(this.module.description(), (int) (textBudget / 0.85F)),
                    textX, nameY + RenderUtil.lineHeight() + 2.0F, 0.85F, theme.textSecondary, false);
        }

        // Keybind chip, only when one is actually bound.
        KeybindSetting keybind = this.module.keybind();
        if (keybind.bound()) {
            String label = keybind.displayValue();
            float chipWidth = RenderUtil.textWidthScaled(label, 0.8F) + 8.0F;
            float chipX = textRight - chipWidth;
            float chipY = this.y + this.height - PADDING - 9.0F;
            RenderUtil.roundedRect(context, chipX, chipY, chipWidth, 10.0F, 3.0F,
                    ColorUtil.withAlpha(theme.surfaceElevated, 220));
            RenderUtil.textScaled(context, label, chipX + 4.0F, chipY + 1.5F, 0.8F,
                    theme.textSecondary, false);
        }

        // Favourite star, filled when set.
        drawStar(context, starCenterX(), starCenterY(),
                this.module.favorite(),
                RenderUtil.hovered(mouseX, mouseY, starCenterX() - 6.0F, starCenterY() - 6.0F, 12.0F, 12.0F),
                theme);

        // Settings affordance, only for modules that have something to configure.
        if (this.module.hasConfigurableSettings()) {
            float gearY = this.y + this.height - PADDING - 4.0F;
            boolean gearHovered = RenderUtil.hovered(mouseX, mouseY,
                    starCenterX() - 7.0F, gearY - 6.0F, 14.0F, 12.0F);
            RenderUtil.textCentered(context, "⋯", starCenterX(), gearY - 4.0F,
                    gearHovered ? theme.textPrimary : theme.textDisabled);
        }

        this.toggle.setBounds(this.x + this.width - PADDING - ToggleComponent.TRACK_WIDTH,
                this.y + (this.height - ToggleComponent.TRACK_HEIGHT) / 2.0F + 8.0F,
                ToggleComponent.TRACK_WIDTH, ToggleComponent.TRACK_HEIGHT);
        this.toggle.render(context, mouseX, mouseY, delta);
    }

    /** A five-pointed star drawn from rows, filled or outlined. */
    private static void drawStar(DrawContext context, float centerX, float centerY,
                                 boolean filled, boolean hovered, Theme theme) {
        int color = filled ? theme.warning : (hovered ? theme.textSecondary : theme.textDisabled);
        String glyph = filled ? "★" : "☆";
        RenderUtil.textCentered(context, glyph, centerX,
                centerY - RenderUtil.lineHeight() / 2.0F + 1.0F, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible() || !isHovered(mouseX, mouseY)) {
            return false;
        }
        if (button == 0) {
            if (this.toggle.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (RenderUtil.hovered(mouseX, mouseY,
                    starCenterX() - 7.0F, starCenterY() - 7.0F, 14.0F, 14.0F)) {
                this.modules.setFavorite(this.module, !this.module.favorite());
                return true;
            }
            if (this.module.hasConfigurableSettings()) {
                // Anywhere else on the card opens the settings view.
                this.onOpenSettings.accept(this.module);
                return true;
            }
            return true;
        }
        if (button == 1) {
            // Right click is a shortcut straight to the settings view.
            if (this.module.hasConfigurableSettings()) {
                this.onOpenSettings.accept(this.module);
            }
            return true;
        }
        return false;
    }

    @Override
    public String tooltip() {
        if (!this.ui.settings().showDescriptions.get() && !this.module.description().isEmpty()) {
            return this.module.description();
        }
        return null;
    }
}

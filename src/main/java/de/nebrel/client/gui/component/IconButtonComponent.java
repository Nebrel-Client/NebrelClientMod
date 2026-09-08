package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * A compact button showing a single glyph, optionally with a label.
 *
 * <p>Glyphs are text rather than textures, so a button costs nothing to add and
 * follows the theme automatically.</p>
 */
public final class IconButtonComponent extends Component {

    /** Visual weight of the button. */
    public enum Style {
        /** Transparent until hovered. Used for toolbar actions. */
        GHOST,
        /** Filled with the elevated surface. Used for secondary actions. */
        SOFT,
        /** Filled with the accent. Used for the single primary action. */
        ACCENT
    }

    private final String glyph;
    private final Runnable action;
    private final Animation hover;

    private Style style = Style.GHOST;
    private String label = "";
    private String tooltip;
    private boolean active;

    public IconButtonComponent(UiContext ui, String glyph, Runnable action) {
        super(ui);
        this.glyph = glyph == null ? "" : glyph;
        this.action = action == null ? () -> {
        } : action;
        this.hover = new Animation(0.0F, ui.duration(130L), Easing.EASE_OUT);
        this.width = 20.0F;
        this.height = 20.0F;
    }

    public IconButtonComponent style(Style value) {
        this.style = value;
        return this;
    }

    public IconButtonComponent label(String value) {
        this.label = value == null ? "" : value;
        return this;
    }

    public IconButtonComponent tooltip(String value) {
        this.tooltip = value;
        return this;
    }

    /** Marks the button as representing an on state, e.g. a filter that is applied. */
    public void setActive(boolean value) {
        this.active = value;
    }

    public boolean active() {
        return this.active;
    }

    /** Natural width for the glyph plus optional label. */
    public float measuredWidth() {
        float width = 20.0F;
        if (!this.label.isEmpty()) {
            width = 12.0F + RenderUtil.textWidth(this.glyph + " " + this.label);
        }
        return width;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        boolean hovered = isHovered(mouseX, mouseY) && enabled();
        this.hover.animateTo(hovered);
        float amount = this.hover.value();

        int background = switch (this.style) {
            case GHOST -> this.active
                    ? theme.accentSoft
                    : ColorUtil.fadeAlpha(theme.surfaceHover, amount);
            case SOFT -> ColorUtil.lerp(theme.surfaceElevated, theme.surfaceHover, amount);
            case ACCENT -> ColorUtil.lerp(theme.accent, theme.accentHover, amount);
        };
        RenderUtil.roundedRect(context, this.x, this.y, this.width, this.height, 5.0F, background);

        if (this.style == Style.SOFT) {
            RenderUtil.roundedOutline(context, this.x, this.y, this.width, this.height, 5.0F, theme.border);
        }

        int foreground = switch (this.style) {
            case ACCENT -> 0xFFFFFFFF;
            case GHOST, SOFT -> this.active
                    ? theme.accent
                    : ColorUtil.lerp(theme.textSecondary, theme.textPrimary, amount);
        };
        if (!enabled()) {
            foreground = theme.textDisabled;
        }

        String content = this.label.isEmpty() ? this.glyph : this.glyph + " " + this.label;
        RenderUtil.textCentered(context, content, this.x + this.width / 2.0F,
                this.y + (this.height - RenderUtil.lineHeight()) / 2.0F + 1.0F, foreground);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled() || !isHovered(mouseX, mouseY)) {
            return false;
        }
        this.action.run();
        return true;
    }

    @Override
    public String tooltip() {
        return this.tooltip;
    }
}

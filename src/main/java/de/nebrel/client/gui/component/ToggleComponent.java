package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A pill switch.
 *
 * <p>Reads its state through a supplier rather than caching it, so a toggle
 * stays correct when the same value is changed elsewhere (a keybind firing
 * while the menu is open).</p>
 */
public final class ToggleComponent extends Component {

    public static final float TRACK_WIDTH = 26.0F;
    public static final float TRACK_HEIGHT = 14.0F;

    private final BooleanSupplier state;
    private final Consumer<Boolean> onChange;

    private final Animation knob;
    private final Animation hover;
    private boolean lastState;

    public ToggleComponent(UiContext ui, BooleanSupplier state, Consumer<Boolean> onChange) {
        super(ui);
        this.state = state;
        this.onChange = onChange;
        this.lastState = state.getAsBoolean();
        this.knob = new Animation(this.lastState ? 1.0F : 0.0F, ui.duration(160L), Easing.EASE_OUT_CUBIC);
        this.hover = new Animation(0.0F, ui.duration(120L), Easing.EASE_OUT);
        this.width = TRACK_WIDTH;
        this.height = TRACK_HEIGHT;
    }

    @Override
    public float preferredHeight() {
        return TRACK_HEIGHT;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        boolean current = this.state.getAsBoolean();
        if (current != this.lastState) {
            // Something outside the menu changed the value; catch up smoothly.
            this.lastState = current;
            this.knob.animateTo(current);
        }
        boolean hovered = isHovered(mouseX, mouseY) && enabled();
        this.hover.animateTo(hovered);

        float progress = this.knob.value();
        float hoverAmount = this.hover.value();

        int trackOff = ColorUtil.lerp(theme.surfaceHover, theme.border, 0.35F);
        int trackOn = enabled() ? theme.accent : theme.textDisabled;
        int track = ColorUtil.lerp(trackOff, trackOn, progress);
        if (hoverAmount > 0.0F) {
            track = ColorUtil.lerp(track,
                    progress > 0.5F ? theme.accentHover : theme.surfaceHover, hoverAmount * 0.6F);
        }
        if (!enabled()) {
            track = ColorUtil.fadeAlpha(track, 0.45F);
        }

        float radius = this.height / 2.0F;
        RenderUtil.roundedRect(context, this.x, this.y, this.width, this.height, radius, track);

        // A hairline outline keeps the off state visible on a light theme.
        if (progress < 0.5F) {
            RenderUtil.roundedOutline(context, this.x, this.y, this.width, this.height, radius,
                    ColorUtil.fadeAlpha(theme.border, 1.0F - progress));
        }

        float knobSize = this.height - 4.0F;
        float travel = this.width - knobSize - 4.0F;
        float knobX = this.x + 2.0F + travel * progress;
        int knobColor = progress > 0.5F ? 0xFFFFFFFF : theme.textSecondary;
        if (!enabled()) {
            knobColor = ColorUtil.fadeAlpha(knobColor, 0.5F);
        }
        RenderUtil.roundedRect(context, knobX, this.y + 2.0F, knobSize, knobSize,
                knobSize / 2.0F, knobColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled() || !isHovered(mouseX, mouseY)) {
            return false;
        }
        boolean next = !this.state.getAsBoolean();
        this.onChange.accept(next);
        this.lastState = next;
        this.knob.animateTo(next);
        return true;
    }

    /** Forces the knob to a position without animating; used when reusing a row. */
    public void syncImmediately() {
        this.lastState = this.state.getAsBoolean();
        this.knob.set(this.lastState ? 1.0F : 0.0F);
    }

    /** Convenience for callers that only want the current animated position. */
    public float progress() {
        return NebrelMath.clamp(this.knob.value(), 0.0F, 1.0F);
    }
}
